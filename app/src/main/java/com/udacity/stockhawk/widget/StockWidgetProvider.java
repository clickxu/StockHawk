package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteIntentService;
import com.udacity.stockhawk.ui.HistoryActivity;

/**
 * Created by t-xu on 5/22/17.
 */

public class StockWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateStockList(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateStockList(Context context,
                                        AppWidgetManager appWidgetManager, int appWidgetId) {
        //which layout to show on widget
        RemoteViews remoteViews = new RemoteViews(
                context.getPackageName(),R.layout.widget_stocks);

        //RemoteViews Service needed to provide adapter for ListView
        Intent svcIntent = new Intent(context, WidgetService.class);
        //passing app widget id to that RemoteViews Service
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        //setting adapter to listview of the widget
        remoteViews.setRemoteAdapter(R.id.stock_list, svcIntent);

        Intent appIntent = new Intent(context, HistoryActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.stock_list, appPendingIntent);

        //setting an empty view in case of no data
        remoteViews.setEmptyView(R.id.stock_list, R.id.no_data);
        // Add the wateringservice click handler
        Intent quoteIntent = new Intent(context, QuoteIntentService.class);
        PendingIntent wateringPendingIntent = PendingIntent.getService(context, 0, quoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.no_data, wateringPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

}
