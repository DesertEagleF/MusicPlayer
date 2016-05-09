package com.deserteaglefe.musicplayer.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.deserteaglefe.musicplayer.MusicItem;
import com.deserteaglefe.musicplayer.R;
import com.deserteaglefe.musicplayer.service.MusicService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, SensorEventListener {

    // 常量系列
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String MUSIC_SELECTED_ID = "music_selected_id";
    public static final String MUSIC_LIST = "music_id";
    public static final String ITEM_NAME = "item_name";
    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int SELECT = 2;
    public static final int SEEKING = 3;
    public static final int SOUGHT = 4; // seek的过去式和过去分词是sought →_→
    public static final int PROGRESS = 5;
    public static final int CONTINUE = 6;
    public static final int NEXT = 7;

    // View控件
    private ImageView mPreviousButton;
    private ImageView mPlayButton;
    private ImageView mNextButton;
    private ImageView mCoverImage;
    private ImageView mPlayDiscImage; // 呐，跟我念：PS大法好
    private ImageView mNeedleImage;
    private ImageView mPlayList;
    private ImageView mLoveImage;
    private SeekBar mSeekBar;

    // 基本类型
    private int mProgress;
    private boolean mIsStart = false;
    private boolean mIsSeeking = false;
    private boolean mIsFirstPlay = true;

    // 音乐资源
    private int mMusicId;
    private ArrayList<MusicItem> mMusicList = new ArrayList<>(); // 播放列表

    // 动画相关
    private ObjectAnimator mCoverAnimator;
    private ObjectAnimator mDiscAnimator;
    private Animation mNeedlePlayAnimation;
    private Animation mNeedleStopAnimation;

    // 消息相关
    private Intent mIntent;
    private PlayingHandler mPlayingHandler = new PlayingHandler(this);
    private Messenger mMessenger;
    private Messenger mServiceMessenger;

    // 本周新增：传感器
    private SensorManager mSensorManager;
    private Sensor mSensor;

    // ServiceConnection
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMessenger = new Messenger(service);
            mServiceMessenger = new Messenger(mPlayingHandler);
            Log.i(TAG, "set MusicService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private MusicBroadcastReceiver mMusicBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 绑定服务
        mIntent = new Intent(MainActivity.this, MusicService.class);
        bindService(mIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(mIntent);

        findViews();     // 关联控件
        setListeners();  // 设置监听器
        setReceiver();   // 设置广播接收器
        initAnimation(); // 设定动画
        init();          // 初始化数据
        setSensor();     // 本周新增：传感器
    }

    private void setSensor() {
        // (1)获取SensorManager对象
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // (2)获取Sensor对象
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void findViews() {
        mPreviousButton = (ImageView) findViewById(R.id.previous_button);
        mPlayButton = (ImageView) findViewById(R.id.play_button);
        mNextButton = (ImageView) findViewById(R.id.next_button);
        mCoverImage = (ImageView) findViewById(R.id.cover);
        mPlayDiscImage = (ImageView) findViewById(R.id.play_disc);
        mNeedleImage = (ImageView) findViewById(R.id.play_needle_image);
        mPlayList = (ImageView) findViewById(R.id.play_list);
        mLoveImage = (ImageView) findViewById(R.id.love_it);
        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
    }

    private void initAnimation() {
        mNeedlePlayAnimation = AnimationUtils.loadAnimation(this, R.anim.needle_start);
        mNeedleStopAnimation = AnimationUtils.loadAnimation(this, R.anim.needle_stop);
        mNeedlePlayAnimation.setFillAfter(true);
        mNeedleStopAnimation.setFillAfter(true);
        needleStop();
    }

    private void init() {
        // 以下均可以替换成读取SharedPreferences或者查询数据库、解析json等形式……我懒
        mMusicId = 0;
        mMusicList.add(new MusicItem("Spectre", R.raw.spectre, R.drawable.spectre, true));
        mMusicList.add(new MusicItem("False King", R.raw.false_king, R.drawable.invincible, false));
        mMusicList.add(new MusicItem("Breath and Life", R.raw.bal, R.drawable.platinum, false));
        mMusicList.add(new MusicItem("Immortal", R.raw.immortal, R.drawable.illusions, false));
        initSong();
    }


    private void initSong() {
        setTitle(mMusicList.get(mMusicId).getName());
        if (mMusicList.get(mMusicId).isFavorite()) {
            mLoveImage.setImageResource(R.drawable.play_icn_loved);
        } else {
            mLoveImage.setImageResource(R.drawable.play_icn_love);
        }
        if (!mIsFirstPlay) {
            stopDiscAnimation();
        }
        int resId = mMusicList.get(mMusicId).getAlbumResId();
        mCoverImage.setImageResource(resId);
        mCoverImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mCoverImage.postInvalidate();
        mPlayDiscImage.setVisibility(View.VISIBLE);
    }

    private void startDiscAnimation() {
        mCoverAnimator = ObjectAnimator.ofFloat(mCoverImage, "rotation", 0f, 360f);
        mCoverAnimator.setDuration(30000);
        mCoverAnimator.setRepeatCount(Animation.INFINITE);
        mCoverAnimator.setRepeatMode(ValueAnimator.INFINITE);
        mCoverAnimator.setInterpolator(new LinearInterpolator());
        mCoverAnimator.start();
        mDiscAnimator = ObjectAnimator.ofFloat(mPlayDiscImage, "rotation", 0f, 360f);
        mDiscAnimator.setDuration(30000);
        mDiscAnimator.setRepeatCount(Animation.INFINITE);
        mDiscAnimator.setRepeatMode(ValueAnimator.INFINITE);
        mDiscAnimator.setInterpolator(new LinearInterpolator());
        mDiscAnimator.start();
    }

    private void pauseDiscAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mCoverAnimator.pause();
            mDiscAnimator.pause();
        }
    }

    private void resumeDiscAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mCoverAnimator.resume();
            mDiscAnimator.resume();
        }
    }

    private void stopDiscAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mCoverAnimator.setCurrentPlayTime(0);
            mDiscAnimator.setCurrentPlayTime(0);
        }
    }

    private void needleStop() {
        mNeedleImage.startAnimation(mNeedleStopAnimation);
    }

    private void needlePlay() {
        mNeedleImage.startAnimation(mNeedlePlayAnimation);
    }

    private void setSeekProgress(int progress) {
        if (!mIsSeeking) {
            mSeekBar.setProgress(progress);
        }
    }

    private void setReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.ACTION_BUTTON);
        mMusicBroadcastReceiver = new MusicBroadcastReceiver();
        registerReceiver(mMusicBroadcastReceiver, intentFilter);
    }

    private void setListeners() {
        if (mPreviousButton != null) {
            mPreviousButton.setOnClickListener(this);
        }

        if (mPlayButton != null) {
            mPlayButton.setOnClickListener(this);
        }

        if (mNextButton != null) {
            mNextButton.setOnClickListener(this);
        }
        if (mPlayList != null) {
            mPlayList.setOnClickListener(this);
        }

        if (mLoveImage != null) {
            mLoveImage.setOnClickListener(this);
        }

        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT && resultCode == RESULT_OK) {
            mMusicId = data.getIntExtra(MUSIC_SELECTED_ID, mMusicId);
            Message message = Message.obtain();
            buildSelectMessage(message);
            try {
                message.replyTo = mServiceMessenger;
                mMessenger.send(message);
                initSong();
                startMusic();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * 每个功能抽象成单独的方法，方便Notification和Widget
    */

    // 选择音乐或切换音乐后，直接开始播放
    private void startMusic() {
        initSong();
        if (!mIsStart) {
            resumeMusic();
        }
    }

    // 点击播放按钮播放音乐
    private void playMusic() {
        resumeMusic();
        Message message = Message.obtain();
        message.what = PLAY;
        sendMessageToService(message);
    }

    // 暂停音乐
    private void pauseMusic() {
        Message message = Message.obtain();
        message.what = PAUSE;
        mPlayButton.setImageResource(R.drawable.desk_play);
        needleStop();
        pauseDiscAnimation();
        Log.i(TAG, "Pause");
        mIsStart = false;
        sendMessageToService(message);
    }

    // 播放音乐
    private void resumeMusic() {
        mPlayButton.setImageResource(R.drawable.desk_pause);
        if (mIsFirstPlay) {
            startDiscAnimation();
            mIsFirstPlay = false;
        } else {
            resumeDiscAnimation();
        }
        needlePlay();
        mIsStart = true;
        Log.i(TAG, "Play");
    }

    // 上一曲
    private void previousMusic() {
        Message message = Message.obtain();
        if (mMusicId > 0) {
            mMusicId--;
        } else {
            mMusicId = mMusicList.size() - 1;
        }
        buildSelectMessage(message);
        sendMessageToService(message);
        initSong();
        startMusic();
    }

    // 下一曲
    private void nextMusic() {
        Message message = Message.obtain();
        if (mMusicId < mMusicList.size() - 1) {
            mMusicId++;
        } else {
            mMusicId = 0;
        }
        Log.i(TAG,"Music ID: " + mMusicId);
        buildSelectMessage(message);
        sendMessageToService(message);
        initSong();
        startMusic();
    }

    // 喜爱
    private void setLoveMusic() {
        boolean isLoved = !mMusicList.get(mMusicId).isFavorite();
        mMusicList.get(mMusicId).setFavorite(isLoved);
        if (isLoved) {
            mLoveImage.setImageResource(R.drawable.play_icn_loved);
        } else {
            mLoveImage.setImageResource(R.drawable.play_icn_love);
        }
    }

    // 通过 上一曲/下一曲/列表 选择音乐，创建一个需要向Service发送的消息
    private void buildSelectMessage(Message message) {
        message.what = SELECT;
        message.arg1 = mMusicList.get(mMusicId).getResId();
        message.arg2 = mMusicList.get(mMusicId).getAlbumResId();
        Bundle bundle = new Bundle();
        bundle.putString(ITEM_NAME, mMusicList.get(mMusicId).getName());
        message.setData(bundle);
    }

    // 把需要向Service发送的消息发出去
    private void sendMessageToService(Message message) {
        try {
            message.replyTo = mServiceMessenger;
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_button:
                if (!mIsStart) {
                    playMusic();
                } else {
                    pauseMusic();
                }
                break;
            case R.id.previous_button:
                previousMusic();
                break;
            case R.id.next_button:
                nextMusic();
                break;
            case R.id.play_list:
                // Notification 和 widget中不提供这个功能，因此不需抽象成单独方法
                Intent intent = new Intent(MainActivity.this, MusicListActivity.class);
                intent.putParcelableArrayListExtra(MUSIC_LIST, mMusicList);
                startActivityForResult(intent, SELECT);
                break;
            case R.id.love_it:
                setLoveMusic();
                break;
        }
    }

    // 本周新增：注册传感器监听器
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    // 本周新增：注销传感器监听器
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(mIntent);
        unregisterReceiver(mMusicBroadcastReceiver);
        unbindService(mServiceConnection);
    }

    // 本周新增：传感器事件——摇一摇切歌
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (x > 18 || y > 18 || z > 18) {
            nextMusic();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // SeekBar - OnSeekBarChangeListener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mProgress = progress; // 手滑到的进度
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Message message = Message.obtain();
        message.what = SEEKING;
        message.arg1 = mProgress;
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mIsSeeking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Message message = Message.obtain();
        message.what = SOUGHT;
        message.arg1 = mProgress;
        sendMessageToService(message);
        mIsSeeking = false;
    }

    public class MusicBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // receive broadcast, handle data
            if (intent != null) {
                switch (intent.getIntExtra(MusicService.BUTTON_ID, -1)) {
                    case 0:
                        if (!mIsStart) {
                            playMusic();
                        } else {
                            pauseMusic();
                        }
                        break;
                    case 1:
                        previousMusic();
                        break;
                    case 2:
                        nextMusic();
                        break;
                }
            }
        }
    }

    static class PlayingHandler extends Handler {
        private WeakReference<MainActivity> mMainActivityWeakReference;

        public PlayingHandler(MainActivity activity) {
            mMainActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity mainActivity = mMainActivityWeakReference.get();
            // 处理消息
            switch (msg.what) {
                case PROGRESS:
                    mainActivity.setSeekProgress(msg.arg1);
                    break;
                case NEXT:
                    mainActivity.nextMusic();
                    break;
            }
        }
    }
}