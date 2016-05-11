package com.deserteaglefe.musicplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.deserteaglefe.musicplayer.R;
import com.deserteaglefe.musicplayer.activity.MainActivity;
import com.deserteaglefe.musicplayer.item.MusicItem;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by DesertEagleFe on 2016/4/25.
 * Function: Music Service
 */
public class MusicService extends Service implements MediaPlayer.OnCompletionListener{

    // 常量
    public static final String ACTION_BUTTON = "action_button";
    public static final String BUTTON_ID = "button_id";
    public static final String ALBUM_EMBEDDED = "album_id";
    public static final String NAME = "name";
    public static final String INFO = "info";
    public static final String WIDGET_UPDATE_ACTION = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final int REMOTE_PLAY_ID = 0;
    public static final int REMOTE_PREV_ID = 1;
    public static final int REMOTE_NEXT_ID = 2;
    private int notifyId = 1;
    // 播放器
    private MediaPlayer mMediaPlayer;

    // 基本类型
    private int mDuration;
    private static final int MAX = 500;
    private static boolean mIsPlaying = false;
    private static boolean mIsComplete = false;
    private boolean mIsRes = false;

    // 消息相关
    private IncomingHandler mIncomingHandler = new IncomingHandler(this);
    private ServiceHandler mServiceHandler = new ServiceHandler(this);
    private Messenger mMessenger = new Messenger(mIncomingHandler);
    private static Messenger serviceMessenger;
    NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    Notification mNotify;

    // 进度线程
    private Runnable mRunnable = new Runnable() {
        private Message mMessage = Message.obtain();
        @Override
        public void run() {
            try {
                if(mIsPlaying){
                    if(!mIsComplete){
                        mMessage.what = MainActivity.PROGRESS;
                        mMessage.arg1 = mMediaPlayer.getCurrentPosition() * MAX / mDuration; // 直接换算成进度，MainActivity不需知道音乐长度
                    }else{
                        mMessage.what = MainActivity.NEXT;
                        mIsComplete = false;
                    }
                    serviceMessenger.send(mMessage);
                    mServiceHandler.sendEmptyMessageDelayed(MainActivity.CONTINUE, 100); // 貌似用sleep()来定时的话会使得Messenger无法响应其他事件
                }
            } catch (RemoteException e) {
                Log.i(MainActivity.TAG,"RemoteException");
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        initMediaPlayer();
        showButtonNotify();
    }

    private void initMediaPlayer(){
        mMediaPlayer = MediaPlayer.create(this, R.raw.spectre);
        mMediaPlayer.setOnCompletionListener(this);
        mDuration = mMediaPlayer.getDuration();
        mIsRes = true;
    }
    /**
     * notification
     * 本方法参考下面文章：
     * @link http://blog.csdn.net/vipzjyno1/article/details/25248021
     */
    public void showButtonNotify(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // RemoteViews部分和Widget的一样，注意Notification在Service中可以直接用this，Widget需要用context
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.remote_view);
        mRemoteViews.setImageViewResource(R.id.remote_album, R.drawable.spectre);
        mRemoteViews.setTextViewText(R.id.remote_name, "Spectre");
        mRemoteViews.setTextViewText(R.id.remote_info, "Alan Walker - Spectre");
        if(mIsPlaying){
            mRemoteViews.setImageViewResource(R.id.remote_play, R.drawable.desk_pause);
        }else{
            mRemoteViews.setImageViewResource(R.id.remote_play, R.drawable.desk_play);
        }
        mRemoteViews.setImageViewResource(R.id.remote_prev, R.drawable.desk_pre);
        mRemoteViews.setImageViewResource(R.id.remote_next, R.drawable.desk_next);
        //点击的事件处理
        Intent buttonIntent = new Intent(ACTION_BUTTON);
        /* 上一首按钮 */
        buttonIntent.putExtra(BUTTON_ID, REMOTE_PREV_ID);
        //这里加了广播，所及INTENT的必须用getBroadcast方法
        PendingIntent intent_prev = PendingIntent.getBroadcast(this, 1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.remote_prev, intent_prev);
        /* 播放/暂停  按钮 */
        buttonIntent.putExtra(BUTTON_ID, REMOTE_PLAY_ID);
        PendingIntent intent_play = PendingIntent.getBroadcast(this, 2, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.remote_play, intent_play);
        /* 下一首 按钮  */
        buttonIntent.putExtra(BUTTON_ID, REMOTE_NEXT_ID);
        PendingIntent intent_next = PendingIntent.getBroadcast(this, 3, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.remote_next, intent_next);

        // Build Notification
        builder.setContent(mRemoteViews)
                .setContentIntent(getDefaultIntent(Notification.FLAG_ONGOING_EVENT))
                .setWhen(System.currentTimeMillis())// 通知产生的时间
                .setTicker("正在播放")
                .setPriority(Notification.PRIORITY_DEFAULT)// 设置该通知优先级
                .setOngoing(true)
                .setSmallIcon(R.drawable.nact_icn_music);
        mNotify = builder.build();
        mNotify.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(notifyId, mNotify);
    }

    public PendingIntent getDefaultIntent(int flags){
        return PendingIntent.getActivity(this, 1, new Intent(), flags);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mIsComplete = true;
        Log.i(MainActivity.TAG, "onCompletion");
    }

    public void start() {
        mIsPlaying = true;
        mMediaPlayer.start();
        mRemoteViews.setImageViewResource(R.id.remote_play, R.drawable.desk_pause);
        Intent intent = new Intent();
        intent.setAction(WIDGET_UPDATE_ACTION);
        intent.putExtra(BUTTON_ID, 0);
        sendBroadcast(intent);
        mNotificationManager.notify(notifyId, mNotify);
        Log.i(MainActivity.TAG,"MediaPlayer Start");
        mRunnable.run();
    }

    public void pause() {
        mIsPlaying = false;
        mMediaPlayer.pause();
        mRemoteViews.setImageViewResource(R.id.remote_play, R.drawable.desk_play);
        Intent intent = new Intent();
        intent.setAction(WIDGET_UPDATE_ACTION);
        intent.putExtra(BUTTON_ID, 1);
        sendBroadcast(intent);
        mNotificationManager.notify(notifyId, mNotify);
        Log.i(MainActivity.TAG,"MediaPlayer Pause");
    }

    public void prepare(MusicItem musicItem){
        if(mIsRes){
            mMediaPlayer.release();
        }else{
            mMediaPlayer.reset(); // 似乎会清掉与OnCompletionListener之间的绑定
        }

        mIsRes = musicItem.isRes();
        try {
            if(mIsRes){
                musicItem.setContext(this);
                mMediaPlayer = MediaPlayer.create(this, musicItem.getResId());
            } else {
                String path = musicItem.getPath();
                AssetFileDescriptor afd = getAssets().openFd(path);
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mMediaPlayer.prepare();
            }
            mDuration = mMediaPlayer.getDuration();
            mIsPlaying = true;
            mMediaPlayer.setOnCompletionListener(this); // 那就重新设置
            mMediaPlayer.start();
            mRunnable.run();
            byte[] art = musicItem.getArt();
            Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            mRemoteViews.setImageViewBitmap(R.id.remote_album, bitmap);
            mRemoteViews.setTextViewText(R.id.remote_name, musicItem.getTitle());
            mRemoteViews.setTextViewText(R.id.remote_info, musicItem.getArtist() + " - " + musicItem.getAlbum());
            mRemoteViews.setImageViewResource(R.id.remote_play, R.drawable.desk_pause);
            Intent intent = new Intent();
            intent.setAction(WIDGET_UPDATE_ACTION);
            intent.putExtra(BUTTON_ID, 2);
            intent.putExtra(ALBUM_EMBEDDED, art);
            intent.putExtra(NAME, musicItem.getTitle());
            intent.putExtra(INFO, musicItem.getArtist() + " - " + musicItem.getAlbum());
            sendBroadcast(intent);
            mNotificationManager.notify(notifyId, mNotify);
        } catch (IOException e) {
            Log.i(MainActivity.TAG,"File Open Error");
            e.printStackTrace();
        }
    }

    public void setProgress(int progress) {
        mMediaPlayer.seekTo(progress * mDuration / MAX);
        mIsPlaying = true; // 重置进度条更新线程的判断条件
        mRunnable.run();
    }
    public void setSeeking(){
        mIsPlaying = false; // 暂停进度条更新的线程
    }

    public void continueRun(){
        mRunnable.run();
    }

    @Override
    public void onDestroy() {
        mMediaPlayer.stop();
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    static class IncomingHandler extends Handler {
        private WeakReference<MusicService> mMusicServiceWeakReference;

        public IncomingHandler(MusicService musicService) {
            mMusicServiceWeakReference = new WeakReference<>(musicService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            serviceMessenger = msg.replyTo;
            MusicService musicService = mMusicServiceWeakReference.get();
            // 处理消息
            switch (msg.what) {
                case MainActivity.PLAY:
                    musicService.start();
                    break;
                case MainActivity.PAUSE:
                    musicService.pause();
                    break;
                case MainActivity.SELECT:
                    msg.getData().setClassLoader(getClass().getClassLoader());
                    musicService.prepare((MusicItem) msg.getData().getParcelable(MainActivity.ITEM_NAME));
                    break;
                case MainActivity.SEEKING:
                    musicService.setSeeking();
                    break;
                case MainActivity.SOUGHT:
                    musicService.setProgress(msg.arg1);
                    break;
            }
        }
    }
    static class ServiceHandler extends Handler {
        private WeakReference<MusicService> mMusicServiceWeakReference;

        public ServiceHandler(MusicService musicService) {
            mMusicServiceWeakReference = new WeakReference<>(musicService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MusicService musicService = mMusicServiceWeakReference.get();
            // 处理消息
            switch (msg.what) {
                case MainActivity.CONTINUE:
                    musicService.continueRun();
                    break;
            }
        }
    }
}
