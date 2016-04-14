package com.example.shihwei.listviewdemo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.shihwei.listviewdemo.data.Destination;
import com.example.shihwei.listviewdemo.data.Record;
import com.example.shihwei.listviewdemo.data.Source;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ListViewDemo extends AppCompatActivity {
    private final boolean DEBUG = Utils.DEBUG;
    private final String DEBUG_TAG = getClass().getSimpleName();
    private final int LOADED_ITEM_COUNT = 100;

    // load more records if upper or lower invisible item count is less than the threshold
    private final int THRESHOLD_COUNT = 50;
    private RecyclerView mRecyclerView;
    private ListAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private int mFirstVisibleItemPos = 0;
    private int mLastVisibleItemPos = 0;
    private long mEndId = 0;
    private int mListSize = 0;
    private RequestTask mRequestTask;
    private boolean mLoadingFail = false;
    private boolean mLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mAdapter = new ListAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // scroll vertically
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mFirstVisibleItemPos = mLayoutManager.findFirstVisibleItemPosition();
                mLastVisibleItemPos = mLayoutManager.findLastVisibleItemPosition();

                // load mode data
                if (!mLoading && mLastVisibleItemPos > mListSize - THRESHOLD_COUNT) {
                    loadMoreData();
                }
                if (DEBUG) {
                    Log.i(DEBUG_TAG, "visible item from index " + mFirstVisibleItemPos
                            + " to " + mLastVisibleItemPos
                            + " total data index to " + mEndId);
                }
            }
        });
        loadMoreData();
    }

    private class RequestTask extends AsyncTask<URL, Integer, List<Record>> {

        @Override
        protected List<Record> doInBackground(URL... urls) {
            try {
                return Utils.retrieveData(urls[0]);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Retrieve record from endpoint fail.");
                mLoadingFail = true;
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Record> list) {
            mAdapter.setList(list);
            if (mLoadingFail) {
                mAdapter.mProgressView.mRetryMessage.setVisibility(View.VISIBLE);
                mAdapter.mProgressView.mProgress.setVisibility(View.INVISIBLE);
                mLoadingFail = false;
            }
            mLoading = false;
        }
    }

    // each record item view
    private class RecordViewHolder extends RecyclerView.ViewHolder {
        public TextView mTransactionView;
        public TextView mNoteView;
        public TextView mAmountView;
        public TextView mCreatedView;
        public TextView mIdView;

        public RecordViewHolder(View v) {
            super(v);
            mTransactionView = (TextView) v.findViewById(R.id.transaction);
            mNoteView = (TextView) v.findViewById(R.id.note);
            mAmountView = (TextView) v.findViewById(R.id.amount);
            mCreatedView = (TextView) v.findViewById(R.id.created);
            mIdView = (TextView) v.findViewById(R.id.id);
        }
    }

    private class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar mProgress;
        public TextView mRetryMessage;

        public ProgressViewHolder(View v) {
            super(v);
            mProgress = (ProgressBar) v.findViewById(R.id.progress_item);
            mRetryMessage = (TextView) v.findViewById(R.id.retry_message);
            mRetryMessage.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mAdapter.mProgressView.mRetryMessage.setVisibility(View.INVISIBLE);
                    mAdapter.mProgressView.mProgress.setVisibility(View.VISIBLE);
                    loadMoreData();
                }
            });
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int VIEW_TYPE_RECORD = 0;
        private final int VIEW_TYPE_LOADING = 1;
        private List<Record> mList = new ArrayList<>();
        private ProgressViewHolder mProgressView;

        public ListAdapter() {
            mList.add(null); // add a null item to show progress circle
        }

        public void setList(List<Record> list) {
            if (list == null) {
                return;
            }
            final int newSize = list.size();
            final int currentSize = mList.size();

            // remove progress item
            if (currentSize != 0) {
                if (mList.get(currentSize - 1) == null) {
                    mList.remove(currentSize - 1);
                    notifyItemRemoved(currentSize - 1);
                }
            }

            // add new data
            for (int i = 0; i < newSize; i++) {
                mList.add(list.get(i));
            }
            mListSize = mList.size();
            if (mListSize > 0) {
                mEndId = mList.get(mListSize - 1).getId();
            }
            
            mList.add(null); // add one more dummy item to show progress circle
            notifyItemRangeInserted(currentSize - 1, newSize + 1);

            if (DEBUG) {
                Log.i(DEBUG_TAG, "update data size to " + mListSize + " id to " + mEndId);
            }
        }

        // create item view as ViewHolder
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
            Context context = parent.getContext();
            if (viewType == VIEW_TYPE_LOADING) {
                View v = LayoutInflater.from(context).inflate(R.layout.list_item_progress, parent, false);
                mProgressView = new ProgressViewHolder(v);
                return mProgressView;
            } else {
                View v = LayoutInflater.from(context).inflate(R.layout.list_item_record, parent, false);
                return new RecordViewHolder(v);
            }
        }

        // display records to view
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            if (mList != null) {
                int index = position;
                if (index < mList.size()) {
                    Record record = mList.get(index);
                    if (record != null) {
                        Source source = record.getSource();
                        Destination dest = record.getDestination();
                        String sender = source == null ? source.ERROR_MSG : source.getSender();
                        String note = source == null ? source.ERROR_MSG : source.getNote();
                        String recipient = dest == null ? dest.ERROR_MSG : dest.getRecipient();
                        String currency = dest == null ? dest.ERROR_MSG : dest.getCurrency();
                        String amount = dest == null ? dest.ERROR_MSG : Long.toString(dest.getAmount());

                        RecordViewHolder holder = (RecordViewHolder) viewHolder;
                        holder.mTransactionView.setText(
                                String.format("from %s to %s", sender, recipient));
                        holder.mNoteView.setText(note);
                        holder.mAmountView.setText(
                                String.format("%s %s", currency, amount));
                        holder.mCreatedView.setText(record.getCreated());
                        holder.mIdView.setText(Long.toString(record.getId()));
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public int getItemViewType(int position) {
            int type = VIEW_TYPE_LOADING;
            if (mList != null) {
                int size = mList.size();
                if (size > position && mList.get(position) != null) {
                    type = VIEW_TYPE_RECORD;
                }
            }
            return type;
        }
    }

    private void loadMoreData() {
        mLoading = true;
        long index = mEndId;
        if (index > 0) {
            index = index + 1;
        }
        mRequestTask = new RequestTask();
        mRequestTask.execute(Utils.buildUrl(index, LOADED_ITEM_COUNT));

        if (DEBUG) {
            Log.i(DEBUG_TAG, "load data from " + index);
        }
    }
}
