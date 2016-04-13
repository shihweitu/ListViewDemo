package com.example.shihwei.listviewdemo.data;

/**
 * Data of an item
 */
public class Record {
    private long mId;
    private String mCreated;
    private Source mSource;
    private Destination mDestination;

    public Record(long args1, String args2, Source args3, Destination args4) {
        mId = args1;
        mCreated = args2;
        mSource = args3;
        mDestination = args4;
    }

    public long getId() {
        return mId;
    }

    public String getCreated() {
        return mCreated;
    }

    public Source getSource() {
        return mSource;
    }

    public Destination getDestination() {
        return mDestination;
    }
}
