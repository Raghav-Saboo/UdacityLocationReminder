package com.udacity.project4.source.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalDataRepositoryTest {

  private lateinit var localDataSource: RemindersLocalRepository
  private lateinit var database: RemindersDatabase

  // Executes each task synchronously using Architecture Components.
  @get:Rule
  var instantExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setup() {
    // Using an in-memory database for testing, because it doesn't survive killing the process.
    database = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      RemindersDatabase::class.java
    )
      .allowMainThreadQueries()
      .build()

    localDataSource =
      RemindersLocalRepository(
        database.reminderDao(),
        Dispatchers.Main
      )
  }

  @After
  fun cleanUp() {
    database.close()
  }

  @Test
  fun saveReminder_retrievesReminder() = runBlocking {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    localDataSource.saveReminder(reminder)

    val result: Result<ReminderDTO> = localDataSource.getReminder(reminder.id)

    assert(result is Success<ReminderDTO>)
    val reminderDTO = (result as Success<ReminderDTO>).data
    MatcherAssert.assertThat(reminderDTO.id, `is`(reminder.id))
    MatcherAssert.assertThat(reminderDTO.title, `is`(reminder.title))
    MatcherAssert.assertThat(reminderDTO.description, `is`(reminder.description))
    MatcherAssert.assertThat(reminderDTO.location, `is`(reminder.location))
    MatcherAssert.assertThat(reminderDTO.longitude, `is`(reminder.longitude))
    MatcherAssert.assertThat(reminderDTO.latitude, `is`(reminder.latitude))
  }

  @Test
  fun deleteAllReminders_deletesAllReminder() = runBlocking {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    localDataSource.saveReminder(reminder)

    localDataSource.deleteAllReminders()

    val result: Result<List<ReminderDTO>> = localDataSource.getReminders()
    assert(result is Success<List<ReminderDTO>>)
    result as Success
    MatcherAssert.assertThat(result.data, Matchers.empty())
  }

  @Test
  fun getNonExistingReminder_deletesAllReminder() = runBlocking {
    val result: Result<ReminderDTO> = localDataSource.getReminder("randomId")

    assert(result is Result.Error)
    result as Result.Error
    MatcherAssert.assertThat(result.message, Matchers.`is`("Reminder not found!"))
  }

}