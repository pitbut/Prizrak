package com.pit.bahromtaxi.auth

import com.google.firebase.auth.FirebaseAuth
import com.pit.bahromtaxi.network.ApiClient
import com.pit.bahromtaxi.network.PhoneSendCodeRequest
import com.pit.bahromtaxi.network.PhoneVerifyCodeRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Вход по телефону через SMS-шлюз на нашем backend (Eskiz.uz), а не через встроенный
 * Firebase Phone Auth — тот требовал Play Integrity, которая не проходила без публикации
 * приложения в Google Play Console. Backend сам шлёт SMS и проверяет код, а после успешной
 * проверки выдаёт Firebase custom token — им мы логинимся в тот же Firebase-проект, так что
 * дальше (матчинг по uid, /auth/register) ничего не меняется.
 */
object PhoneAuthClient {

    private val api = ApiClient.service

    suspend fun sendCode(phone: String) {
        api.sendPhoneCode(PhoneSendCodeRequest(phone))
    }

    suspend fun verifyCode(phone: String, code: String): String {
        val response = api.verifyPhoneCode(PhoneVerifyCodeRequest(phone, code))
        return signInWithCustomToken(response.firebaseCustomToken)
    }

    private suspend fun signInWithCustomToken(customToken: String): String =
        suspendCancellableCoroutine { cont ->
            FirebaseAuth.getInstance().signInWithCustomToken(customToken)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        if (cont.isActive) cont.resumeWithException(IllegalStateException("Firebase не вернул пользователя"))
                        return@addOnSuccessListener
                    }
                    user.getIdToken(true)
                        .addOnSuccessListener tokenListener@{ tokenResult ->
                            if (!cont.isActive) return@tokenListener
                            val token = tokenResult.token
                            if (token != null) cont.resume(token)
                            else cont.resumeWithException(IllegalStateException("Не удалось получить токен"))
                        }
                        .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
                }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    // ВРЕМЕННО: показывает всю цепочку причин ошибки, а не только обобщённое сообщение.
    fun describeError(e: Throwable): String {
        val sb = StringBuilder()
        var current: Throwable? = e
        var depth = 0
        while (current != null && depth < 6) {
            if (depth > 0) sb.append(" ← ")
            sb.append(current.javaClass.simpleName).append(": ").append(current.message ?: "(без сообщения)")
            current = current.cause
            depth++
        }
        return sb.toString()
    }
}
