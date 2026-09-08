package com.pit.bahromtaxi.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/** Явные операции регистрации и входа по email/паролю — режим выбирает пользователь на экране, а не угадывается по ошибке. */
object EmailAuthClient {

    suspend fun register(email: String, password: String): String {
        val auth = FirebaseAuth.getInstance()
        return suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.sendEmailVerification()
                    resolveToken(result.user, cont)
                }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(friendlyError(it)) }
        }
    }

    suspend fun signIn(email: String, password: String): String {
        val auth = FirebaseAuth.getInstance()
        return suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { resolveToken(it.user, cont) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(friendlyError(it)) }
        }
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

    private fun friendlyError(e: Exception): Exception = when (e) {
        is FirebaseAuthUserCollisionException ->
            IllegalStateException("Этот email уже зарегистрирован — нажмите «Войти»")
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException ->
            IllegalStateException("Неверный email или пароль, либо аккаунта нет — нажмите «Зарегистрироваться»")
        else -> e
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
