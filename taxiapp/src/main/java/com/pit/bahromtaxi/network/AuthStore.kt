package com.pit.bahromtaxi.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Токен/роль/имя текущего пользователя на этом устройстве. Пассажир и водитель — разные
 * роли, каждая сохраняется отдельно (ключи вида "token_passenger"/"token_driver"), поэтому
 * вход в одну роль не сбивает уже сохранённый вход в другую. token/userId/role/name — это
 * "активная" сессия, которую использует сетевой слой (Authorization-заголовок, WebSocket);
 * activate(role) переключает её на сохранённую сессию нужной роли, вызывается при входе
 * на экран пассажира/водителя.
 */
object AuthStore {
    private const val PREFS = "bahromtaxi_auth"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var token: String? = null
        private set

    var userId: String? = null
        private set

    var role: String? = null
        private set

    var name: String? = null
        private set

    fun isRegisteredAs(expectedRole: String): Boolean = prefs.getString("token_$expectedRole", null) != null

    /** Сохраняет сессию под конкретной ролью и сразу делает её активной. */
    fun saveSession(role: String, token: String, userId: String, name: String) {
        prefs.edit()
            .putString("token_$role", token)
            .putString("user_id_$role", userId)
            .putString("name_$role", name)
            .apply()
        activate(role)
    }

    /** Загружает ранее сохранённую сессию этой роли как активную — вызывать при входе на экран роли. */
    fun activate(role: String) {
        this.role = role
        this.token = prefs.getString("token_$role", null)
        this.userId = prefs.getString("user_id_$role", null)
        this.name = prefs.getString("name_$role", null)
    }

    /** Обновляет имя в активной роли (и в памяти, и на диске) — например, после редактирования профиля. */
    fun updateActiveName(newName: String) {
        val activeRole = role ?: return
        name = newName
        prefs.edit().putString("name_$activeRole", newName).apply()
    }

    /** Стирает сохранённую сессию активной роли (например, после удаления аккаунта) — вернёт на экран входа. */
    fun clearActiveRole() {
        val activeRole = role ?: return
        prefs.edit()
            .remove("token_$activeRole")
            .remove("user_id_$activeRole")
            .remove("name_$activeRole")
            .apply()
        token = null
        userId = null
        name = null
    }
}
