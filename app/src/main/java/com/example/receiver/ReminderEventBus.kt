package com.example.receiver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ReminderEventBus {
    data class InAppReminder(
        val id: Int,
        val type: String, // "preorder", "subscription", "forwarding_storage"
        val title: String,
        val message: String
    )

    private val _currentReminder = MutableStateFlow<InAppReminder?>(null)
    val currentReminder = _currentReminder.asStateFlow()

    fun post(reminder: InAppReminder) {
        _currentReminder.value = reminder
    }

    fun dismiss() {
        _currentReminder.value = null
    }
}
