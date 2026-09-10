package com.pit.bahromtaxi.domain

/**
 * Межгородняя поездка "по сбору мест": один водитель, несколько пассажиров бронируют места
 * в одной машине. Отличается от Ride тем, что это не 1:1 (пассажир:водитель), а 1:N — поэтому
 * отдельная модель, а не ещё один OrderType поверх Ride.
 *
 * Методика набора:
 *  - Водитель создаёт поездку с городом отправления/назначения, датой (можно "как наберётся" —
 *    scheduledAt = null, либо конкретная дата за несколько дней вперёд) и числом мест в машине.
 *  - Пассажиры видят открытые поездки по нужному направлению и бронируют 1+ мест.
 *  - Статус OPEN, пока bookedSeats < totalSeats. Как только машина набралась — FULL.
 *  - Водитель может вручную отправиться (DEPART) даже не набрав полную машину — тогда поездка
 *    переходит в IN_PROGRESS с тем числом пассажиров, что успели забронировать.
 *  - COMPLETE — доехали, CANCELLED — водитель отменил (например, никто не забронировал к дате).
 */
enum class IntercityStatus { OPEN, FULL, IN_PROGRESS, COMPLETED, CANCELLED }

data class IntercityTrip(
    val id: String,
    val driverId: String,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val fromCity: String,
    val toCity: String,
    val totalSeats: Int,
    val bookedSeats: Int,
    val pricePerSeat: Double,
    val scheduledAt: String? = null,
    val status: IntercityStatus = IntercityStatus.OPEN,
    val createdAt: String? = null,
    /** Сколько мест забронировал я (для пассажирского экрана) — 0, если не бронировал. */
    val myBookedSeats: Int = 0
) {
    val seatsLeft: Int get() = (totalSeats - bookedSeats).coerceAtLeast(0)
}
