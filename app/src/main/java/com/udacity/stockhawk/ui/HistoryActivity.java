package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by t-xu on 5/6/17.
 */

public class HistoryActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TARGET_SYMBOL = "target_symbol";
    private static final int HISTORY_LOADER = 1;

    private CompositeDisposable mDisposables = new CompositeDisposable();

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.chart) LineChart mChart;

    private String mTargetSymbol;

    public static void launch(Context from, String symbol) {
        Intent i = new Intent();
        i.setClass(from, HistoryActivity.class);
        i.putExtra(TARGET_SYMBOL, symbol);
        from.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);

        mTargetSymbol = getIntent().getStringExtra(TARGET_SYMBOL);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTargetSymbol);
        }

        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.setDrawGridBackground(false);
        mChart.setMaxVisibleValueCount(40);
        mChart.getDescription().setEnabled(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleYEnabled(false);
        mChart.setScaleXEnabled(true);
        mChart.setPinchZoom(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.getLegend().setEnabled(false);
        mChart.setNoDataTextColor(Color.WHITE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getSupportLoaderManager().getLoader(HISTORY_LOADER) == null) {
            getSupportLoaderManager().initLoader(HISTORY_LOADER, null, this);
        } else {
            getSupportLoaderManager().restartLoader(HISTORY_LOADER, null, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            mDisposables.clear();
        }
    }

    private void showHistory(String history) {
        Disposable d =
                Observable.defer(() -> {
                    ArrayList<Entry> values = new ArrayList<>();
                    String[] historyRecords = history.split("\n");

                    int index = historyRecords.length;
                    for (String record : historyRecords) {
                        String[] entry = record.split(",");
                        float val = Float.parseFloat(entry[1]);
                        DateTime dateTime = new DateTime()
                                .withMillis(Long.parseLong(entry[0]));
                        values.add(new Entry(--index, val, null, dateTime.toString("MM/dd/yyyy")));
                    }
                    Collections.reverse(values);
                    return Observable.just(values);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onHistoryData, Timber::e);
        mDisposables.add(d);
    }

    private void onHistoryData(ArrayList<Entry> values) {

        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        for (Entry entry : values) {
            max = Math.max(max, entry.getY());
            min = Math.min(min, entry.getY());
        }
        float range = max - min;
        float rangeMax = max + range * 0.05f;
        float rangeMin = min - range * 0.05f;

        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setDrawGridLines(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new TimeBaseXAxisValueFormatter(values));
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.setAxisMaximum(rangeMax);
        yAxis.setAxisMinimum(rangeMin);
        //leftAxis.setYOffset(20f);
        yAxis.enableGridDashedLine(10f, 10f, 0f);
        yAxis.setDrawGridLines(true);
        yAxis.setDrawZeroLine(false);
        yAxis.setTextColor(Color.WHITE);
        // limit lines are drawn behind data (and not on top)
        yAxis.setDrawLimitLinesBehindData(true);
        // add data
        LineDataSet set1;

        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet)mChart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "DataSet 1");
            set1.setDrawIcons(false);
            set1.setColor(getResources().getColor(R.color.colorPrimaryDark));
            set1.setDrawCircles(false);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(true);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_primary);
            set1.setFillDrawable(drawable);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            // create a data object with the datasets
            LineData lineData = new LineData(dataSets);
            // set data
            mChart.setData(lineData);
        }

        mChart.animateX(150);
        // mChart.invalidate();
    }

    private static class TimeBaseXAxisValueFormatter implements IAxisValueFormatter {

        private ArrayList<Entry> mValues;

        TimeBaseXAxisValueFormatter(ArrayList<Entry> values) {
            mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return (String) mValues.get((int) value).getData();
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.d("onCreateLoader");
        return new CursorLoader(this,
                Contract.Quote.URI,
                new String[]{Contract.Quote.COLUMN_HISTORY},
                Contract.Quote.COLUMN_SYMBOL  + " = ?",
                new String[]{mTargetSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Timber.d("onLoadFinished : " + loader.getId());
        if (data.getCount() == 0) {
            //TODO: No data
            return;
        }
        data.moveToFirst();
        showHistory(data.getString(0));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Timber.d("onLoaderReset");
    }
}
