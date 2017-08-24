package justintime.com.productscale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;

public class NotificationService extends Service implements ParticleEventHandler{


    //private long weightChangeSubscriptionID;
    private long lockedChangeSubscriptionID;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        final String deviceID;
        SharedPreferences sharedPreferences = this.getSharedPreferences("justintime.com.productscale", Context.MODE_PRIVATE);
        deviceID = sharedPreferences.getString("deviceID"," ");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!deviceID.equals(" ")) {
                        //weightChangeSubscriptionID = ParticleCloudSDK.getCloud().subscribeToDeviceEvents("weightChange", deviceID, NotificationService.this);
                        lockedChangeSubscriptionID = ParticleCloudSDK.getCloud().subscribeToDeviceEvents("lockWtChange", deviceID, NotificationService.this);
                    }
                    else
                        Log.wtf("Error", "Not started! Possible no deviceID available");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //ParticleCloudSDK.getCloud().unsubscribeFromEventWithID(weightChangeSubscriptionID);
                    ParticleCloudSDK.getCloud().unsubscribeFromEventWithID(lockedChangeSubscriptionID);
                } catch (ParticleCloudException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public void onEventError(Exception e) {
        Log.wtf("error", "error on event");
    }

    @Override
    public void onEvent(String eventName, ParticleEvent particleEvent) {

        //if(eventName.equals("weightChange")) {
        //    DisplayActivity.updateScaleReading();
        //}


        if(eventName.equals("lockWtChange")) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.scale)
                    .setContentTitle("Scale change!")
                    .setContentText("Scales Value Has Changed While Locked!!")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVibrate(new long[]{500, 1000, 500})
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setCategory(Notification.CATEGORY_EVENT)
                    .setAutoCancel(true)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setColor(Color.GREEN);

            Intent resultIntent = new Intent(this, MainScreen.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainScreen.class);
            stackBuilder.addNextIntent(resultIntent);

            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1234, mBuilder.build());
        }
    }
}
