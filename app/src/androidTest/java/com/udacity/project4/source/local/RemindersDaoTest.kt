package com.udacity.project4.source.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

  // Executes each task synchronously using Architecture Components.
  @get:Rule
  var instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var database: RemindersDatabase

  @Before
  fun initDb() {
    // Using an in-memory database so that the information stored here disappears when the
    // process is killed.
    database = Room.inMemoryDatabaseBuilder(
      getApplicationContext(),
      RemindersDatabase::class.java
    ).allowMainThreadQueries().build()
  }

  @After
  fun closeDb() = database.close()

  @Test
  fun insertReminderAndGetById() = runBlockingTest {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    database.reminderDao().saveReminder(reminder)

    // WHEN - Get the task by id from the database.
    val loaded = database.reminderDao().getReminderById(reminder.id)

    // THEN - The loaded data contains the expected values.
    assertThat(loaded as ReminderDTO, notNullValue())
    assertThat(loaded.id, `is`(reminder.id))
    assertThat(loaded.title, `is`(reminder.title))
    assertThat(loaded.description, `is`(reminder.description))
    assertThat(loaded.location, `is`(reminder.location))
    assertThat(loaded.longitude, `is`(reminder.longitude))
    assertThat(loaded.latitude, `is`(reminder.latitude))
  }

  @Test
  fun getUnknownReminder() = runBlockingTest {
    val reminder = database.reminderDao().getReminderById("randomId")
    assertThat(reminder, nullValue())
  }

  @Test
  fun deleteAllReminders() = runBlockingTest {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    database.reminderDao().saveReminder(reminder)

    database.reminderDao().deleteAllReminders()

    assertThat(database.reminderDao().getReminders(), Matchers.empty())
  }

}