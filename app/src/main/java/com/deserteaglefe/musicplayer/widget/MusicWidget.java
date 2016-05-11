package com.deserteaglefe.musicplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.deserteaglefe.musicplayer.R;
import com.deserteaglefe.musicplayer.service.MusicService;

/**
 * 作为一个播放器，没有widget的话岂不是很无聊？
 * Created by DesertEagleFe on 2016/5/1.
 */
public class MusicWidget extends AppWidgetProvider {
    private RemoteViews mRemoteViews;
    private static int album_id = R.drawable.spectre;
    private static Bitmap sBitmap = null;
    private static String name = "Spectre";
    private static String info = "Alan Walker - Spectre";
    private static int play_state = R.drawable.desk_play;

    private void setRemoteViews(Context context){
        // RemoteViews部分和Notification的一样，注意Notification在Service中可以直接用this，Widget需要用context
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.remote_view);
        if(sBitmap == null){
            mRemoteViews.setImageViewResource(R.id.remote_album, album_id);
        } else {
            mRemoteViews.setImageViewBitmap(R.id.remote_album, sBitmap);
        }
        mRemoteViews.setTextViewText(R.id.remote_name, name);
        mRemoteViews.setTextViewText(R.id.remote_info, info);
        mRemoteViews.setImageViewResource(R.id.remote_play, play_state);
        mRemoteViews.setImageViewResource(R.id.remote_prev, R.drawable.desk_pre);
        mRemoteViews.setImageViewResource(R.id.remote_next, R.drawable.desk_next);
        //点击的事件处理
        Intent buttonIntent = new Intent(MusicService.ACTION_BUTTON);
        /* 上一首按钮 */
        buttonIntent.putExtra(MusicService.BUTTON_ID, MusicService.REMOTE_PREV_ID);
        //这里加了广播，所及INTENT的必须用getBroadcast方法
        PendingIntent intent_prev = PendingIntent.getBroadcast(context, 1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.remote_prev, intent_prev);
        /* 播放/暂停  按钮 */
        buttonIntent.putExtra(MusicService.BUTTON_ID, MusicService.REMOTE_PLAY_ID);
        PendingIntent intent_play = PendingIntent.getBroadcast(context, 2, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.remote_play, intent_play);
        /* 下一首 按钮  */
        buttonIntent.putExtra(MusicService.BUTTON_ID, MusicService.REMOTE_NEXT_ID);
        PendingIntent intent_next = PendingIntent.getBroadcast(context, 3, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.remote_next, intent_next);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Notification在Service中可以直接更新，Widget需要广播
        if(intent != null && TextUtils.equals(intent.getAction(), MusicService.WIDGET_UPDATE_ACTION)){
            setRemoteViews(context);
            switch (intent.getIntExtra(MusicService.BUTTON_ID, -1)){
                case 0:
                    play_state = R.drawable.desk_pause;
                    mRemoteViews.setImageViewResource(R.id.remote_play, play_state);
                    break;
                case 1:
                    play_state = R.drawable.desk_play;
                    mRemoteViews.setImageViewResource(R.id.remote_play, play_state);
                    break;
                case 2:
                    byte[] art = intent.getByteArrayExtra(MusicService.ALBUM_EMBEDDED);
                    sBitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    name = intent.getStringExtra(MusicService.NAME);
                    info = intent.getStringExtra(MusicService.INFO);
                    if(sBitmap == null){
                        mRemoteViews.setImageViewResource(R.id.remote_album, album_id);
                    } else {
                        mRemoteViews.setImageViewBitmap(R.id.remote_album, sBitmap);
                    }
                    mRemoteViews.setTextViewText(R.id.remote_name, name);
                    mRemoteViews.setTextViewText(R.id.remote_info, info);
                    play_state = R.drawable.desk_pause;
                    mRemoteViews.setImageViewResource(R.id.remote_play, play_state);
                    break;

            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context,MusicWidget.class);
            appWidgetManager.updateAppWidget(componentName,mRemoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        setRemoteViews(context);
        // widget更新
        appWidgetManager.updateAppWidget(appWidgetIds,mRemoteViews);
    }
}
