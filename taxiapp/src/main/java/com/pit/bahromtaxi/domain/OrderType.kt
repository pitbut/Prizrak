package com.pit.bahromtaxi.domain

enum class OrderType { RIDE, DELIVERY }

val OrderType.label: String
    get() = when (this) {
        OrderType.RIDE -> "Поездка"
        OrderType.DELIVERY -> "Доставка"
    }
