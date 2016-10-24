package com.pabrou.live;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.pabrou.live.service.RadioService;

/**
 * Created by pablo on 22/10/16.
 */

public class NotificationHelper {

    public final static String ACTION_PLAY = "com.pabrou.live.ACTION_PLAY";
    public final static String ACTION_PAUSE = "com.pabrou.live.ACTION_PAUSE";
    public final static String ACTION_CLOSE = "com.pabrou.live.ACTION_STOP";

    public final static String EXTRA_URL = "com.pabrou.live.EXTRA_URL";


    public static Notification newPlayingNotification(Context context, String radioUrl){

        Intent pauseIntent = new Intent(context, RadioService.class);
        pauseIntent.setAction(ACTION_PAUSE);

        PendingIntent pausePendingIntent = PendingIntent.getService(context, 1,
                pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return getNotificationBuilder(context)
                .addAction(android.R.drawable.ic_media_pause,
                        context.getString(R.string.pause), pausePendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setShowActionsInCompactView(1)
                        .setShowCancelButton(true))
                .setContentText(radioUrl)
                .build();
    }

    public static Notification newPausedNotification(Context context, String radioUrl){

        Intent playIntent = new Intent(context, RadioService.class);
        playIntent.setAction(ACTION_PLAY);
        playIntent.putExtra(EXTRA_URL, radioUrl);

        PendingIntent playPendingIntent = PendingIntent.getService(context, 1,
                playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return getNotificationBuilder(context)
                .addAction(android.R.drawable.ic_media_play,
                        context.getString(R.string.play), playPendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setShowActionsInCompactView(1)
                        .setShowCancelButton(true))
                .setContentText(radioUrl)
                .build();
    }


    private static android.support.v4.app.NotificationCompat.Builder getNotificationBuilder(Context context){

        PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent closeIntent = new Intent(context, RadioService.class);
        closeIntent.setAction(ACTION_CLOSE);

        PendingIntent closePendingIntent = PendingIntent.getService(context, 2,
                closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Radio")
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        context.getString(R.string.close),
                        closePendingIntent)
                .setContentIntent(mainActivityPendingIntent);
    }

    public static final PendingIntent newMainActivityPendingIntent(Context context){
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 99, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
