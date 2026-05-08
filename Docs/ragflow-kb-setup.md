# RAGFlow Knowledge Base Setup

## 1. Install prerequisites

```bash
pip install python-docx ragflow-sdk
```

## 2. Export environment variables

PowerShell:

```powershell
$env:RAGFLOW_BASE_URL="http://localhost:9380"
$env:RAGFLOW_API_KEY="your-ragflow-api-key"
$env:RAGFLOW_DATASET_SPOT_STRUCTURED="scenic-spot-structured"
$env:RAGFLOW_DATASET_HISTORY_ROUTE="history-culture-route"
```

## 3. Prepare local chunk files

```bash
python Docs/scripts/import_ragflow_kb.py --replace
```

Prepared files will be written to:

- `data/ragflow_import/scenic-spot-structured`
- `data/ragflow_import/history-culture-route`

## 4. Upload to RAGFlow

```bash
python Docs/scripts/import_ragflow_kb.py --upload
```

## 5. Create RAGFlow chat assistants

Create two chat assistants in the RAGFlow UI:

1. One assistant bound to `scenic-spot-structured`
2. One assistant bound to `history-culture-route`

Then expose their IDs to the backend:

```powershell
$env:RAGFLOW_CHAT_SPOT_STRUCTURED_ID="your-spot-chat-id"
$env:RAGFLOW_CHAT_HISTORY_ROUTE_ID="your-history-chat-id"
$env:DEEPSEEK_API_KEY="your-deepseek-api-key"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_MODEL="deepseek-chat"
```

## 6. Runtime behavior

- The backend first routes the user question.
- RAGFlow returns references from the matching assistant.
- DeepSeek generates the final answer from retrieved evidence.
- If retrieval or generation fails, the backend falls back to `knowledge_base`.
