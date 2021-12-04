package com.example.android.locationreminder.data.source

import com.example.android.locationreminder.locationreminders.data.ReminderDataSource
import com.example.android.locationreminder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationreminder.locationreminders.data.dto.Result

class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
  ReminderDataSource {

  private var shouldReturnError = false

  fun setReturnError(value: Boolean) {
    shouldReturnError = value
  }

  override suspend fun getReminders(): Result<List<ReminderDTO>> {
    if (shouldReturnError) return Result.Error("Tasks not found")
    reminders?.let { return Result.Success(ArrayList(it)) }
    return Result.Error("Tasks not found")
  }

  override suspend fun saveReminder(reminder: ReminderDTO) {
    reminders?.add(reminder)
  }

  override suspend fun getReminder(id: String): Result<ReminderDTO> {
    reminders?.let {
      it.firstOrNull { reminderDTO -> reminderDTO.id == id }?.let { reminder ->
        return Result.Success(reminder)
      }
    }
    return Result.Error("Reminder not found!")
  }

  override suspend fun deleteAllReminders() {
    reminders?.clear()
  }
}