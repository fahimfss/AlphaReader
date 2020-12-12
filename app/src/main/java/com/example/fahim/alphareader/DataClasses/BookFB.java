package com.example.fahim.alphareader.DataClasses;

import android.os.Parcel;
import android.os.Parcelable;

public class BookFB implements Parcelable {
    public static final Parcelable.Creator<BookFB> CREATOR = new Parcelable.Creator<BookFB>() {
        public BookFB createFromParcel(Parcel param1Parcel) {
            return new BookFB(param1Parcel);
        }

        public BookFB[] newArray(int param1Int) {
            return new BookFB[param1Int];
        }
    };

    private String bookKey;
    private String bookName;
    private String filePath;
    private int numChapters;
    private long timeAdded;
    private long lastAccessed;

    public BookFB(){}

    public BookFB(Parcel parcel) {
        this.bookKey = parcel.readString();
        this.timeAdded = parcel.readLong();
        this.numChapters = parcel.readInt();
        this.bookName = parcel.readString();
        this.lastAccessed = parcel.readLong();
        this.filePath = parcel.readString();
    }

    public BookFB(String bookKey, long timeAdded, int numChapters, String bookName, String filePath) {
        this.bookKey = bookKey;
        this.timeAdded = timeAdded;
        this.numChapters = numChapters;
        this.bookName = bookName;
        this.filePath = filePath;
        lastAccessed = -1;
    }

    public int describeContents() {
        return 0;
    }

    public String getBookKey() {
        return this.bookKey;
    }

    public String getBookName() {
        return this.bookName;
    }

    public int getNumChapters() {
        return this.numChapters;
    }

    public long getTimeAdded() {
        return this.timeAdded;
    }


    public void setBookKey(String bookKey) {
        this.bookKey = bookKey;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public void setNumChapters(int numChapters) {
        this.numChapters = numChapters;
    }

    public void setTimeAdded(long timeAdded) {
        this.timeAdded = timeAdded;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void writeToParcel(Parcel parcel, int paramInt) {
        parcel.writeString(this.bookKey);
        parcel.writeLong(this.timeAdded);
        parcel.writeInt(this.numChapters);
        parcel.writeString(this.bookName);
        parcel.writeLong(this.lastAccessed);
        parcel.writeString(this.filePath);
    }
}
