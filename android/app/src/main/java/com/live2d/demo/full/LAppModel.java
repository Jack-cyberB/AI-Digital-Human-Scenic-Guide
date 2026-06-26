/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d.demo.full;

import com.live2d.demo.LAppDefine;
import com.live2d.sdk.cubism.framework.CubismDefaultParameterId.ParameterId;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismModelSettingJson;
import com.live2d.sdk.cubism.framework.ICubismModelSetting;
import com.live2d.sdk.cubism.framework.effect.CubismBreath;
import com.live2d.sdk.cubism.framework.effect.CubismEyeBlink;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.id.CubismIdManager;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.model.CubismMoc;
import com.live2d.sdk.cubism.framework.model.CubismUserModel;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionMotion;
import com.live2d.sdk.cubism.framework.motion.CubismMotion;
import com.live2d.sdk.cubism.framework.motion.IBeganMotionCallback;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;
import com.live2d.sdk.cubism.framework.rendering.CubismRenderer;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenSurfaceAndroid;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LAppModel extends CubismUserModel {
    public LAppModel() {
        if (LAppDefine.MOC_CONSISTENCY_VALIDATION_ENABLE) {
            mocConsistency = true;
        }

        if (LAppDefine.MOTION_CONSISTENCY_VALIDATION_ENABLE) {
            motionConsistency = true;
        }

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            debugMode = true;
        }

        CubismIdManager idManager = CubismFramework.getIdManager();

        idParamAngleX = idManager.getId(ParameterId.ANGLE_X.getId());
        idParamAngleY = idManager.getId(ParameterId.ANGLE_Y.getId());
        idParamAngleZ = idManager.getId(ParameterId.ANGLE_Z.getId());
        idParamBodyAngleX = idManager.getId(ParameterId.BODY_ANGLE_X.getId());
        idParamEyeBallX = idManager.getId(ParameterId.EYE_BALL_X.getId());
        idParamEyeBallY = idManager.getId(ParameterId.EYE_BALL_Y.getId());
    }

    public void loadAssets(final String dir, final String fileName) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("load model setting: " + fileName);
        }

        modelHomeDirectory = dir;
        String filePath = modelHomeDirectory + fileName;
        byte[] buffer = createBuffer(filePath);
        ICubismModelSetting setting = new CubismModelSettingJson(buffer);
        setupModel(setting);

        if (model == null) {
            LAppPal.printLog("Failed to loadAssets().");
            return;
        }

        CubismRenderer renderer = CubismRendererAndroid.create();
        setupRenderer(renderer);
        setupTextures();
    }

    public void deleteModel() { delete(); }

    public void update() {
        final float deltaTimeSeconds = LAppPal.getDeltaTime();
        userTimeSeconds += deltaTimeSeconds;
        dragManager.update(deltaTimeSeconds);
        dragX = dragManager.getX();
        dragY = dragManager.getY();

        boolean isMotionUpdated = false;
        model.loadParameters();
        if (motionManager.isFinished()) {
            startRandomMotion(LAppDefine.MotionGroup.IDLE.getId(), LAppDefine.Priority.IDLE.getPriority());
        } else {
            isMotionUpdated = motionManager.updateMotion(model, deltaTimeSeconds);
        }
        model.saveParameters();
        opacity = model.getModelOpacity();
        if (!isMotionUpdated && eyeBlink != null) {
            eyeBlink.updateParameters(model, deltaTimeSeconds);
        }
        if (expressionManager != null) {
            expressionManager.updateMotion(model, deltaTimeSeconds);
        }
        model.addParameterValue(idParamAngleX, dragX * 30);
        model.addParameterValue(idParamAngleY, dragY * 30);
        model.addParameterValue(idParamAngleZ, dragX * dragY * (-30));
        model.addParameterValue(idParamBodyAngleX, dragX * 10);
        model.addParameterValue(idParamEyeBallX, dragX);
        model.addParameterValue(idParamEyeBallY, dragY);
        if (breath != null) {
            breath.updateParameters(model, deltaTimeSeconds);
        }
        if (physics != null) {
            physics.evaluate(model, deltaTimeSeconds);
        }
        if (lipSync) {
            float value = lipSyncValue;
            for (int i = 0; i < lipSyncIds.size(); i++) {
                CubismId lipSyncId = lipSyncIds.get(i);
                model.addParameterValue(lipSyncId, value, 0.8f);
            }
        }
        if (pose != null) {
            pose.updateParameters(model, deltaTimeSeconds);
        }
        model.update();
    }

    public int startMotion(final String group, int number, int priority) {
        return startMotion(group, number, priority, null, null);
    }

    public int startMotion(final String group, int number, int priority,
                           IFinishedMotionCallback onFinishedMotionHandler,
                           IBeganMotionCallback onBeganMotionHandler) {
        if (priority == LAppDefine.Priority.FORCE.getPriority()) {
            motionManager.setReservationPriority(priority);
        } else if (!motionManager.reserveMotion(priority)) {
            if (debugMode) {
                LAppPal.printLog("Cannot start motion.");
            }
            return -1;
        }

        String name = group + "_" + number;
        CubismMotion motion = (CubismMotion) motions.get(name);
        if (motion == null) {
            String fileName = modelSetting.getMotionFileName(group, number);
            if (!fileName.equals("")) {
                String path = modelHomeDirectory + fileName;
                byte[] buffer = createBuffer(path);
                motion = loadMotion(buffer, onFinishedMotionHandler, onBeganMotionHandler, motionConsistency);
                if (motion != null) {
                    final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, number);
                    if (fadeInTime != -1.0f) motion.setFadeInTime(fadeInTime);
                    final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, number);
                    if (fadeOutTime != -1.0f) motion.setFadeOutTime(fadeOutTime);
                    motion.setEffectIds(eyeBlinkIds, lipSyncIds);
                } else {
                    CubismDebug.cubismLogError("Can't start motion %s", path);
                    motionManager.setReservationPriority(LAppDefine.Priority.NONE.getPriority());
                    return -1;
                }
            }
        } else {
            motion.setBeganMotionHandler(onBeganMotionHandler);
            motion.setFinishedMotionHandler(onFinishedMotionHandler);
        }

        String voice = modelSetting.getMotionSoundFileName(group, number);
        if (!voice.equals("")) {
            String path = modelHomeDirectory + voice;
            LAppWavFileHandler voicePlayer = new LAppWavFileHandler(path);
            voicePlayer.start();
        }

        if (debugMode) {
            LAppPal.printLog("start motion: " + group + "_" + number);
        }
        return motionManager.startMotionPriority(motion, priority);
    }

    public int startRandomMotion(final String group, int priority) {
        return startRandomMotion(group, priority, null, null);
    }

    public int startRandomMotion(final String group, int priority,
                                 IFinishedMotionCallback onFinishedMotionHandler,
                                 IBeganMotionCallback onBeganMotionHandler) {
        if (modelSetting.getMotionCount(group) == 0) return -1;
        Random random = new Random();
        int number = random.nextInt(Integer.MAX_VALUE) % modelSetting.getMotionCount(group);
        return startMotion(group, number, priority, onFinishedMotionHandler, onBeganMotionHandler);
    }

    public void draw(CubismMatrix44 matrix) {
        if (model == null) return;
        CubismMatrix44.multiply(modelMatrix.getArray(), matrix.getArray(), matrix.getArray());
        this.<CubismRendererAndroid>getRenderer().setMvpMatrix(matrix);
        this.<CubismRendererAndroid>getRenderer().drawModel();
    }

    public boolean hitTest(final String hitAreaName, float x, float y) {
        if (opacity < 1) return false;
        final int count = modelSetting.getHitAreasCount();
        for (int i = 0; i < count; i++) {
            if (modelSetting.getHitAreaName(i).equals(hitAreaName)) {
                final CubismId drawID = modelSetting.getHitAreaId(i);
                return isHit(drawID, x, y);
            }
        }
        return false;
    }

    public void setExpression(final String expressionID) {
        ACubismMotion motion = expressions.get(expressionID);
        if (debugMode) {
            LAppPal.printLog("expression: " + expressionID);
        }
        if (motion != null) {
            expressionManager.startMotionPriority(motion, LAppDefine.Priority.FORCE.getPriority());
        } else if (debugMode) {
            LAppPal.printLog("expression " + expressionID + "is null");
        }
    }

    public void setRandomExpression() {
        if (expressions.size() == 0) return;
        Random random = new Random();
        int number = random.nextInt(Integer.MAX_VALUE) % expressions.size();
        int i = 0;
        for (String key : expressions.keySet()) {
            if (i == number) {
                setExpression(key);
                return;
            }
            i++;
        }
    }

    public CubismOffscreenSurfaceAndroid getRenderingBuffer() { return renderingBuffer; }

    public boolean hasMocConsistencyFromFile(String mocFileName) {
        assert mocFileName != null && !mocFileName.isEmpty();
        String path = modelHomeDirectory + mocFileName;
        byte[] buffer = createBuffer(path);
        boolean consistency = CubismMoc.hasMocConsistency(buffer);
        if (!consistency) {
            CubismDebug.cubismLogInfo("Inconsistent MOC3.");
        } else {
            CubismDebug.cubismLogInfo("Consistent MOC3.");
        }
        return consistency;
    }

    private static byte[] createBuffer(final String path) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("create buffer: " + path);
        }
        return LAppPal.loadFileAsBytes(path);
    }

    private void setupModel(ICubismModelSetting setting) {
        modelSetting = setting;
        isUpdated = true;
        isInitialized = false;
        String fileName = modelSetting.getModelFileName();
        if (!fileName.equals("")) {
            String path = modelHomeDirectory + fileName;
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("create model: " + modelSetting.getModelFileName());
            }
            byte[] buffer = createBuffer(path);
            loadModel(buffer, mocConsistency);
        }
        if (modelSetting.getExpressionCount() > 0) {
            final int count = modelSetting.getExpressionCount();
            for (int i = 0; i < count; i++) {
                String name = modelSetting.getExpressionName(i);
                String path = modelSetting.getExpressionFileName(i);
                path = modelHomeDirectory + path;
                byte[] buffer = createBuffer(path);
                CubismExpressionMotion motion = loadExpression(buffer);
                if (motion != null) expressions.put(name, motion);
            }
        }
        String path = modelSetting.getPhysicsFileName();
        if (!path.equals("")) {
            String modelPath = modelHomeDirectory + path;
            byte[] buffer = createBuffer(modelPath);
            loadPhysics(buffer);
        }
        path = modelSetting.getPoseFileName();
        if (!path.equals("")) {
            String modelPath = modelHomeDirectory + path;
            byte[] buffer = createBuffer(modelPath);
            loadPose(buffer);
        }
        if (modelSetting.getEyeBlinkParameterCount() > 0) {
            eyeBlink = CubismEyeBlink.create(modelSetting);
        }
        breath = CubismBreath.create();
        List<CubismBreath.BreathParameterData> breathParameters = new ArrayList<>();
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleX, 0.0f, 15.0f, 6.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleY, 0.0f, 8.0f, 3.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleZ, 0.0f, 10.0f, 5.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamBodyAngleX, 0.0f, 4.0f, 15.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(CubismFramework.getIdManager().getId(ParameterId.BREATH.getId()), 0.5f, 0.5f, 3.2345f, 0.5f));
        breath.setParameters(breathParameters);
        path = modelSetting.getUserDataFile();
        if (!path.equals("")) {
            String modelPath = modelHomeDirectory + path;
            byte[] buffer = createBuffer(modelPath);
            loadUserData(buffer);
        }
        int eyeBlinkIdCount = modelSetting.getEyeBlinkParameterCount();
        for (int i = 0; i < eyeBlinkIdCount; i++) {
            eyeBlinkIds.add(modelSetting.getEyeBlinkParameterId(i));
        }
        int lipSyncIdCount = modelSetting.getLipSyncParameterCount();
        for (int i = 0; i < lipSyncIdCount; i++) {
            lipSyncIds.add(modelSetting.getLipSyncParameterId(i));
        }
        if (modelSetting == null || modelMatrix == null) {
            LAppPal.printLog("Failed to setupModel().");
            return;
        }
        Map<String, Float> layout = new HashMap<>();
        if (modelSetting.getLayoutMap(layout)) {
            modelMatrix.setupFromLayout(layout);
        }
        model.saveParameters();
        for (int i = 0; i < modelSetting.getMotionGroupCount(); i++) {
            String group = modelSetting.getMotionGroupName(i);
            preLoadMotionGroup(group);
        }
        motionManager.stopAllMotions();
        isUpdated = false;
        isInitialized = true;
    }

    private void preLoadMotionGroup(final String group) {
        final int count = modelSetting.getMotionCount(group);
        for (int i = 0; i < count; i++) {
            String name = group + "_" + i;
            String path = modelSetting.getMotionFileName(group, i);
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                if (debugMode) {
                    LAppPal.printLog("load motion: " + path + "==>[" + group + "_" + i + "]");
                }
                byte[] buffer = createBuffer(modelPath);
                CubismMotion tmp = loadMotion(buffer, motionConsistency);
                if (tmp == null) continue;
                final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, i);
                if (fadeInTime != -1.0f) tmp.setFadeInTime(fadeInTime);
                final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, i);
                if (fadeOutTime != -1.0f) tmp.setFadeOutTime(fadeOutTime);
                tmp.setEffectIds(eyeBlinkIds, lipSyncIds);
                motions.put(name, tmp);
            }
        }
    }

    private void setupTextures() {
        for (int modelTextureNumber = 0; modelTextureNumber < modelSetting.getTextureCount(); modelTextureNumber++) {
            if (modelSetting.getTextureFileName(modelTextureNumber).equals("")) continue;
            String texturePath = modelSetting.getTextureFileName(modelTextureNumber);
            texturePath = modelHomeDirectory + texturePath;
            LAppTextureManager.TextureInfo texture =
                LAppDelegate.getInstance().getTextureManager().createTextureFromPngFile(texturePath);
            final int glTextureNumber = texture.id;
            this.<CubismRendererAndroid>getRenderer().bindTexture(modelTextureNumber, glTextureNumber);
            this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(LAppDefine.PREMULTIPLIED_ALPHA_ENABLE);
        }
    }

    private ICubismModelSetting modelSetting;
    private String modelHomeDirectory;
    private float userTimeSeconds;
    private final List<CubismId> eyeBlinkIds = new ArrayList<>();
    private final List<CubismId> lipSyncIds = new ArrayList<>();
    /** Mouth open value driven by TTS (0.0 = closed, 1.0 = open) */
    private float lipSyncValue = 0.0f;
    public void setLipSyncValue(float v) { this.lipSyncValue = Math.max(0f, Math.min(1f, v)); }
    private final Map<String, ACubismMotion> motions = new HashMap<>();
    private final Map<String, ACubismMotion> expressions = new HashMap<>();
    private final CubismId idParamAngleX;
    private final CubismId idParamAngleY;
    private final CubismId idParamAngleZ;
    private final CubismId idParamBodyAngleX;
    private final CubismId idParamEyeBallX;
    private final CubismId idParamEyeBallY;
    private final CubismOffscreenSurfaceAndroid renderingBuffer = new CubismOffscreenSurfaceAndroid();
}
