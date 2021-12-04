package com.example.android.locationreminder.locationreminders.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.android.locationreminder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationreminder.locationreminders.data.local.RemindersDao

/**
 * The Room Database that contains the reminders table.
 */
@Database(entities = [ReminderDTO::class], version = 1, exportSchema = false)
abstract class RemindersDatabase : RoomDatabase() {

  abstract fun reminderDao(): RemindersDao
}