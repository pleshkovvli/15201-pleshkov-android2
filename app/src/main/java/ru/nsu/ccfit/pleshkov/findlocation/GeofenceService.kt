package ru.nsu.ccfit.pleshkov.findlocation

import android.app.ActivityManager
import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.raizlabs.android.dbflow.kotlinextensions.*

const val GEOFENCE_SERVICE_NAME: String = "GEOFENCE_SERVICE_NAME";

class GeofenceService : IntentService(GEOFENCE_SERVICE_NAME) {

    companion object {
        private const val REQUEST_CODE = 0

        fun getPendingIntent(context: Context): PendingIntent =
                PendingIntent.getService(
                        context,
                        REQUEST_CODE,
                        Intent(context, GeofenceService::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
    }

    override fun onHandleIntent(intent: Intent?) {

        Log.d("GEO", "GOT INTENT!!")

        val event = GeofencingEvent.fromIntent(intent)
        if (event.hasError()) {
            Log.d("GEO", "${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition

        val geofence = event.triggeringGeofences.firstOrNull() ?: return

        val id = geofence.requestId.toLong()

        val location = (select from LocationWithRadius::class
                where (LocationWithRadius_Table.id eq id)
                ).list.firstOrNull() ?: return

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER
                || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            val preferencesEditor
                    = PreferenceManager.getDefaultSharedPreferences(this).edit()
            with(preferencesEditor) {
                putLong(GEOFENCE_ID, id)
                putInt(GEOFENCE_ACTION, transition)
                commit()
            }

            if (onForeground()) {
                val broadcastIntent = MainActivity.geofenceIntent(id, transition)
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
            } else {
                val launchIntent = MainActivity.launchIntent(this, id, transition)
                val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(this)
                        .setContentTitle(
                                resources.getString(
                                        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                                            R.string.text_enter_location
                                        else R.string.text_exit_location)
                        )
                        .setSmallIcon(R.drawable.ic_location_on_black_24dp)
                        .setContentText(location.name)
                        .setContentIntent(pendingIntent)

                val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(0, notification.build())

                Log.d("GEO", "not on foreground")
            }

        }
    }

    private fun onForeground(): Boolean {
        return checkStatus(ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
    }
}


fun Context.checkStatus(status: Int): Boolean {
    val manager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return true

    manager.runningAppProcesses.filter {
        it.importance == status
    }.map {
                it.pkgList
            }.forEach {
                if (it.any { string -> string == this.packageName }) {
                    return true
                }
            }

    return false
}