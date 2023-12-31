package com.dom.camera.dom_camera;

import android.content.Context;
import android.os.Environment;
import android.view.ViewGroup;

import com.lib.MsgContent;
import com.lib.sdk.bean.HandleConfigData;
import com.lib.sdk.bean.HumanDetectionBean;
import com.lib.sdk.bean.JsonConfig;
import com.lib.sdk.bean.PtzCtrlInfoBean;
import com.lib.sdk.struct.H264_DVR_FILE_DATA;
import com.manager.device.DeviceManager;
import com.manager.device.config.DevConfigInfo;
import com.manager.device.config.DevConfigManager;
import com.manager.device.media.MediaManager;
import com.manager.device.media.attribute.PlayerAttribute;
import com.manager.device.media.audio.OnAudioDecibelListener;
import com.manager.device.media.monitor.MonitorManager;
import com.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DeviceClass {

  private static MonitorManager monitorManager;
  private static List<H264_DVR_FILE_DATA> recordList = new ArrayList<>();
  private List<Map<String, Object>> recordTimeList = new ArrayList<>();
  public static final int TIME_UNIT = 60;
  private int timeUnit = TIME_UNIT;
  public static final int MN_COUNT = 8;
  private int timeCount = MN_COUNT;

  public interface myDomResultInterface {
    void onSuccess(List<String> dataList);
    void onFailed(String errorId, String message);
  }

  static void cameraLogin(
    String cameraId,
    DeviceManager.OnDevManagerListener onDevManagerListener
  ) {
    List devList = UserClass.DeviceList();
    boolean cameraIdExists = false;

    for (Object map : devList) {
      if (map instanceof Map) {
        Object devIdValue = ((Map<?, ?>) map).get("devId");

        if (devIdValue != null && devIdValue.toString().equals(cameraId)) {
          cameraIdExists = true;
          break;
        }
      }
    }

    if (cameraIdExists) {
      DeviceManager.getInstance().loginDev(cameraId, onDevManagerListener);
    } else {
      onDevManagerListener.onFailed("0", 0, "0", 0);
    }
  }

  static void stopStream() {
    monitorManager.destroyPlay();
  }

  static void ptzControl(String cameraId, int commandID, boolean isStop) {
    PtzCtrlInfoBean ptzCtrlInfoBean = new PtzCtrlInfoBean();
    ptzCtrlInfoBean.setPtzCommandId(commandID);
    ptzCtrlInfoBean.setDevId(cameraId);
    ptzCtrlInfoBean.setSpeed(1);
    ptzCtrlInfoBean.setStop(isStop);
    DeviceManager
      .getInstance()
      .devPTZControl(
        ptzCtrlInfoBean,
        new DeviceManager.OnDevManagerListener() {
          @Override
          public void onSuccess(
            String devId,
            int operationType,
            Object result
          ) {
            System.out.println("Success");
          }

          @Override
          public void onFailed(
            String devId,
            int msgId,
            String jsonName,
            int errorId
          ) {
            System.out.println("Failure");
          }
        }
      );
  }

  static void HumanDetection(
    String cameraId,
    boolean isEnabled,
    myDomResultInterface resultCb
  ) {
    DevConfigManager devConfigManager = DeviceManager
      .getInstance()
      .getDevConfigManager(cameraId);
    HumanDetectionBean humanDetectionBean = new HumanDetectionBean();
    humanDetectionBean.setEnable(isEnabled);
    DevConfigInfo devConfigInfo = DevConfigInfo.create(
      new DeviceManager.OnDevManagerListener() {
        @Override
        public void onSuccess(String devId, int operationType, Object result) {
          resultCb.onSuccess(new ArrayList<>());
        }

        @Override
        public void onFailed(
          String devId,
          int msgId,
          String jsonName,
          int errorId
        ) {
          resultCb.onFailed("0", "0");
        }
      }
    );
    devConfigInfo.setChnId(0);
    devConfigInfo.setJsonName(JsonConfig.DETECT_HUMAN_DETECTION);
    devConfigInfo.setJsonData(
      HandleConfigData.getSendData(
        HandleConfigData.getFullName(JsonConfig.DETECT_HUMAN_DETECTION, 0),
        "0x08",
        humanDetectionBean
      )
    );

    devConfigManager.setDevConfig(devConfigInfo);
  }

  static void liveStream(
          Context context,
          String cameraId,
          ViewGroup view,
          myDomResultInterface resultCb) {
    System.out.println("Live stream Started");
    monitorManager =
            DeviceManager.getInstance().createMonitorPlayer(view, cameraId);
    monitorManager.startMonitor();
    monitorManager.setChnId(1);
    monitorManager.setOnAudioDecibelListener(
            new OnAudioDecibelListener() {
              @Override
              public void onVolume(double v) {}
            }
    );
    monitorManager.setOnMediaManagerListener(new MediaManager.OnMediaManagerListener() {
      @Override
      public void onMediaPlayState(PlayerAttribute attribute, int state) {

      }

      @Override
      public void onFailed(PlayerAttribute attribute, int msgId, int errorId) {
        resultCb.onFailed("0", "0");
      }

      @Override
      public void onShowRateAndTime(PlayerAttribute attribute, boolean isShowTime, String time, String rate) {

      }

      @Override
      public void onVideoBufferEnd(PlayerAttribute attribute, MsgContent ex) {
        resultCb.onSuccess(new ArrayList<>());
      }
    });
  }

  public static void capture(Context context) {
    String galleryPath =
      Environment.getExternalStorageDirectory() +
      File.separator +
      Environment.DIRECTORY_DCIM +
      File.separator +
      "Camera" +
      File.separator;

    if (!FileUtils.isFileAvailable(galleryPath)) {
      galleryPath =
        Environment.getExternalStorageDirectory() +
        File.separator +
        Environment.DIRECTORY_DCIM +
        File.separator;
    }

    monitorManager.capture(galleryPath);
  }

  public static void startRecord() {
    String galleryPath =
      Environment.getExternalStorageDirectory() +
      File.separator +
      Environment.DIRECTORY_DCIM +
      File.separator +
      "Camera" +
      File.separator;

    if (!FileUtils.isFileAvailable(galleryPath)) {
      galleryPath =
        Environment.getExternalStorageDirectory() +
        File.separator +
        Environment.DIRECTORY_DCIM +
        File.separator;
    }

    if (!monitorManager.isRecord()) {
      monitorManager.startRecord(galleryPath);
    }
  }

  public static void stopRecord() {
    if (monitorManager.isRecord()) {
      monitorManager.stopRecord();
    }
  }

  public static void openVoice() {
    monitorManager.openVoiceBySound();
  }

  public static void closeVoice() {
    monitorManager.closeVoiceBySound();
  }

  public static void startSingleIntercomAndSpeak(Context context) {
    if (monitorManager == null) return;

    monitorManager.startTalkByHalfDuplex(context);
  }

  public static void stopSingleIntercomAndHear() {
    if (monitorManager == null) return;

    monitorManager.stopTalkByHalfDuplex();
  }

  public static void startDoubleIntercom(Context context) {
    if (monitorManager == null) return;

    monitorManager.startTalkByDoubleDirection(context, true);
  }

  public static void stopIntercom() {
    if (monitorManager == null) return;

    monitorManager.destroyTalk();
  }
}
