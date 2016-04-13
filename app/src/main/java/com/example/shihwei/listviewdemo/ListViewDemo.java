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
import android.widget.Button;
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
    private final int LOADED_ITEM_COUNT = 15;

    // load more records if upper or lower invisible item count is less than the threshold
    private final int THRESHOLD_COUNT = 10;
    private RecyclerView mRecyclerView;
    private ListAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private int mFirstVisibleItemPos = 0;
    private int mLastVisibleItemPos = 0;
    private int mStartIndex = 0; // start index of current retrieved data
    private int mEndIndex = LOADED_ITEM_COUNT - 1;
    private RequestTask mRequestTask;
    private boolean loadingFail = false;

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

                int lowerBound = mEndIndex - THRESHOLD_COUNT;

                if (DEBUG) {
                    Log.i(DEBUG_TAG, "visible item position from index " + mFirstVisibleItemPos
                            + " to " + mLastVisibleItemPos + " end index=" + mEndIndex
                            + " lowerBound=" + lowerBound);
                }

                // load mode data
                if (mLastVisibleItemPos == lowerBound) {
                    mStartIndex = mStartIndex + LOADED_ITEM_COUNT;
                    mEndIndex = mStartIndex + LOADED_ITEM_COUNT - 1;
                    loadMoreData();
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
                loadingFail = true;
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Record> list) {
            mAdapter.setList(list);
            if (loadingFail) {
                mAdapter.mProgressView.mRetryMessage.setVisibility(View.VISIBLE);
                mAdapter.mProgressView.mProgress.setVisibility(View.INVISIBLE);
                mEndIndex = mEndIndex - LOADED_ITEM_COUNT;
                mStartIndex = mEndIndex - LOADED_ITEM_COUNT + 1;
                loadingFail = false;
            }
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
                    mStartIndex = mStartIndex + LOADED_ITEM_COUNT;
                    mEndIndex = mStartIndex + LOADED_ITEM_COUNT - 1;
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
            mList.add(null); // add a null record to show progress circle
        }

        public void setList(List<Record> list) {
            if (list == null) {
                return;
            }
            final int size = list.size();
            final int total = mList.size();

            // remove progress item
            if (total != 0) {
                if (mList.get(total - 1) == null) {
                    mList.remove(total - 1);
                    notifyItemRemoved(total - 1);
                }
            }

            // add new data
            for (int i = 0; i < size; i++) {
                mList.add(list.get(i));
            }
            mList.add(null); // add one more dummy item to show progress circle
            notifyDataSetChanged();
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
        if (DEBUG) {
            Log.i(DEBUG_TAG, "load data from " + mStartIndex
                    + " to " + (mStartIndex + LOADED_ITEM_COUNT));
        }
        mRequestTask = new RequestTask();
        mRequestTask.execute(Utils.buildUrl(mStartIndex, LOADED_ITEM_COUNT));
    }
}
