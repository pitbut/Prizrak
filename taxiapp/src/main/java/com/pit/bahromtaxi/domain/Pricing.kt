package com.pit.bahromtaxi.domain

import kotlin.math.roundToInt

/**
 * Тарифная сетка агрегатора. Цену считает алгоритм по этим правилам — ни пассажир,
 * ни водитель её не назначают и не торгуются, это и держит доверие к сервису.
 */
object Tariff {
    const val BASE_FARE = 99.0        // посадка, ₽
    const val PER_KM = 14.0           // ₽ за км
    const val PER_MIN = 4.0           // ₽ за минуту в пути
    const val MIN_FARE = 149.0        // минимальный чек поездки, ₽
    const val COMMISSION_RATE = 0.01  // комиссия агрегатора — 1% от суммы поездки
}

data class PriceBreakdown(
    val baseFare: Double,
    val distanceCost: Double,
    val timeCost: Double,
    val demandFactor: Double,
    val total: Double,
    val commission: Double,
    val driverPayout: Double
)

object PricingEngine {

    fun calculate(distanceKm: Double, durationMin: Double, demandFactor: Double): PriceBreakdown {
        val distanceCost = distanceKm * Tariff.PER_KM
        val timeCost = durationMin * Tariff.PER_MIN
        val raw = (Tariff.BASE_FARE + distanceCost + timeCost) * demandFactor
        val total = raw.coerceAtLeast(Tariff.MIN_FARE).roundToStep(10.0)
        val commission = (total * Tariff.COMMISSION_RATE).roundToKopeks()
        val driverPayout = total - commission
        return PriceBreakdown(
            baseFare = Tariff.BASE_FARE,
            distanceCost = distanceCost,
            timeCost = timeCost,
            demandFactor = demandFactor,
            total = total,
            commission = commission,
            driverPayout = driverPayout
        )
    }

    private fun Double.roundToStep(step: Double): Double = (this / step).roundToInt() * step
    private fun Double.roundToKopeks(): Double = (this * 100.0).roundToInt() / 100.0
}
