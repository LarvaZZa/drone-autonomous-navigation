package com.dji.ux.sample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dji.mapkit.core.maps.DJIMap;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.panel.CameraSettingAdvancedPanel;
import dji.ux.panel.CameraSettingExposurePanel;
import dji.ux.utils.DJIProductUtil;
import dji.ux.widget.FPVOverlayWidget;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.ThermalPaletteWidget;
import dji.ux.widget.config.CameraConfigApertureWidget;
import dji.ux.widget.config.CameraConfigEVWidget;
import dji.ux.widget.config.CameraConfigISOAndEIWidget;
import dji.ux.widget.config.CameraConfigSSDWidget;
import dji.ux.widget.config.CameraConfigShutterWidget;
import dji.ux.widget.config.CameraConfigStorageWidget;
import dji.ux.widget.config.CameraConfigWBWidget;
import dji.ux.widget.controls.CameraControlsWidget;
import dji.ux.widget.controls.LensControlWidget;

/**
 * Activity that shows all the UI elements together
 */
public class CompleteWidgetActivity extends Activity implements View.OnClickListener{

    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private FPVOverlayWidget fpvOverlayWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private CameraSettingExposurePanel cameraSettingExposurePanel;
    private CameraSettingAdvancedPanel cameraSettingAdvancedPanel;
    private CameraConfigISOAndEIWidget cameraConfigISOAndEIWidget;
    private CameraConfigShutterWidget cameraConfigShutterWidget;
    private CameraConfigApertureWidget cameraConfigApertureWidget;
    private CameraConfigEVWidget cameraConfigEVWidget;
    private CameraConfigWBWidget cameraConfigWBWidget;
    private CameraConfigStorageWidget cameraConfigStorageWidget;
    private CameraConfigSSDWidget cameraConfigSSDWidget;
    private CameraControlsWidget controlsWidget;
    private LensControlWidget lensControlWidget;
    private ThermalPaletteWidget thermalPaletteWidget;


    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;


    private Button startVSBtn;
    private Button stopBtn;
    private Button calcBtn;
    private FlightController flightController;
    private ScheduledExecutorService scheduleTaskExecutor;
    private float pitch = 0;
    private float roll = 0;
    private float yaw = 0;
    private float verticalThrottle = 1;

    //DON'T FORGET TO ADJUST
    private LocationCoordinate3D targetLocation = new LocationCoordinate3D(52.240421, 6.849494, 3f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);

        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        deviceHeight = outPoint.y;
        deviceWidth = outPoint.x;

        mapWidget = (MapWidget) findViewById(R.id.map_widget);
        mapWidget.initAMap(map -> map.setOnMapClickListener((DJIMap.OnMapClickListener) latLng -> onViewClick(mapWidget)));
        mapWidget.onCreate(savedInstanceState);

        initCameraView();
        parentView = (ViewGroup) findViewById(R.id.root_view);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(view -> onViewClick(fpvWidget));
        fpvOverlayWidget = findViewById(R.id.fpv_overlay_widget);
        primaryVideoView = findViewById(R.id.fpv_container);
        secondaryVideoView = findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(view -> swapVideoSource());

        startVSBtn = (Button) parentView.findViewById(R.id.virtual_sticks_button);
        startVSBtn.setOnClickListener(this);
        stopBtn = (Button) parentView.findViewById(R.id.stop_button);
        stopBtn.setOnClickListener(this);
        calcBtn = (Button) parentView.findViewById(R.id.start_calculations_button);
        calcBtn.setOnClickListener(this);
        flightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();

        fpvWidget.setCameraIndexListener((cameraIndex, lensIndex) -> cameraWidgetKeyIndexUpdated(fpvWidget.getCameraKeyIndex(), fpvWidget.getLensKeyIndex()));
        updateSecondaryVideoVisibility();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.virtual_sticks_button:
                enableVirtualSticks();
                break;
            case R.id.stop_button:
                stopVirtualSticks();
                break;
            case R.id.start_calculations_button:
                verticalThrottle = targetLocation.getAltitude();

                scheduleTaskExecutor = Executors.newScheduledThreadPool(2);
                startCalculations();
                startUploadVirtualSticksData();
                break;
        }
    }

    private void startCalculations() {
        scheduleTaskExecutor.scheduleAtFixedRate(this::calculations, 0, 800, TimeUnit.MILLISECONDS);
    }


    private void calculations() {
        LocationCoordinate3D droneCurrentLocation = new LocationCoordinate3D(flightController.getState().getAircraftLocation().getLatitude(),
                flightController.getState().getAircraftLocation().getLongitude(),
                flightController.getState().getAircraftLocation().getAltitude());

        double bearing = bearing(droneCurrentLocation, targetLocation);
        double distance = distance(droneCurrentLocation, targetLocation);
        double VSAltitudeDifference = Math.abs(Math.round(droneCurrentLocation.getAltitude())-targetLocation.getAltitude());

        if(VSAltitudeDifference>0.5){
            yaw = (float) bearing;
        } else if (Math.abs(flightController.getState().getAttitude().yaw-bearing)>5){
            yaw = (float) bearing;
        } else if (distance>50){
            yaw = (float) bearing;
            roll = 5;
        } else if (distance<=50 && distance>1) {
            yaw = (float) bearing;
            roll = 2;
        } else {
            yaw = 0;
            roll = 0;
            stopVirtualSticks();
        }
    }

    private void stopVirtualSticks(){
        if(scheduleTaskExecutor != null){
            scheduleTaskExecutor.shutdown();
            scheduleTaskExecutor = null;
        }
        flightController.setVirtualStickModeEnabled(false,djiError -> {
            if(djiError != null) {
                showToast("Stopping VS error:" + djiError.getDescription());
            } else {
                showToast("VS Stopped!");
            }
        });
    }

    private void startUploadVirtualSticksData() {
        scheduleTaskExecutor.scheduleAtFixedRate(this::uploadVSData, 0, 60, TimeUnit.MILLISECONDS);
    }

    private void uploadVSData(){
        flightController.sendVirtualStickFlightControlData(new FlightControlData(roll, pitch, yaw, verticalThrottle), djiError -> {
            if (djiError != null) {
                showToast(djiError.getDescription());
            }
        });
    }

    private void enableVirtualSticks() {
        flightController.setVirtualStickModeEnabled(true, djiError -> {
            if(djiError == null){
                flightController.setVirtualStickAdvancedModeEnabled(true);
                flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                flightController.setYawControlMode(YawControlMode.ANGLE);
                flightController.setVerticalControlMode(VerticalControlMode.POSITION);
                flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                showToast("VS Sticks Enabled!");
            } else {
                showToast("VS Sticks Enable error: " + djiError.getDescription());
            }
        });
    }

    private static double bearing(LocationCoordinate3D current, LocationCoordinate3D target) {
        double lat1 = current.getLatitude();
        double lon1 = current.getLongitude();
        double lat2 = target.getLatitude();
        double lon2 = target.getLongitude();
        double d, tc1, sn;
        double latAlt, lonAlt, latNeu, lonNeu;

        latAlt = lat1 * Math.PI / 180;
        lonAlt = -lon1 * Math.PI / 180;
        latNeu = lat2 * Math.PI / 180;
        lonNeu = -lon2 * Math.PI / 180;

        d = 2 * Math.asin(Math.sqrt(Math.pow((Math.sin((latAlt-latNeu)/2)),2)+ Math.cos(latAlt)*Math.cos(latNeu)*Math.pow((Math.sin((lonAlt-lonNeu)/2)),2)));

        sn = Math.sin(lonNeu-lonAlt);
        double acos = Math.acos((Math.sin(latNeu) - Math.sin(latAlt) * Math.cos(d)) / (Math.sin(d) * Math.cos(latAlt)));
        if(sn < 0){
            tc1= acos;
        } else {
            tc1=2*Math.PI - acos;
        }

        return tc1*180 / Math.PI;
    }

    public static double distance(LocationCoordinate3D current, LocationCoordinate3D target) {

        double lat1 = current.getLatitude();
        double lon1 = current.getLongitude();
        double el1 = current.getAltitude();
        double lat2 = target.getLatitude();
        double lon2 = target.getLongitude();
        double el2 = target.getAltitude();

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    private void initCameraView() {
        cameraSettingExposurePanel = findViewById(R.id.camera_setting_exposure_panel);
        cameraSettingAdvancedPanel = findViewById(R.id.camera_setting_advanced_panel);
        cameraConfigISOAndEIWidget = findViewById(R.id.camera_config_iso_and_ei_widget);
        cameraConfigShutterWidget = findViewById(R.id.camera_config_shutter_widget);
        cameraConfigApertureWidget = findViewById(R.id.camera_config_aperture_widget);
        cameraConfigEVWidget = findViewById(R.id.camera_config_ev_widget);
        cameraConfigWBWidget = findViewById(R.id.camera_config_wb_widget);
        cameraConfigStorageWidget = findViewById(R.id.camera_config_storage_widget);
        cameraConfigSSDWidget = findViewById(R.id.camera_config_ssd_widget);
        lensControlWidget = findViewById(R.id.camera_lens_control);
        controlsWidget = findViewById(R.id.CameraCapturePanel);
        thermalPaletteWidget = findViewById(R.id.thermal_pallette_widget);
    }

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void cameraWidgetKeyIndexUpdated(int keyIndex, int subKeyIndex) {
        controlsWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraSettingExposurePanel.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraSettingAdvancedPanel.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigISOAndEIWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigShutterWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigApertureWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigEVWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigWBWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigStorageWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigSSDWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        controlsWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        lensControlWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        thermalPaletteWidget.updateKeyOnIndex(keyIndex, subKeyIndex);

        fpvOverlayWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null || !DJIProductUtil.isSupportMultiCamera()) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED, fpvWidget.getCameraKeyIndex()), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED, fpvWidget.getCameraKeyIndex()), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        //findViewById(R.id.simulator_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }
}
