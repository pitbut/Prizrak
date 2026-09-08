package com.pit.bahromtaxi.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

data class EmailAuthResult(val token: String, val isNewAccount: Boolean)

/**
 * Email/пароль: если аккаунта с таким email ещё нет — создаёт его и шлёт письмо
 * подтверждения, иначе логинит (для пользователя это один и тот же экран
 * "Продолжить", без отдельной формы регистрации).
 */
object EmailAuthClient {

    suspend fun signInOrRegister(email: String, password: String): EmailAuthResult {
        val auth = FirebaseAuth.getInstance()
        return try {
            EmailAuthResult(signIn(auth, email, password), isNewAccount = false)
        } catch (e: Exception) {
            EmailAuthResult(register(auth, email, password), isNewAccount = true)
        }
    }

    private suspend fun signIn(auth: FirebaseAuth, email: String, password: String): String =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { resolveToken(it.user, cont) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    private suspend fun register(auth: FirebaseAuth, email: String, password: String): String =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.sendEmailVerification()
                    resolveToken(result.user, cont)
                }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    /** Firebase сам шлёт письмо со ссылкой сброса — backend тут не участвует вообще. */
    suspend fun sendPasswordReset(email: String): Unit = suspendCancellableCoroutine { cont ->
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    /** Перечитывает пользователя у Firebase и возвращает, подтверждён ли уже email. */
    suspend fun refreshEmailVerified(): Boolean = suspendCancellableCoroutine { cont ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            if (cont.isActive) cont.resume(false)
            return@suspendCancellableCoroutine
        }
        user.reload()
            .addOnSuccessListener { if (cont.isActive) cont.resume(user.isEmailVerified) }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    suspend fun resendVerificationEmail(): Unit = suspendCancellableCoroutine { cont ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            if (cont.isActive) cont.resume(Unit)
            return@suspendCancellableCoroutine
        }
        user.sendEmailVerification()
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    private fun resolveToken(user: FirebaseUser?, cont: CancellableContinuation<String>) {
        if (user == null) {
            if (cont.isActive) cont.resumeWithException(IllegalStateException("Firebase не вернул пользователя"))
            return
        }
        user.getIdToken(true)
            .addOnSuccessListener { result ->
                if (!cont.isActive) return@addOnSuccessListener
                val token = result.token
                if (token != null) cont.resume(token)
                else cont.resumeWithException(IllegalStateException("Не удалось получить токен"))
            }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }
}
