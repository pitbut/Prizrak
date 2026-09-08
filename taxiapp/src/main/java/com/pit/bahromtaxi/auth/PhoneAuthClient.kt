package com.pit.bahromtaxi.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

sealed class PhoneCodeResult {
    data class CodeSent(val verificationId: String) : PhoneCodeResult()
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneCodeResult()
}

/** Тонкая обёртка над Firebase Phone Auth в виде suspend-функций для Compose/coroutines. */
object PhoneAuthClient {

    suspend fun sendCode(activity: Activity, phoneNumber: String): PhoneCodeResult =
        suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (cont.isActive) cont.resume(PhoneCodeResult.AutoVerified(credential))
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    if (cont.isActive) cont.resume(PhoneCodeResult.CodeSent(verificationId))
                }
            }
            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

    suspend fun confirmCode(verificationId: String, code: String): String =
        signIn(PhoneAuthProvider.getCredential(verificationId, code))

    suspend fun signIn(credential: PhoneAuthCredential): String = suspendCancellableCoroutine { cont ->
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    cont.resumeWithException(IllegalStateException("Firebase не вернул пользователя"))
                    return@addOnSuccessListener
                }
                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val token = tokenResult.token
                        if (token != null) cont.resume(token)
                        else cont.resumeWithException(IllegalStateException("Не удалось получить токен"))
                    }
                    .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
            }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }
}
