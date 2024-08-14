package com.example.gaodemapdemo;

import static com.example.gaodemapdemo.BluetoothService.SharedData.connectFlag;
import static com.example.gaodemapdemo.BluetoothService.SharedData.mBtFlag;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlueToothActivity extends AppCompatActivity implements View.OnClickListener{
    private final String TAG="蓝牙";
    private BluetoothAdapter mBtAdapter;
    //    private boolean mBtFlag;
    private BluetoothSocket mBtSocket;
    private BluetoothDevice mBtDevice;
    private static final String HC_MAC="58:56:00:01:21:B2";
    private EditText Message;
    private Button BtSend;
    private Button Btopen;
    private Button Btclose;
    private FloatingActionButton openNaviButton;
    private TextView Tv;
    private TextView Tvdevice_name;
    private TextView Tvdevice_address;
    ImageView disconnect,connected;
    private final int Message_Arduino = 1;
    private final int Message_Android = 2;
    private final int Message_ConnectError = 3;
    private final int Bt_Flag = 0;
    //用来更新UI
    private Handler UIHandler;
    private BluetoothService mBluetoothService;
    private boolean mServiceBound = false;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);

        // 启动服务
        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);
        // 绑定到服务
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        Message=(EditText) findViewById(R.id.edit_message);
        BtSend= (Button) findViewById(R.id.BtSend);
        BtSend.setOnClickListener(v -> {
            String MessageContent = Message.getText().toString();
            if (!MessageContent.isEmpty()){
                try {
                    mBluetoothService.writeSerial(MessageContent.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Message.setText("");
            }
            else {
                showMsg("发送的消息不能为空。");
            }
        });
        Btopen = (Button) findViewById(R.id.Btopen);

        Btopen.setOnClickListener(v -> {
            if(!mBtFlag.get()){ //如果此时未连接
                try {
                    mBluetoothService.myStartService();
                    showMsg("连接中~");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                while (!mBtFlag.get()) {
                    if(!connectFlag.get()){
                        showMsg("连接失败");
                        connectFlag.set(true);
                        break;
                    }
                }
                if(mBtFlag.get()){
                    mBtDevice = mBluetoothService.getmBtDevice();
                    updateUI(true);
                    byte[] successFlag = "win".getBytes(StandardCharsets.UTF_8);
                    try {
                        mBluetoothService.writeSerial(successFlag);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }else if(mBtSocket != null && mBtSocket.isConnected()){
                showToast("已连接，请勿重复点击");
            }
        });
        Btclose = (Button) findViewById(R.id.Btclose);
        Btclose.setOnClickListener(v -> {
            if(!mBtFlag.get()){ //如果此时未连接
                showToast("当前未连接");
                showMsg("当前未连接");
            }else{
                updateUI(false);
                byte[] falseFlag = "false".getBytes(StandardCharsets.UTF_8);
                try {
                    mBluetoothService.writeSerial(falseFlag);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    mBluetoothService.myStopService();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        openNaviButton = findViewById(R.id.openNaviButton);
        openNaviButton.setOnClickListener(this);

        Tv=(TextView) findViewById(R.id.TV);
        Tvdevice_address= (TextView)findViewById(R.id.device_address);
        Tvdevice_address.setText(HC_MAC);
        Tvdevice_name=(TextView)findViewById(R.id.device_name);
        disconnect = findViewById(R.id.disconnect);
        connected = findViewById(R.id.connected);
        //创建属于主线程的handler
        UIHandler=new Handler();
    }

    /**
    * 进入路线规划
     * @param view
     */
    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.openNaviButton){
            startActivity(new Intent(this,RouteActivity.class));
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //断开蓝牙连接
//        if (mServiceBound) {
    //        try {
    //            mBluetoothService.myStopService();
    //            updateUI(false);
    //        } catch (IOException e) {
    //            throw new RuntimeException(e);
    //        }
//        }
        // 解绑服务
//        if (mServiceConnection != null && mServiceBound) {
//            unbindService(mServiceConnection);
//            mServiceBound = false;
//        }
    }

//    /**
//     * Called by onDestroyCommand
//     */
//    private void myStopService() throws IOException {
//        if(mThreadFlag && !mThread.isInterrupted()){
//            mThread.interrupt();
//            mThreadFlag = false;
//        }
//        updateUI(false);
//        if (mBtSocket != null && mBtSocket.isConnected()) {
//            try {
//                mBtSocket.close();
//                mBtSocket = null;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        mBtDevice = null;
//        mBtFlag = false;
//    }


    public void showToast(String str){
        Log.d(TAG,str);
    }

    /*
     * 创建一个新线程更新界面
     */
    public void updateUI(boolean flag){
        if(flag) {
            // 构建Runnable对象，在runnable中更新界面
            final Runnable runnableUi = new Runnable() {
                @Override
                public void run() {
                    if(mBtDevice != null){
                        Tvdevice_name.setText(mBtDevice.getName());
                        Tvdevice_address.setText(mBtDevice.getAddress());
                        Btopen.setEnabled(false);
                        Btclose.setEnabled(true);
                        BtSend.setEnabled(true);
                        connected.setVisibility(View.VISIBLE);
                        disconnect.setVisibility(View.INVISIBLE);
                        Context context = getApplicationContext();
                        Toast.makeText(context, "连接成功~", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            new Thread(){
                public void run(){
                    UIHandler.post(runnableUi);
                }
            }.start();
        }else{
            // 构建Runnable对象，在runnable中更新界面
            @SuppressLint("SetTextI18n") final Runnable runnableUi = () -> {
                Tvdevice_name.setText("");
                Tvdevice_address.setText("");
                Btopen.setEnabled(true);
                Btclose.setEnabled(false);
                BtSend.setEnabled(false);
                connected.setVisibility(View.INVISIBLE);
                disconnect.setVisibility(View.VISIBLE);
                Context context = getApplicationContext();
                Toast.makeText(context, "已断开连接", Toast.LENGTH_SHORT).show();
            };
            new Thread(){
                public void run(){
                    UIHandler.post(runnableUi);
                }
            }.start();
        }
    }

    //将发送的消息显示在主线程上
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case Message_Arduino:
                    showToast("Arduino:"+msg.obj);
                    Tv.append("Arduino:"+msg.obj+"\n");
                    break;
                case Message_Android:
                    showToast("Android:"+msg.obj);
//                    Tv.append("Android:"+msg.obj+"\n");
                    break;
                case Message_ConnectError:
                    Tv.append(msg.obj.toString());
            }
        }
    };

    //Android 6.0（API级别23）及以上版本检查
    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 版本大于等于 Android12 时
            String[] permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            // Android 版本小于 Android12 及以下版本
            String[] permissions = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户已同意所有请求的权限
                mBtAdapter.enable(); //申请打开蓝牙
            } else {
                // 用户拒绝了至少一个权限
                showMsg("蓝牙未打开，请打开蓝牙。");
            }
        }
    }
    /**
      * Toast提示
      * @param msg 提示内容
     **/
    private void showMsg(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show();
    }

    // 创建服务连接对象
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mBluetoothService = binder.getService();
            mServiceBound = true;
            //蓝牙权限
            initPermission();
            mBtSocket = mBluetoothService.getmBtSocket();
            mBtDevice = mBluetoothService.getmBtDevice();
            mBtAdapter = mBluetoothService.getmBtAdapter();
            mBtFlag = mBluetoothService.getmBtFlagValue();//读取多次确保同步跟新了
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };
}

