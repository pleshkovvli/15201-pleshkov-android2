package ru.nsu.ccfit.pleshkov.findlocation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

private object JacksonAPI {
    private val mapper = jacksonObjectMapper()
    private val writer = mapper.writerWithDefaultPrettyPrinter()
    fun toJson(value: Any) : String = writer.writeValueAsString(value)
    fun <T> fromJson(json: String, clazz: Class<T>) : T = mapper.readValue<T>(json, clazz)
}

fun Any.toJson() = JacksonAPI.toJson(this)
fun <T> String.fromJson(clazz: Class<T>) = JacksonAPI.fromJson(this, clazz)
