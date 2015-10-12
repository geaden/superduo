package barqsoft.footballscores;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import barqsoft.footballscores.sync.FootballSyncAdapter;

public class MainActivity extends ActionBarActivity {
    public static int selected_match_id;
    public static int current_fragment = 2;
    public static String LOG_TAG = "MainActivity";
    private final String save_tag = "Save Test";
    private PagerFragment my_main;

    public static final String SELECTED_MATCH = "Selected_match";
    public static final String CURRENT_FRAGMENT = "Pager_Current";
    public static final String MAIN_FRAGMENT = "my_main";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "Reached MainActivity onCreate");
        // @Gennady: Open specific match
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(SELECTED_MATCH)
                && extras.containsKey(CURRENT_FRAGMENT)) {
            Log.d(LOG_TAG, "Extras " + extras);
            selected_match_id = (int) extras.getDouble(SELECTED_MATCH, 0);
            current_fragment = extras.getInt(CURRENT_FRAGMENT);
        }
        if (savedInstanceState == null) {
            my_main = new PagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, my_main)
                    .commit();
        }
        // @Gennady: Instead of calling sync service directly initialize sync adapter
        // to update widgets data periodically
        FootballSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent start_about = new Intent(this, AboutActivity.class);
            startActivity(start_about);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(save_tag, "will save");
        Log.v(save_tag, "fragment: " + String.valueOf(my_main.mPagerHandler.getCurrentItem()));
        Log.v(save_tag, "selected id: " + selected_match_id);
        outState.putInt(CURRENT_FRAGMENT, my_main.mPagerHandler.getCurrentItem());
        outState.putInt(SELECTED_MATCH, selected_match_id);
        getSupportFragmentManager().putFragment(outState, MAIN_FRAGMENT, my_main);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(save_tag, "will retrieve");
        Log.v(save_tag, "fragment: " + String.valueOf(savedInstanceState.getInt(CURRENT_FRAGMENT)));
        Log.v(save_tag, "selected id: " + savedInstanceState.getInt(SELECTED_MATCH));
        current_fragment = savedInstanceState.getInt(CURRENT_FRAGMENT);
        selected_match_id = savedInstanceState.getInt(SELECTED_MATCH);
        my_main = (PagerFragment) getSupportFragmentManager().getFragment(savedInstanceState, MAIN_FRAGMENT);
        super.onRestoreInstanceState(savedInstanceState);
    }
}
