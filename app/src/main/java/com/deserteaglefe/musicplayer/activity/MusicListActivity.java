package com.deserteaglefe.musicplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.deserteaglefe.musicplayer.MusicItem;
import com.deserteaglefe.musicplayer.R;
import com.deserteaglefe.musicplayer.adapter.MusicAdapter;

import java.util.ArrayList;

/**
 * the Play List For Selection
 * Created by DesertEagleFe on 2016/4/27.
 */
public class MusicListActivity extends AppCompatActivity {
    private ArrayList<MusicItem> mMusicList = new ArrayList<>(); // 播放列表
    private int mMusicId; // 只需要传回第几个就行了
    private ListView mListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);
        setTitle("播放列表");
        initView();
        getOriginal();
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.music_list_view);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mMusicId = position;
                returnData();
            }
        });
    }

    private void getOriginal() {
        Intent intent = getIntent();
        mMusicList = intent.getParcelableArrayListExtra(MainActivity.MUSIC_LIST);
        setAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        returnData();
    }

    public void setAdapter() {
        MusicAdapter musicAdapter = new MusicAdapter(MusicListActivity.this, mMusicList);
        mListView.setAdapter(musicAdapter);
        musicAdapter.notifyDataSetChanged();
    }

    private void returnData(){
        Intent intent = new Intent();
        intent.putExtra(MainActivity.MUSIC_SELECTED_ID, mMusicId);
        setResult(RESULT_OK, intent);
        finish();
    }
}
