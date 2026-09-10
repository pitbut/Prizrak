package com.pit.bahromtaxi.ui.chat

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pit.bahromtaxi.data.RideRepository
import com.pit.bahromtaxi.network.ChatMessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private var rideId: String? = null

    var messages by mutableStateOf<List<ChatMessageDto>>(emptyList())
        private set
    var textInput by mutableStateOf("")
    var loading by mutableStateOf(true)
        private set
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun open(rideId: String) {
        if (this.rideId == rideId) return
        this.rideId = rideId
        loading = true
        viewModelScope.launch {
            messages = RideRepository.getMessages(rideId)
            loading = false
        }
        viewModelScope.launch {
            RideRepository.chatMessages.collect { msg ->
                if (msg.rideId == rideId) messages = messages + msg
            }
        }
    }

    fun sendText() {
        val rid = rideId ?: return
        val text = textInput.trim()
        if (text.isBlank()) return
        textInput = ""
        error = null
        sending = true
        viewModelScope.launch {
            val message = RideRepository.sendChatText(rid, text)
            if (message != null) messages = messages + message else error = RideRepository.lastError.value
            sending = false
        }
    }

    fun sendImage(context: Context, uri: Uri) {
        val rid = rideId ?: return
        error = null
        sending = true
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            if (bytes == null) {
                error = "Не удалось прочитать фото"
                sending = false
                return@launch
            }
            val message = RideRepository.sendChatImage(rid, bytes)
            if (message != null) messages = messages + message else error = RideRepository.lastError.value
            sending = false
        }
    }
}
