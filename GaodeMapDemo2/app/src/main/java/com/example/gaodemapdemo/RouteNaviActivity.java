package com.example.gaodemapdemo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.amap.api.maps.AMapException;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 按照选定策略导航
 */
public class RouteNaviActivity extends Activity implements AMapNaviListener, AMapNaviViewListener{

    private AMapNaviView mAMapNaviView;
    private AMapNavi mAMapNavi;
    boolean mIsGps;
    private BluetoothService mBluetoothService;
    private boolean mServiceBound = false;
    private boolean success;
    //帧头、帧尾
    final byte FRAME_HEADER = (byte) 0x40;//@
    final byte FRAME_FOOTER = (byte) 0x23;//#
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_route_navi);

        mAMapNaviView = findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);
        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }
        mAMapNavi.addAMapNaviListener(this);
        mAMapNavi.setUseInnerVoice(true);
        mAMapNavi.setEmulatorNaviSpeed(60);

        try {
            getNaviParam();
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }

        // 启动服务
        Intent intent = new Intent(this, BluetoothService.class);
        // 绑定到服务,BIND_AUTO_CREATE 表示在活动和服务进行绑定后自动创建服务
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
    * 获取intent参数并计算路线
     */
    private void getNaviParam() throws AMapException {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        mIsGps = intent.getBooleanExtra("gps", false);
        if (mIsGps) {
            mAMapNavi.startNavi(1); //实时导航
        } else {
            mAMapNavi.startNavi(2); //模拟导航
        }

        switch(intent.getIntExtra("NaviMode",0)){
            case 0:
                NaviLatLng WalkStart = intent.getParcelableExtra("start");
                NaviLatLng WalkEnd = intent.getParcelableExtra("end");
                mAMapNavi.calculateWalkRoute(WalkStart,WalkEnd);
                break;
            case 1:
                NaviLatLng start = intent.getParcelableExtra("start");
                NaviLatLng end = intent.getParcelableExtra("end");
                mAMapNavi.calculateRideRoute(start,end);
//                calculateDriveRoute(start, end);
                break;
            default:
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAMapNaviView.onPause();
        //停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完
        //        mAMapNavi.stopNavi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAMapNaviView.onDestroy();
        if (mAMapNavi != null){
            mAMapNavi.stopNavi();
            mAMapNavi = null;
        }
        AMapNavi.destroy();

        // 解绑服务
        if (mServiceConnection != null && mServiceBound ) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    @Override
    public void onInitNaviFailure() {
        Toast.makeText(this, "init navi Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInitNaviSuccess() {
    }

    @Override
    public void onStartNavi(int type) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation location) {

    }

    @Override
    public void onGetNavigationText(int type, String text) {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onEndEmulatorNavi() {
    }

    @Override
    public void onArriveDestination() {
    }

    @Override
    public void onCalculateRouteFailure(int errorInfo) {
    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int wayID) {

    }

    @Override
    public void onGpsOpenStatus(boolean enabled) {
    }

    @Override
    public void onNaviSetting() {
    }

    @Override
    public void onNaviMapMode(int isLock) {

    }

    @Override
    public void onNaviCancel() {
        finish();
        byte[] falseFlag = "end".getBytes(StandardCharsets.UTF_8);
        try {
            mBluetoothService.writeSerial(falseFlag);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onNaviTurnClick() {

    }

    @Override
    public void onNextRoadClick() {

    }

    @Override
    public void onScanViewButtonClick() {
    }


    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {
        if (success) {
            int distance = naviInfo.getCurStepRetainDistance();
            int iconType = naviInfo.getIconType();
            // 启动数据传输
            if(mBluetoothService != null){
                packAndSendString(distance,iconType);
            }
        }
        /** * 更新下一路口 路名及 距离 */
//        Log.e("TAG","下个路口:"+naviInfo.getNextRoadName()+" 当前路段剩余距离:"+ naviInfo.getCurStepRetainDistance()+"当前路段剩余时间"+naviInfo.getCurStepRetainTime()+" 总路程剩余距离："+naviInfo.getPathRetainDistance());
        Log.e("TAG","当前路段剩余距离:"+ naviInfo.getCurStepRetainDistance()+"方向："+naviInfo.getIconType());
    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {
    }

    @Override
    public void hideCross() {
    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] laneInfos, byte[] laneBackgroundInfo, byte[] laneRecommendedInfo) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        if (mIsGps) {
            mAMapNavi.startNavi(1); //实时导航
        } else {
            mAMapNavi.startNavi(2); //模拟导航
        }
    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }

    @Override
    public void onGpsSignalWeak(boolean b) {

    }

    @Override
    public void onLockMap(boolean isLock) {
    }

    @Override
    public void onNaviViewLoaded() {
    }

    @Override
    public void onMapTypeChanged(int i) {

    }

    @Override
    public void onNaviViewShowMode(int i) {

    }

    @Override
    public boolean onNaviBackClick() {
        return false;
    }

    // 创建服务连接对象
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        //连接服务成功时被调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mBluetoothService = binder.getService();
            mServiceBound = true;
            //判断是否有蓝牙设备连接
            success = mBluetoothService.isBluetoothDevice();

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };

    //数据打包
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void packAndSendString(int distance, int iconType) {
        // 将两个int数据转换为字节数组
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 2);
        String interim = "" + distance +"$"+ iconType;
        buffer.put(interim.getBytes());
        // 创建最终的帧数组，加上1字节的帧头和1字节的帧尾
        byte[] frame = new byte[buffer.position() + 2];
        // 设置帧头
        frame[0] = FRAME_HEADER;
        // 将缓冲区的数据复制到帧数组中，从第二个位置开始
        System.arraycopy(interim.getBytes(), 0, frame, 1, buffer.position());
        // 设置帧尾
        frame[frame.length - 1] = FRAME_FOOTER;
        mBluetoothService.sendNaviData(frame);
    }
}
