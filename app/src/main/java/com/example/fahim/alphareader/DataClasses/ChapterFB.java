package com.example.fahim.alphareader.DataClasses;

import android.os.Parcel;
import android.os.Parcelable;

public class ChapterFB implements Parcelable {
    public static final Parcelable.Creator<ChapterFB> CREATOR = new Parcelable.Creator<ChapterFB>() {
        public ChapterFB createFromParcel(Parcel param1Parcel) {
            return new ChapterFB(param1Parcel);
        }

        public ChapterFB[] newArray(int param1Int) {
            return new ChapterFB[param1Int];
        }
    };

    private String chapterKey;

    private String chapterName;

    private double completed;

    private long timeAdded;

    private String filePath;

    public ChapterFB() {}

    protected ChapterFB(Parcel parcel) {
        this.chapterKey = parcel.readString();
        this.timeAdded = parcel.readLong();
        this.chapterName = parcel.readString();
        this.completed = parcel.readDouble();
        this.filePath = parcel.readString();
    }

    public ChapterFB(String chapterKey, long timeAdded, String chapterName, double completed, String filePath) {
        this.chapterKey = chapterKey;
        this.timeAdded = timeAdded;
        this.chapterName = chapterName;
        this.completed = completed;
        this.filePath = filePath;
    }

    public int describeContents() {
        return 0;
    }

    public String getChapterKey() {
        return this.chapterKey;
    }

    public String getChapterName() {
        return this.chapterName;
    }

    public double getCompleted() {
        return this.completed;
    }

    public long getTimeAdded() {
        return this.timeAdded;
    }

    public void setChapterKey(String paramString) {
        this.chapterKey = paramString;
    }

    public void setChapterName(String paramString) {
        this.chapterName = paramString;
    }

    public void setCompleted(double paramDouble) {
        this.completed = paramDouble;
    }

    public void setTimeAdded(long paramLong) {
        this.timeAdded = paramLong;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void writeToParcel(Parcel parcel, int paramInt) {
        parcel.writeString(this.chapterKey);
        parcel.writeLong(this.timeAdded);
        parcel.writeString(this.chapterName);
        parcel.writeDouble(this.completed);
        parcel.writeString(this.filePath);
    }
}
