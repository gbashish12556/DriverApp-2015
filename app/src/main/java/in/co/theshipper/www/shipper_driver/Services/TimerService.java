package in.co.theshipper.www.shipper_driver.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import in.co.theshipper.www.shipper_driver.Activities.FullActivity;
import in.co.theshipper.www.shipper_driver.Constants;
import in.co.theshipper.www.shipper_driver.Helper;
import in.co.theshipper.www.shipper_driver.R;

/**
 * Created by Ashish on 8/25/2016.
 */
public class TimerService extends Service {

    private String ToastString = "";
    private Notification notification;
    private long start_time = 0;
    private Timer timer;
    private Calendar calendar2;
    private long diff = 0, diffsec = 0, diffmin = 0, diffHours = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        if(getApplicationContext() != null){

            start_time = Long.parseLong(Helper.getPreference(getApplicationContext(),Constants.Keys.LOADING_START_TIME));

        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent i = new Intent(this, FullActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        android.support.v7.app.NotificationCompat.Builder builder = new android.support.v7.app.NotificationCompat.Builder(this);

        notification = builder.setContentIntent(pi)
                .setSmallIcon(R.drawable.vehicle_1).setTicker("SHIPPER").setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle("SHIPPER")
                .setContentText("Timer Running").build();

        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(1317, notification);
        startTracking();
        return START_NOT_STICKY;

    }

    public void startTracking() {
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                showTime();

            }
        }, Constants.Config.SEND_DISTANCE_REQUEST_DELAY, Constants.Config.SEND_DISTANCE_REQUEST_PERIOD);

    }

    public void showTime(){

        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        calendar2 = Calendar.getInstance(tz);
        diff = (calendar2.getTimeInMillis() - start_time);
        diffsec = (diff / (1000)) % 60;
        diffmin = (diff / (60 * 1000)) % 60;
        diffHours = diff / (60 * 60 * 1000);
        ToastString =  String.valueOf(diffHours) + "hrs :" + String.valueOf(diffmin) + "min :" + String.valueOf(diffsec)+"sec";

        if(getApplicationContext() != null) {

            Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {

                @Override
                public void run() {

                    Helper.ToastShort(getApplicationContext(), ToastString);

                }
            });

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (timer != null) {

            timer.cancel();
            timer = null;

        }

        stopForeground(true);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}