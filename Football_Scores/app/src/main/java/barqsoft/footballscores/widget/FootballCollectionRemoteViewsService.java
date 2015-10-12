package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

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
 * RemoteViewsService controlling the data being shown in the widget.
 *
 * @author Gennady Denisov
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class FootballCollectionRemoteViewsService  extends RemoteViewsService {
    private static final String LOG_TAG = FootballCollectionRemoteViewsService.class.getSimpleName();
    GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder;

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                requestBuilder = Glide.with(FootballCollectionRemoteViewsService.this)
                        .using(Glide.buildStreamModelLoader(Uri.class,
                                FootballCollectionRemoteViewsService.this), InputStream.class)
                        .from(Uri.class)
                        .as(SVG.class)
                        .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                        .sourceEncoder(new StreamEncoder())
                        .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                        .decoder(new SvgDecoder())
                        .placeholder(R.drawable.no_icon)
                        .error(R.drawable.no_icon);
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                // Construct current date to get scores for
                DateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
                String dateOfScores = mFormat.format(new Date(System.currentTimeMillis()));
                Uri footballScoreWithDate = DatabaseContract.scores_table.buildScoreWithDate();

                data = getContentResolver().query(footballScoreWithDate,
                        null, null, new String[]{dateOfScores}, null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }

            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_football_list_item);
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

                String description = getString(R.string.score_description, homeName, awayName, score);

                views.setTextViewText(R.id.home_name, homeName);
                views.setTextViewText(R.id.away_name, awayName);
                views.setTextViewText(R.id.data_textview, date);
                views.setTextViewText(R.id.score_textview, score);

                // Get home crest
                if (homeCrestUrl == null || homeCrestUrl.isEmpty()) {
                    views.setImageViewResource(R.id.home_crest, homeCrest);
                } else {
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
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }
                }

                // Away crest
                if (awayCrestUrl == null || awayCrestUrl.isEmpty()) {
                    views.setImageViewResource(R.id.away_crest, awayCrest);
                } else {
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
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }
                }

                // Content Descriptions for RemoteViews were only added in ICS MR1
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, description);
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MainActivity.SELECTED_MATCH, matchId);
                fillInIntent.putExtra(MainActivity.CURRENT_FRAGMENT, 2);
                views.setOnClickFillInIntent(R.id.widget_football_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_football_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(scoresAdapter.COL_ID);
                return position;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.home_crest, description);
                views.setContentDescription(R.id.away_crest, description);
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
