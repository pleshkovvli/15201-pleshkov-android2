package ru.nsu.ccfit.pleshkov.findlocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.raizlabs.android.dbflow.kotlinextensions.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import ru.nsu.ccfit.pleshkov.findlocation.LocationWithRadius_Table.id

private const val LOCATION_PERMISSION_CODE = 0

private const val GEOFENCE_SEND_KEY = "GEOFENCE_SEND_KEY"
private const val GEOFENCE_REQUEST_ID_KEY = "GEOFENCE_REQUEST_ID_KEY"
private const val GEOFENCE_TRANSITION_KEY = "GEOFENCE_TRANSITION_KEY"

private const val GEOFENCES_INITED = "GEOFENCES_INITED"
const val GEOFENCE_ID = "GEOFENCE_ID"
const val GEOFENCE_ACTION = "GEOFENCE_ACTION"

class MainActivity : AppCompatActivity() {
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var broadcastManager: LocalBroadcastManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            //this@MainActivity.intent = intent
            if (intent.action == GEOFENCE_SEND_KEY) {
                updateLocation(intent)
            }
        }
    }

    private fun updateLocation(intent: Intent) {
//        val id = intent.getLongExtra(GEOFENCE_REQUEST_ID_KEY, -1L)
//        val transition = intent.getIntExtra(GEOFENCE_TRANSITION_KEY, -1)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val id = preferences.getLong(GEOFENCE_ID, -1L)
        val transition = preferences.getInt(GEOFENCE_ACTION, -1)

        Log.d("GEO", "$id $transition")

        if (id == -1L || transition == -1) {
            return
        }

        async(UI) {
            this@MainActivity.onLocationChanged(id, transition)
        }
    }

    companion object {
        fun geofenceIntent(id: Long, transition: Int) = Intent(GEOFENCE_SEND_KEY)
                .also {
                    it.putExtra(GEOFENCE_REQUEST_ID_KEY, id)
                    it.putExtra(GEOFENCE_TRANSITION_KEY, transition)
                }

        fun launchIntent(context: Context, id: Long, transition: Int) = Intent(context, MainActivity::class.java).also {
            it.putExtra(GEOFENCE_REQUEST_ID_KEY, id)
            it.putExtra(GEOFENCE_TRANSITION_KEY, transition)
        }
    }

    fun onLocationChanged(triggeredId: Long, transition: Int) {
        if (transition == GEOFENCE_TRANSITION_ENTER) {
            val locationWithRadius = (
                    select from LocationWithRadius::class
                            where (id eq triggeredId)
                    ).list.firstOrNull() ?: return

            text_status.text = locationWithRadius.description
            Log.d("GEO", "YES")
        } else {
            text_status.text = resources.getString(R.string.unspecified_location)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        broadcastManager = LocalBroadcastManager.getInstance(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        val filter = IntentFilter()
        filter.addAction(GEOFENCE_SEND_KEY)
        broadcastManager.registerReceiver(broadcastReceiver, filter)
    }

    override fun onStart() {
        super.onStart()

        mode_button.setOnClickListener {

            val preferences = getPreferences(Context.MODE_PRIVATE)
            val geofencesInited = preferences.getBoolean(GEOFENCES_INITED, false)

            if (!geofencesInited) {
                initGeolocationFinder()
            } else {
                removeLocationFinder()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateLocation(intent)
        //text_status.text = resources.getString(R.string.unspecified_location)

        val preferences = getPreferences(Context.MODE_PRIVATE)
        val geofencesInited = preferences.getBoolean(GEOFENCES_INITED, false)

        mode_button.text = resources.getString(
                if (geofencesInited) R.string.turn_off_text
                else R.string.turn_on_text
        )
    }

    override fun onStop() {
        super.onStop()

        mode_button.setOnClickListener(null)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_CODE) {
            return
        }

        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGeolocationFinder()
        } else {
            showToast("Too bad, that's the whole purpose of the app \uD83E\uDD14\uD83E\uDD14")
            async {
                delay(1000L)
                finishAffinity()
            }
        }
    }


    private fun initGeolocationFinder() {
        val fineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
        val locationPermission = ContextCompat.checkSelfPermission(this, fineLocation)

        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    Array(1) { fineLocation },
                    LOCATION_PERMISSION_CODE
            )

            return
        }

        val request = geofencesRequest(allGeofences())

        val pendingIntent = GeofenceService.getPendingIntent(this)
        geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    showToast(resources.getString(R.string.text_start_monitor))

                    val preferencesEditor = getPreferences(Context.MODE_PRIVATE).edit()
                    with(preferencesEditor) {
                        putBoolean(GEOFENCES_INITED, true)
                        apply()
                    }


                    mode_button.text = resources.getString(R.string.turn_off_text)

                }
                .addOnFailureListener { e ->
                    showToast(resources.getString(R.string.text_failed_to_start))
                    Log.d("GEO", "$e")
                }
    }

    private fun geofencesRequest(geofences: List<Geofence>) =
            GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofences)
                    .build()

    private fun allGeofences(): List<Geofence> {
        val locations = (select from LocationWithRadius::class).list

        val geofences = locations.map {
            Geofence.Builder()
                    .setRequestId(it.id.toString())
                    .setCircularRegion(it.latitude, it.longitude, it.radius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
                    .build()
        }

        return geofences
    }

    private fun removeLocationFinder() {
        val pendingIntent = GeofenceService.getPendingIntent(this)

        geofencingClient.removeGeofences(pendingIntent)
                .addOnSuccessListener {
                    showToast(resources.getString(R.string.text_stop_monitor))

                    val preferencesEditor = getPreferences(Context.MODE_PRIVATE).edit()
                    with(preferencesEditor) {
                        putBoolean(GEOFENCES_INITED, false)
                        apply()
                    }

                    mode_button.text = resources.getString(R.string.turn_on_text)

                }
                .addOnFailureListener { e ->
                    showToast(resources.getString(R.string.text_failed_to_stop))
                }
    }

    override fun onDestroy() {
        broadcastManager.unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
