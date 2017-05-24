package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import timber.log.Timber;

import static com.udacity.stockhawk.util.FormatUtils.sDollarFormat;
import static com.udacity.stockhawk.util.FormatUtils.sDollarFormatWithPlus;
import static com.udacity.stockhawk.util.FormatUtils.sPercentageFormat;

/**
 * Created by t-xu on 5/23/17.
 */

public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockListViewFactory(getApplicationContext(), intent);
    }

    public static class StockListViewFactory implements RemoteViewsFactory {

        private static final int MAX_STOCK_COUNT = 10;

        private static final Object sWidgetLock = new Object();

        private final Context mContext;
        private final int mAppWidgetId;
        private Cursor mCursor;
        private ContentResolver mResolver;

        public StockListViewFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            mResolver = context.getContentResolver();

        }

        private Cursor query() {
            return mResolver.query(Contract.Quote.URI,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                    null, null, Contract.Quote.COLUMN_SYMBOL);
        }

        @Override
        public void onCreate() {
            mCursor = query();
        }

        @Override
        public void onDataSetChanged() {
            synchronized (sWidgetLock) {
                query();
            }
        }

        @Override
        public void onDestroy() {
            synchronized (sWidgetLock) {
                if (mCursor != null && !mCursor.isClosed()) {
                    mCursor.close();
                    mCursor = null;
                }
            }
        }

        @Override
        public int getCount() {
            synchronized (sWidgetLock) {
                if (mCursor == null) {
                    return 0;
                }
                return mCursor.getCount();
            }
        }

        @Override
        public RemoteViews getViewAt(int position) {

            synchronized (sWidgetLock) {
                final RemoteViews remoteView = new RemoteViews(
                        mContext.getPackageName(), R.layout.list_item_quote);

                mCursor.moveToFirst();
                mCursor.move(position);

                float rawAbsoluteChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                String changeStr = sDollarFormatWithPlus.format(rawAbsoluteChange);
                String percentage = sPercentageFormat.format(percentageChange / 100);
                remoteView.setTextViewText(R.id.symbol, mCursor.getString(Contract.Quote.POSITION_SYMBOL));
                remoteView.setTextViewText(R.id.price,
                        sDollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE)));
                if (PrefUtils.getDisplayMode(mContext)
                        .equals(mContext.getString(R.string.pref_display_mode_absolute_key))) {
                    remoteView.setTextViewText(R.id.change, changeStr);
                } else {
                    remoteView.setTextViewText(R.id.change, percentage);
                }
                if (rawAbsoluteChange > 0) {
                    remoteView.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    remoteView.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }

                int symbolColumn = mCursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL);
                final String symbolStr = mCursor.getString(symbolColumn);
                //view.setOnClickListener(v -> HistoryActivity.launch(mContext, symbolStr));
                return remoteView;
            }
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
