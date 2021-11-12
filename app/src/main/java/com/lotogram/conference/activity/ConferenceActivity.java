package com.lotogram.conference.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseLongArray;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lotogram.conference.R;
import com.lotogram.conference.SetVolumeAudioFilter;
import com.lotogram.conference.widget.AspectTextureView;
import com.lotogram.conference.widget.DragNodePlayerView;

import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;
import me.lake.librestreaming.client.RESClient;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.Size;

public class ConferenceActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, NodePlayerDelegate {

    private static final String TAG = ConferenceActivity.class.getSimpleName();

    private RESClient mResClient;
    private RESConfig mResConfig;

    private NodePlayer mNodePlayer;
    private AspectTextureView txv_preview;
    private DragNodePlayerView mNodePlayerView;

    private String mPullAddress;
    private String mPushAddress;
    private String mOrientation;

    private Handler mHandler;
    private SparseLongArray mDelays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);
        showUserName();
        initParams();

        initRtmpPublisher();
        new Handler().postDelayed(this::initNodePlayerView, 5000);
    }

    @Override
    public void onStart() {
        super.onStart();
        mResClient.startStreaming();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Window window = getWindow();
        View view = window.getDecorView();
        int mSystemUiFlag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        view.setSystemUiVisibility(mSystemUiFlag);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mResClient.stopStreaming();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mResClient != null) {
            mResClient.stopPreview(true);
            mResClient.destroy();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mResClient.startPreview(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mResClient.updatePreview(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mResClient != null) {

        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    private void showUserName() {
        Intent intent = getIntent();
        String user = intent.getStringExtra("user");
        ((TextView) findViewById(R.id.user)).setText(user);
    }

    private void initParams() {
        Log.d(TAG, "initParams");
        Intent intent = getIntent();
        mPullAddress = intent.getStringExtra("pull");
        mPushAddress = intent.getStringExtra("push");
        mOrientation = intent.getStringExtra("orientation");
    }

    private void initNodePlayerView() {
        mNodePlayerView = findViewById(R.id.node_player_view);
        mNodePlayerView.setRenderType(NodePlayerView.RenderType.SURFACEVIEW);
        mNodePlayerView.setUIViewContentMode(NodePlayerView.UIViewContentMode.ScaleAspectFill);
        mNodePlayer = new NodePlayer(this);
        mNodePlayer.setAudioEnable(true);
        mNodePlayer.setBufferTime(33);
        mNodePlayer.setMaxBufferTime(66);
        mNodePlayer.setHWEnable(true);
        mNodePlayer.setSubscribe(false);
        mNodePlayer.setPlayerView(mNodePlayerView);
        mNodePlayer.setInputUrl(mPullAddress);
        mNodePlayer.setNodePlayerDelegate(this);
        mNodePlayer.start();
        detectDelay();
    }

    private void initRtmpPublisher() {
        txv_preview = findViewById(R.id.txv_preview);
        txv_preview.setSurfaceTextureListener(this);

        mResClient = new RESClient();
        mResConfig = RESConfig.obtain();
        mResConfig.setRtmpAddr(mPushAddress);
        mResConfig.setFilterMode(RESConfig.FilterMode.HARD);
        mResConfig.setTargetVideoSize(new Size(1280, 720));
        mResConfig.setBitRate(4 * 1024 * 1024);
        mResConfig.setVideoFPS(60);
        mResConfig.setVideoGOP(1);
        // setrender mode in softmode
        mResConfig.setRenderingMode(RESConfig.RenderingMode.OpenGLES);
        mResConfig.setDefaultCamera(mOrientation.equals("前置") ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);

        int frontDirection, backDirection;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
        frontDirection = cameraInfo.orientation;
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
        backDirection = cameraInfo.orientation;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mResConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
            mResConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270));
        } else {
            mResConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180));
            mResConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
        }

        if (!mResClient.prepare(mResConfig)) {
            Log.d(TAG, "prepare,failed!!");
        }

        Size s = mResClient.getVideoSize();
        txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) s.getWidth()) / s.getHeight());

//        mResClient.setVideoChangeListener((width, height) -> {
//            double ratio = ((double) width) / height;
//            txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ratio);
//        });
        mResClient.setSoftAudioFilter(new SetVolumeAudioFilter());
    }

    @Override
    public void onEventCallback(NodePlayer player, int event, String msg) {
        Log.d(TAG, "NodePlayer事件: " + event);
        switch (event) {
            case 1000://正在连接视频
                break;
            case 1001://视频连接成功
                break;
            case 1002://视频连接失败,流地址不存在,或者本地网络无法和服务端通信,5秒后重连,可停止
                break;
            case 1003://视频开始重连,自动重连总开关
                break;
            case 1004://视频播放结束
                break;
            case 1005://网络异常,播放中断,播放中途网络异常,1秒后重连,可停止
                break;
        }
    }

    private void detectDelay() {
        mHandler = new Handler(Looper.getMainLooper()) {

            private long lastResetTime = System.currentTimeMillis();

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                if (mNodePlayer != null) {
                    long delay = mNodePlayer.getBufferPosition() - mNodePlayer.getCurrentPosition();
                    mNodePlayerView.setDelay(delay);

                    if (mDelays == null) {
                        mDelays = new SparseLongArray(5);
                    }

                    mDelays.put(msg.what % 5, delay);

                    int sum = 0;
                    for (int i = 0; i < mDelays.size(); i++) {
                        sum += mDelays.get(i);
                    }

                    Log.d(TAG, mDelays.toString());

                    if (sum >= 400 * mDelays.size() && System.currentTimeMillis() - lastResetTime > 10 * 1000) {
                        lastResetTime = System.currentTimeMillis();
                        if (mNodePlayer != null) {
                            mNodePlayer.stop();
                            mNodePlayer.setInputUrl(mPullAddress);
                            mNodePlayer.start();
                            Toast.makeText(ConferenceActivity.this, "播放器刷新", Toast.LENGTH_LONG).show();
                        }
                    }

                }
                if (mHandler != null) {
                    mHandler.sendEmptyMessageDelayed(msg.what++, 1000);
                }
            }
        };
        mHandler.sendEmptyMessage(0);
    }
}