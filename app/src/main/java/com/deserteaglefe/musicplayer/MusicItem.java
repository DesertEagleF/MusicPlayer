package com.deserteaglefe.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Item for ListView
 * Created by DesertEagleFe on 2016/4/27.
 */
public class MusicItem implements Parcelable {
    private String mName;
    private int mResId;
    private int mAlbumResId;
    private boolean mIsFavorite;

    public MusicItem(String name, int resId, int albumResId, boolean isFavorite) {
        mName = name;
        mResId = resId;
        mAlbumResId = albumResId;
        mIsFavorite = isFavorite;
    }

    protected MusicItem(Parcel in) {
        mName = in.readString();
        mResId = in.readInt();
        mAlbumResId = in.readInt();
        mIsFavorite = in.readByte() != 0;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeInt(mResId);
        dest.writeInt(mAlbumResId);
        dest.writeByte((byte) (mIsFavorite ? 1 : 0));
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public int getResId() {
        return mResId;
    }

    public void setResId(int resId) {
        mResId = resId;
    }

    public int getAlbumResId() {
        return mAlbumResId;
    }

    public void setAlbumResId(int albumResId) {
        mAlbumResId = albumResId;
    }

    public boolean isFavorite() {
        return mIsFavorite;
    }

    public void setFavorite(boolean favorite) {
        mIsFavorite = favorite;
    }

}
