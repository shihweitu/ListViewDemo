package com.example.shihwei.listviewdemo.data;

/**
 * Data of recipient
 */
public class Destination {
    private String mRecipient;
    private long mAmount;
    private String mCurrency;
    public final String ERROR_MSG = "Destination data not found";

    public Destination(String args1, long args2, String args3) {
        mRecipient = args1;
        mAmount = args2;
        mCurrency = args3;
    }

    public String getRecipient() {
        return mRecipient;
    }

    public long getAmount() {
        return mAmount;
    }

    public String getCurrency() {
        return mCurrency;
    }
}
