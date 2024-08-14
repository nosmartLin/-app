package com.example.gaodemapdemo;

import static com.example.gaodemapdemo.BluetoothService.SharedData.connectFlag;
import static com.example.gaodemapdemo.BluetoothService.SharedData.mBtFlag;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothService extends Service implements MyBinder {
    private final String TAG="蓝牙";
    private BluetoothAdapter mBtAdapter;
//    private boolean mBtFlag;
    private Thread mThread;
    //判断是否退出线程
    private boolean mThreadFlag;
    private BluetoothSocket mBtSocket;
    private BluetoothDevice mBtDevice;
    private static final UUID HC_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String HC_MAC="58:56:00:01:21:B2";
    private InputStream inStream;
    private OutputStream outStream;
    private final IBinder mBinder = new LocalBinder();
//    private Handler mHandler;

    public BluetoothService() {
    }
    public class SharedData {
        public static AtomicBoolean mBtFlag = new AtomicBoolean(false);
        public static AtomicBoolean connectFlag = new AtomicBoolean(true);
    }
    @Override
    public AtomicBoolean getmBtFlagValue() {
        return mBtFlag;
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }
    // onCreate()方法会在服务创建的时候调用
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyService", "服务已创建");
    }
    // onStartCommand()方法会在每次服务启动的时候调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", "服务启动");
        return super.onStartCommand(intent, flags, startId);
    }

    // onDestroy()方法会在服务销毁的时候 调用
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MyService", "服务已销毁");
    }

    public void showToast(String str){
        Log.d(TAG,str);
    }
    /**
     * Called by onStartCommand, initialize and start runtime thread
     */
    public void myStartService() throws IOException {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( mBtAdapter == null ) {
            showToast("Bluetooth unused.");
            mBtFlag.set(false);
            return;
        }
        if ( !mBtAdapter.isEnabled() ) {
            mBtFlag.set(false);
            myStopService();
            showToast("Open bluetoooth then restart program!!");
            return;
        }
        showToast("Start searching!!");
        mThreadFlag = true;
        mThread = new MyThread();
        mThread.start();
    }
    /**
     * Called by onDestroyCommand
     */
    public void myStopService() throws IOException {
        if(mThreadFlag && !mThread.isInterrupted()){
            mThread.interrupt();
            mThreadFlag = false;
        }
        if (mBtSocket != null && mBtSocket.isConnected()) {
            try {
                mBtSocket.close();
                mBtSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mBtDevice = null;
        mBtFlag.set(false);
    }

    /**
     * Thread runtime
     */
    public class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
            myBtConnect();
            while(mThreadFlag && !Thread.currentThread().isInterrupted()) {
                readSerial();//读取蓝牙数据
                try{
                    Thread.sleep(300);
                }catch(Exception  e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt(); // 重新设置中断状态
                }
            }
        }
    }

    /**
     * 尝试连接指定的蓝牙设备。
     * 通过蓝牙适配器获取远程蓝牙设备，然后创建一个与该设备的RFCOMM服务记录的Socket连接。
     * 如果连接成功，则更新UI并开启蓝牙通信；如果连接失败，则关闭Socket并提示用户连接错误。
     */
    public void myBtConnect() {
        showToast("Connecting...");
        //查找蓝牙设备
        mBtDevice = mBtAdapter.getRemoteDevice(HC_MAC);
        // 创建与蓝牙设备的Socket连接
        try {
            mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(HC_UUID);
            showToast("Connecting to "+mBtSocket.getRemoteDevice().getName());
        } catch (IOException e) {
            e.printStackTrace();
            mBtFlag.set(false);
            showToast("创建蓝牙连接失败");
        }
        // 取消蓝牙设备发现
        mBtAdapter.cancelDiscovery();

        /* Setup connection */
        try {// 连接蓝牙设备
            mBtSocket.connect();
            showToast("蓝牙连接成功。");
            Log.i(TAG, "Connect " + HC_MAC + " Success!");
            mBtFlag.set(true);
            connectFlag.set(true);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                showToast("蓝牙连接失败。");
//                mHandler.obtainMessage(Message_ConnectError,"蓝牙连接失败，请重试。\n").sendToTarget();
                mBtFlag.set(false);
                connectFlag.set(false);
                if(mBtSocket != null){
                    mBtSocket.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        // 初始化输入输出流
        if ( mBtFlag.get() ) {
            try {
                inStream  = mBtSocket.getInputStream();
                outStream = mBtSocket.getOutputStream();
                showToast("可以发送数据了。");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Read serial data from HC06
     */
    public int readSerial() {
        int ret = 0;
        byte[] rsp;

        if ( !mBtFlag.get() ) {
            return -1;
        }
        try {
            rsp = new byte[inStream.available()];
            ret = inStream.read(rsp);
            String con = new String(rsp);
            if(ret != 0){
                showToast(con);
            }
//            if(!con.isEmpty())
//                mHandler.obtainMessage(Message_Arduino,con).sendToTarget();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Write serial data to HC06
     * @param value - command
     */
    public void writeSerial(byte[] value) throws IOException {
        outStream = mBtSocket.getOutputStream();
        showToast("发送了数据");
        byte[] ha = value;
//        mHandler.obtainMessage(Message_Android,ha).sendToTarget();
        try {
            if (outStream != null) {
                outStream.write(ha);
            } else {
                Log.e("TAG", "OutputStream is null");
            }
            outStream.flush();  //清空输出缓冲区并立即将数据写入底层的输出流
            Thread.sleep(100);  //连续发送的间隔为100毫秒。
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isBluetoothDevice(){
        if(mBtDevice != null)
            return true;
        else
            return false;
    }

    public BluetoothSocket getmBtSocket(){
        return mBtSocket;
    }
    public BluetoothDevice getmBtDevice(){
        return mBtDevice;
    }
    public BluetoothAdapter getmBtAdapter(){
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBtAdapter;
    }
    public void sendNaviData(byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    writeSerial(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}