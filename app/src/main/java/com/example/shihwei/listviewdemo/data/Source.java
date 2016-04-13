package com.example.shihwei.listviewdemo.data;

/**
 * Data of source
 */
public class Source {
    private String mSender;
    private String mNote;
    public final String ERROR_MSG = "Source data not found";

    public Source(String args1, String args2) {
        mSender = args1;
        mNote = args2;
    }

    public String getSender() {
        return mSender;
    }

    public String getNote() {
        return mNote;
    }
}
