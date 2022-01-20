package it.tecnimed.covidpasscanner.VL;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;


public class VLTimer
{
    public static final int TIME_ELAPSED = 1;

    private Context mContext;

    private boolean mMode;
    private long mTime;
    private final vltimerHandler mHandler;
    private vltimerThread mThread;
    private OnTimeElapsedListener mListener;




    // Handler
    private static class vltimerHandler extends Handler
    {
        private WeakReference<VLTimer> mTimer;

        public vltimerHandler(final VLTimer timer)
        {
            this.mTimer = new WeakReference<VLTimer>(timer);
        }

        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == TIME_ELAPSED)
            {
                // Chiamare interface
                final VLTimer timer = mTimer.get();
                if(timer != null)
                {
                    timer.mListener.VLTimerTimeElapsed(timer);
                    timer.finalizeTimer();
                }
            }
        }
    }

    // Thread
    private class vltimerThread extends Thread
    {
        @Override
        public void run()
        {
            // Sleep for requested time
            try {
                while(mTime > 0) {
                    sleep(1);
                    mTime--;
                }
            } catch (InterruptedException e){}

            // Create a message in child thread.
            Message msg = new Message();
            msg.what = TIME_ELAPSED;
            // Put the message in main thread message queue.
            mHandler.sendMessage(msg);
        }
    }




    // Costruttore
    private VLTimer(Object obj, long time)
    {
        mMode = false;
        mTime = time;
        mHandler = new vltimerHandler(this);
        mThread = new vltimerThread();
        mListener = (VLTimer.OnTimeElapsedListener)obj;
    }

    public static VLTimer create(Object obj)
    {
        return new VLTimer(obj, 0);
    }

    public void startSingle(long time)
    {
        mMode = false;
        mTime = time;
        mThread.start();
    }

    public void retriggerSingle(long time)
    {
        mTime = time;
    }

    public void startRepeating(long time)
    {
        mMode = true;
        mTime = time;
        mThread.start();
    }

    public void stopRepeating()
    {
        mMode = false;
        mTime = 1;
    }

    private void finalizeTimer()
    {
        if(mMode == true) {
            mThread = new vltimerThread();
            mThread.start();
        }
    }

    // Interface
    public interface OnTimeElapsedListener
    {
        public void VLTimerTimeElapsed(VLTimer timer);
    }
}
