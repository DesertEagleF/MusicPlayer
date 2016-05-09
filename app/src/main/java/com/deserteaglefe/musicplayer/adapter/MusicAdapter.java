package com.deserteaglefe.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.deserteaglefe.musicplayer.MusicItem;
import com.deserteaglefe.musicplayer.R;

import java.util.ArrayList;

/**
 * Adapter
 * Created by DesertEagleFe on 2016/4/27.
 */
public class MusicAdapter extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private ArrayList<MusicItem> mMusicList = new ArrayList<>();

    public MusicAdapter(Context context, ArrayList<MusicItem> musicList){
        mMusicList = musicList;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return mMusicList.size();
    }

    @Override
    public Object getItem(int position) {
        return mMusicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 返回一个视图

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.music_item, null);
            viewHolder = new ViewHolder();
            // 获取控件
            viewHolder.idTextView = (TextView) convertView.findViewById(R.id.music_id);
            viewHolder.favoriteImage = (ImageView) convertView.findViewById(R.id.love_it);
            viewHolder.nameTextView = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // 和数据之间进行绑定
        String str = position + ".";
        viewHolder.idTextView.setText(str);
        if(mMusicList.get(position).isFavorite()){
            viewHolder.favoriteImage.setImageResource(R.drawable.play_icn_loved);
        }else{
            viewHolder.favoriteImage.setImageResource(R.drawable.play_icn_love);
        }
        viewHolder.nameTextView.setText(mMusicList.get(position).getName());

        return convertView;
    }

    class ViewHolder {
        TextView idTextView;
        ImageView favoriteImage;
        TextView nameTextView;
    }


    /**
     * 刷新数据
     *
     * @param musicList : List of music
     */
    public void refreshData(ArrayList<MusicItem> musicList) {
        mMusicList = musicList;
    }
}
