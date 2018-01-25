package ru.nsu.ccfit.pleshkov.findlocation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.Database
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel

@Database(name = LocationsDatabase.NAME, version = LocationsDatabase.VERSION)
object LocationsDatabase {
    const val NAME = "LocationDatabase"
    const val VERSION = 1
}

@Table(name = "locations", database = LocationsDatabase::class)
@JsonIgnoreProperties("modelAdapter")
class LocationWithRadius() : BaseModel() {
    @PrimaryKey(autoincrement = true)
    @Column(name = "id")
    var id: Long = 0

    @Column(name = "latitude")
    var latitude: Double = 0.0

    @Column(name = "longitude")
    var longitude: Double = 0.0

    @Column(name = "radius")
    var radius: Double = 0.0

    @Column(name = "name")
    var name: String = ""

    @Column(name = "description")
    var description: String = ""

    constructor(
            id: Long,
            latitude: Double,
            longitude: Double,
            radius: Double,
            name: String,
            description: String
    ) : this() {
        this.id = id
        this.latitude = latitude
        this.longitude = longitude
        this.radius = radius
        this.name = name
        this.description = description
    }
}
