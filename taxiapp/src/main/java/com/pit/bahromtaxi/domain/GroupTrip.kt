package com.pit.bahromtaxi.domain

/**
 * Групповая поездка: пассажир указывает направление, число людей и желаемое время — не
 * бронирует место в чужой машине (как в Intercity), а публикует заявку. Онлайн-водители видят
 * её и предлагают свои места (одна машина не набирает всю группу — предложений может быть
 * несколько, от разных водителей, пока сумма предложенных мест не закроет peopleCount).
 * Пассажир принимает нужные предложения (одно или несколько). Как только принятых мест хватает
 * на всю группу — заявка переходит в CONFIRMED, и обеим сторонам приходит напоминание-будильник
 * за час до отправления (см. TripReminderScheduler).
 *
 * Осознанно не делаю здесь автоматическое распределение "кто из водителей повезёт кого" —
 * пассажир сам решает, чьи предложения принять, точно как при обычном заказе такси он выбирает
 * ехать или нет. Автоматический подбор одной вместительной машины вместо нескольких обычных —
 * следующий шаг, когда у водителей появится поле реальной вместимости салона.
 */
enum class GroupTripStatus { OPEN, CONFIRMED, CANCELLED, COMPLETED }
enum class GroupTripOfferStatus { PENDING, ACCEPTED, REJECTED, WITHDRAWN }

data class GroupTripOffer(
    val id: String,
    val driverId: String,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val carMake: String? = null,
    val carPlate: String? = null,
    val seatsOffered: Int,
    val status: GroupTripOfferStatus = GroupTripOfferStatus.PENDING
)

data class GroupTrip(
    val id: String,
    val passengerId: String,
    val passengerName: String? = null,
    val passengerPhone: String? = null,
    val fromCity: String,
    val toCity: String,
    val peopleCount: Int,
    val seatsConfirmed: Int,
    val desiredAt: String? = null,
    val status: GroupTripStatus = GroupTripStatus.OPEN,
    val createdAt: String? = null,
    val offers: List<GroupTripOffer> = emptyList()
) {
    val seatsStillNeeded: Int get() = (peopleCount - seatsConfirmed).coerceAtLeast(0)
}
