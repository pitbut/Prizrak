package com.pit.bahromtaxi.domain

enum class PaymentMethod { CASH, CARD, CLICK }

val PaymentMethod.label: String
    get() = when (this) {
        PaymentMethod.CASH -> "Наличные"
        PaymentMethod.CARD -> "Картой водителю"
        PaymentMethod.CLICK -> "Click"
    }
