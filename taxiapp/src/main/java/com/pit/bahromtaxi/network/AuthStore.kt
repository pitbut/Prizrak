package com.pit.bahromtaxi.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Токен/роль/имя текущего пользователя на этом устройстве. Регистрация на backend
 * (см. ApiService.register) происходит один раз при первом входе в режим "Пассажир"
 * или "Водитель" — дальше токен переживает перезапуск приложения.
 */
object AuthStore {
    private const val PREFS = "bahromtaxi_auth"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()

    var role: String?
        get() = prefs.getString("role", null)
        set(value) = prefs.edit().putString("role", value).apply()

    var name: String?
        get() = prefs.getString("name", null)
        set(value) = prefs.edit().putString("name", value).apply()

    fun isRegisteredAs(expectedRole: String): Boolean = token != null && role == expectedRole
}
