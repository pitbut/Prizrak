package com.pit.bahromtaxi.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Email/пароль: если аккаунта с таким email ещё нет — создаёт его, иначе логинит
 * (для пользователя это один и тот же экран "Продолжить", без отдельной регистрации).
 */
object EmailAuthClient {

    suspend fun signInOrRegister(email: String, password: String): String {
        val auth = FirebaseAuth.getInstance()
        return try {
            signIn(auth, email, password)
        } catch (e: Exception) {
            register(auth, email, password)
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
                .addOnSuccessListener { resolveToken(it.user, cont) }
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
