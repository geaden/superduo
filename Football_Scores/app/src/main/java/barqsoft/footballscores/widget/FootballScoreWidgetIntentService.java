package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.request.target.Target;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.scoresAdapter;
import barqsoft.footballscores.svg.SvgDecoder;
import barqsoft.footballscores.svg.SvgDrawableTranscoder;

/**
 * IntentService which handles updating all Football Score Widgets with the latest data.
 *
 * @author Gennady Denisov
 */
public class FootballScoreWidgetIntentService extends IntentService {

    private static final String LOG_TAG = FootballScoreWidgetIntentService.class.getSimpleName();

    public FootballScoreWidgetIntentService() {
        super("FootballScoreWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide.with(this)
                .using(Glide.buildStreamModelLoader(Uri.class, this), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.no_icon)
                .error(R.drawable.no_icon);

        // Retrieve all of the Football Score widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(
                this, FootballScoreWidgetProvider.class));

        // Construct current date to get scores for
        DateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateOfScores = mFormat.format(new Date(System.currentTimeMillis()));

        Cursor data = getContentResolver().query(DatabaseContract.scores_table.buildScoreWithDate(),
                null, null, new String[]{dateOfScores}, null);

        if (data == null) {
            return;
        }

        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // Extract score data from the Cursor
        String homeName = data.getString(scoresAdapter.COL_HOME);
        String awayName = data.getString(scoresAdapter.COL_AWAY);
        String date = data.getString(scoresAdapter.COL_MATCHTIME);
        String score = Utilies.getScores(data.getInt(scoresAdapter.COL_HOME_GOALS),
                data.getInt(scoresAdapter.COL_AWAY_GOALS));
        double matchId = data.getDouble(scoresAdapter.COL_ID);
        int homeCrest = Utilies.getTeamCrestByTeamName(
                data.getString(scoresAdapter.COL_HOME));
        int awayCrest = Utilies.getTeamCrestByTeamName(
                data.getString(scoresAdapter.COL_AWAY));
        // Get home crest url
        String homeCrestUrl = data.getString(scoresAdapter.COL_HOME_CREST_URL);
        // Get away crest url
        String awayCrestUrl = data.getString(scoresAdapter.COL_AWAY_CREST_URL);

        data.close();

        // Perform this loop procedure for each Football Score widget
        for (int appWidgetId : appWidgetIds) {
            // Find the correct layout based on the widget's width
            int widgetWidth = getWidgetWidth(appWidgetManager, appWidgetId);
            int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_football_today_score_default_width);
            int largeWidth = getResources().getDimensionPixelSize(R.dimen.widget_football_today_score_large_width);
            int layoutId;
            if (widgetWidth >= largeWidth) {
                layoutId = R.layout.widget_football_today_score_large;
            } else if (widgetWidth >= defaultWidth) {
                layoutId = R.layout.widget_football_today_score;
            } else {
                layoutId = R.layout.widget_football_today_score_small;
            }
            final RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            String description = getString(R.string.score_description, homeName, awayName, score);

            views.setTextViewText(R.id.home_name, homeName);
            views.setTextViewText(R.id.away_name, awayName);
            views.setTextViewText(R.id.data_textview, date);
            views.setTextViewText(R.id.score_textview, score);

            // Get home crest
            if (homeCrestUrl == null) {
                views.setImageViewResource(R.id.home_crest, homeCrest);
            } else {
                Log.d(LOG_TAG, "Placing home crest in widget " + homeCrestUrl);
                Uri uri = Uri.parse(homeCrestUrl);
                try {
                    PictureDrawable pictureDrawable = requestBuilder
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            // SVG cannot be serialized so it's not worth to cache it
                            .load(uri)
                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                    views.setImageViewBitmap(R.id.home_crest,
                            Utilies.getBitmapFromView(pictureDrawable));
                } catch (InterruptedException | ExecutionException e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
            }

            // Away crest
            if (awayCrestUrl == null) {
                views.setImageViewResource(R.id.away_crest, awayCrest);
            } else {
                Log.d(LOG_TAG, "Placing away crest in widget " + awayCrestUrl);
                Uri uri = Uri.parse(awayCrestUrl);
                try {
                    PictureDrawable pictureDrawable = requestBuilder
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                    // SVG cannot be serialized so it's not worth to cache it
                            .load(uri)
                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                    views.setImageViewBitmap(R.id.away_crest,
                            Utilies.getBitmapFromView(pictureDrawable));
                } catch (InterruptedException | ExecutionException e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
            }

            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, description);
            }

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.putExtra(MainActivity.SELECTED_MATCH, matchId);
            launchIntent.putExtra(MainActivity.CURRENT_FRAGMENT, 2);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.football_score_widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private int getWidgetWidth(AppWidgetManager appWidgetManager, int appWidgetId) {
        // Prior to Jelly Bean, widgets were always their default size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_football_today_score_default_width);
        }
        // For Jelly Bean and higher devices, widgets can be resized - the current size can be
        // retrieved from the newly added App Widget Options
        return getWidgetWidthFromOptions(appWidgetManager, appWidgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            // The width returned is in dp, but we'll convert it to pixels to match the other widths
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp,
                    displayMetrics);
        }
        return getResources().getDimensionPixelSize(R.dimen.widget_football_today_score_default_width);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.home_crest, description);
        views.setContentDescription(R.id.away_crest, description);
    }
}
