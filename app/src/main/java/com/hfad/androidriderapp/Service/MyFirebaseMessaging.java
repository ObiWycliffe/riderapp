package com.hfad.androidriderapp.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.hfad.androidriderapp.R;

public class MyFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

       if (remoteMessage.getNotification().getTitle().equals("Cancel"))
       {
           //Because this function is out of Main thread, if there is need of running Toast, there should be a handler to do that.
           Handler handler = new Handler(Looper.getMainLooper());
           handler.post(new Runnable() {
               @Override
               public void run() {
                   Toast.makeText(MyFirebaseMessaging.this, ""+remoteMessage.getNotification().getBody(), Toast.LENGTH_SHORT).show();
               }
           });

       }
       else if (remoteMessage.getNotification().getTitle().equals("Arrived"))
       {
          showArrivedNotification(remoteMessage.getNotification().getBody());

       }
    }

    private void showArrivedNotification(String body) {
        //Code functions only with android API 25 and below
        //Android APIs 26 and higher needs notification channel
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(),
                0, new Intent(), PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder builder = new Notification.Builder(getBaseContext());

        builder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Arrived")
                .setContentText(body)
                .setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1,builder.build());
    }
}
