package com.deserteaglefe.musicplayer.item;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Item for ListView
 * Created by DesertEagleFe on 2016/4/27.
 * <p/>
 * Update: 2016/5/11
 * Read the infomations from the music itself
 */
public class MusicItem implements Parcelable {
    private static final String TAG = "MusicInfo";
    private static final String CONNECTOR = " - ";
    private int mResId;            // 对于res
    private Context mContext;
    private boolean mIsRes = false;
    private String mPath;         // 路径
    private boolean mIsFavorite;  // 是否喜爱
    private String mArtist;       // 艺术家
    private String mAlbum;        // 专辑
    private String mTitle;        // 名称
    private MediaMetadataRetriever mMmr;

    // Parcelable 需要默认构造函数
    public MusicItem(){
    }

    // 对于res文件
    public MusicItem(int resId, Context context) {
        mResId = resId;
        mContext = context;
        Log.d(TAG, "res");
        getMetadata(mResId);
        mIsRes = true;
    }

    // 对于提供路径的文件
    public MusicItem(String path) {
        mPath = path;
        getMetadata(mPath);
        mIsRes = false;
    }

    protected MusicItem(Parcel in) {
        mResId = in.readInt();
        mIsRes = in.readByte() != 0;
        mPath = in.readString();
        mIsFavorite = in.readByte() != 0;
        mArtist = in.readString();
        mAlbum = in.readString();
        mTitle = in.readString();
    }

    public static final Creator<MusicItem> CREATOR = new Creator<MusicItem>() {
        @Override
        public MusicItem createFromParcel(Parcel in) {
            return new MusicItem(in);
        }

        @Override
        public MusicItem[] newArray(int size) {
            return new MusicItem[size];
        }
    };

    // 获取Metadata
    private void getMetadata(String path) {
        mMmr = new MediaMetadataRetriever();
        Log.d(TAG, "str:" + path);
        mMmr.setDataSource(path);
        getMetadata();
    }

    private void getMetadata(int resId) {
        mMmr = new MediaMetadataRetriever();
        Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + resId);
        Log.d(TAG, "uri:" + uri);
        mMmr.setDataSource(mContext, uri);
        getMetadata();
    }

    private void getMetadata() {
        try {
            mArtist = mMmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            mAlbum = mMmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            mTitle = mMmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            Log.d(TAG, "artist:" + mArtist);
            Log.d(TAG, "album:" + mAlbum);
            Log.d(TAG, "title:" + mTitle);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "IllegalArgumentException");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException");
            e.printStackTrace();
        }
    }

    public int getResId() {
        return mResId;
    }

    public byte[] getArt() {
        if(mIsRes){
            getMetadata(mResId);
        }else{
            getMetadata(mPath);
        }
        return mMmr.getEmbeddedPicture();
    }

    public String getName() {
        return mTitle + CONNECTOR + mArtist;
    }

    public String getPath() {
        return mPath;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getArtist() {
        return mArtist;
    }

    public boolean isFavorite() {
        return mIsFavorite;
    }

    public boolean isRes() {
        return mIsRes;
    }

    public void setRes(boolean res) {
        mIsRes = res;
    }

    public void setContext(Context context){
        mContext = context;
    }

    public void setFavorite(boolean favorite) {
        mIsFavorite = favorite;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mResId);
        dest.writeByte((byte) (mIsRes ? 1 : 0));
        dest.writeString(mPath);
        dest.writeByte((byte) (mIsFavorite ? 1 : 0));
        dest.writeString(mArtist);
        dest.writeString(mAlbum);
        dest.writeString(mTitle);
    }
}
