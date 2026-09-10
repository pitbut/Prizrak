package com.pit.bahromtaxi.maps

/** Toolkit-независимая точка — конвертируется в osmdroid GeoPoint только в UI-слое. */
data class Coordinate(val lat: Double, val lng: Double)

/** Центр Ташкента — используется как точка отсчёта по умолчанию для карты и поиска адреса. */
val TASHKENT = Coordinate(41.2995, 69.2401)
