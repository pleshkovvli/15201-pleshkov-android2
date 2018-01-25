package ru.nsu.ccfit.pleshkov.findlocation

import android.app.Application
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager

class FindLocationApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FlowManager.init(FlowConfig.Builder(this).build())

        val jsonString = resources.openRawResource(R.raw.locations).use {
            val readBytes = it.readBytes()
            String(readBytes)
        }

        val locations = jacksonObjectMapper()
                .readValue<HashMap<Int, LocationWithRadius>>(jsonString)

        locations.forEach {
            it.value.save()
        }

    }

    override fun onTerminate() {
        super.onTerminate()
        FlowManager.destroy()
    }
}