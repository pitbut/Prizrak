package com.pit.bahromtaxi.network

/**
 * Адрес backend на VPS (см. taxiapp/README.md, раздел «Backend»). Контракт эндпоинтов
 * там же — если сервер на VPS в итоге назвал пути/поля иначе, поправить нужно только
 * здесь и в ApiService/Dto, остальной код их не касается.
 */
object NetworkConfig {
    const val BASE_URL = "https://taxi-api.robutpit.com/"
    const val WS_URL = "wss://taxi-api.robutpit.com/ws"
}
