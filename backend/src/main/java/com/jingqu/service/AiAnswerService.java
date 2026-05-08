package com.jingqu.service;

import com.jingqu.config.AiProperties;
import com.jingqu.dto.AiAnswerResult;
import com.jingqu.dto.RagDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnswerService {

    private final AiProperties aiProperties;
    private final QuestionRouter questionRouter;
    private final RagflowClient ragflowClient;
    private final DeepSeekClient deepSeekClient;
    private final KnowledgeService knowledgeService;

    public AiAnswerResult answer(String question, String scenicSpot) {
        QuestionRouter.RouteTarget routeTarget = questionRouter.route(question);
        long start = System.currentTimeMillis();

        List<RagDocument> documents = new ArrayList<>();
        if (aiProperties.isEnableRag() && ragflowClient.isConfigured() && routeTarget != QuestionRouter.RouteTarget.FALLBACK_ONLY) {
            for (String datasetKey : questionRouter.datasetKeys(routeTarget)) {
                documents.addAll(ragflowClient.search(datasetKey, question, documents.isEmpty() ? 3 : 2));
            }
        }

        String answer = null;
        boolean fallbackUsed = false;
        String source = "rag";

        if (!documents.isEmpty() && deepSeekClient.isConfigured()) {
            answer = deepSeekClient.answer(question, documents, scenicSpot);
        }

        if (!hasUsableAnswer(answer) && aiProperties.isEnableFallback()) {
            fallbackUsed = true;
            source = "fallback";
            answer = knowledgeService.findBestAnswer(question);
        }

        if (!hasUsableAnswer(answer)) {
            fallbackUsed = true;
            source = "fallback";
            answer = "当前知识库未提供该信息，建议查看景区公告或咨询现场服务人员。";
        }

        return AiAnswerResult.builder()
            .answer(answer)
            .routeTarget(routeTarget.name())
            .retrievedDocsCount(documents.size())
            .fallbackUsed(fallbackUsed)
            .modelLatencyMs(System.currentTimeMillis() - start)
            .finalAnswerSource(source)
            .knowledgeSources(extractSources(documents))
            .build();
    }

    private boolean hasUsableAnswer(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> extractSources(List<RagDocument> documents) {
        List<String> sources = new ArrayList<>();
        for (RagDocument document : documents) {
            String source = document.getDatasetName() + ":" + document.getDocumentName();
            if (!sources.contains(source)) {
                sources.add(source);
            }
        }
        return sources;
    }
}
