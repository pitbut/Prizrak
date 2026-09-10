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
 *  - COMPLETE — доехали, CANCELLED — можно отменить на любом этапе (в том числе уже в пути,
 *    если поездка сорвалась) — и водителем, и (пока не в пути) пассажиром для своей брони.
 *
 * Сбор по адресам (PickupMode/DropoffMode) — намеренно упрощённая версия "как в жизни":
 * водитель либо назначает одну точку встречи/высадки для всех (SINGLE — проще и быстрее для
 * коротких направлений), либо собирает/развозит каждого пассажира по его адресу (COLLECT —
 * тогда при бронировании пассажир указывает свой адрес). Авто-построение оптимального маршрута
 * объезда всех адресов сюда сознательно не включено — это отдельная сложная задача (VRP), а
 * для MVP водителю достаточно видеть список адресов с бронирований и проложить маршрут самому.
 */
enum class IntercityStatus { OPEN, FULL, IN_PROGRESS, COMPLETED, CANCELLED }

enum class TripPointMode { SINGLE, COLLECT }

data class IntercityBooking(
    val id: String,
    val passengerId: String,
    val passengerName: String? = null,
    val passengerPhone: String? = null,
    val seats: Int,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null
)

data class IntercityTrip(
    val id: String,
    val driverId: String,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverCarMake: String? = null,
    val driverCarPlate: String? = null,
    val fromCity: String,
    val toCity: String,
    val totalSeats: Int,
    val bookedSeats: Int,
    val pricePerSeat: Double,
    val scheduledAt: String? = null,
    val status: IntercityStatus = IntercityStatus.OPEN,
    val createdAt: String? = null,
    /** Сколько мест забронировал я (для пассажирского экрана) — 0, если не бронировал. */
    val myBookedSeats: Int = 0,
    val pickupMode: TripPointMode = TripPointMode.SINGLE,
    val dropoffMode: TripPointMode = TripPointMode.SINGLE,
    val pickupPoint: String? = null,
    val dropoffPoint: String? = null,
    /** Список бронирований с адресами — приходит только водителю в его собственных поездках. */
    val bookings: List<IntercityBooking> = emptyList()
) {
    val seatsLeft: Int get() = (totalSeats - bookedSeats).coerceAtLeast(0)
}
