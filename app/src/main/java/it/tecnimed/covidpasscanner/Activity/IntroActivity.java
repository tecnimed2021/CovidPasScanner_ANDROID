package it.tecnimed.covidpasscanner.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import it.tecnimed.covidpasscanner.R;
import it.tecnimed.covidpasscanner.VL.VLTimer;

public class IntroActivity extends AppCompatActivity implements  VLTimer.OnTimeElapsedListener
{
    private static final String TAG_LOG = IntroActivity.class.getName();

    private VLTimer mTimeVar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.intro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        mTimeVar = VLTimer.create(this);
        mTimeVar.startSingle(2000);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
//        super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    //
    // Interaction
    //
    @Override
    public void VLTimerTimeElapsed(VLTimer timer)
    {
 //       CovidPasScannerApplication app = (CovidPasScannerApplication)getApplication();

        final ImageView iv  = (ImageView)findViewById(R.id.IntroIVLogo);

        if(timer == mTimeVar)
            nextActivity();
    }


    //
    // Activity Specific
    //
    public void nextActivity()
    {
        // Istanza Intent esplicito
        final Intent intent = new Intent(this, FirstActivity.class);

        // Start nuova Activity
        startActivity(intent);

        // Terminazione Activity corrente
        finish();
    }
}
