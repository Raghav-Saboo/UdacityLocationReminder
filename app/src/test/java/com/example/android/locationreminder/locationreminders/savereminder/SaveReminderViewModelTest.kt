package com.example.android.locationreminder.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.android.locationreminder.MainCoroutineRule
import com.example.android.locationreminder.R
import com.example.android.locationreminder.base.NavigationCommand
import com.example.android.locationreminder.data.source.FakeDataSource
import com.example.android.locationreminder.getOrAwaitValue
import com.example.android.locationreminder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationreminder.locationreminders.data.dto.Result
import com.example.android.locationreminder.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {

  @get:Rule
  var mainCoroutineRule = MainCoroutineRule()

  @get:Rule
  var instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var remindersDataSource: FakeDataSource

  private lateinit var saveReminderViewModel: SaveReminderViewModel

  @Before
  fun setupViewModel() {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    remindersDataSource = FakeDataSource(mutableListOf(reminder))

    saveReminderViewModel =
      SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersDataSource)
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun validateAndSaveReminder_dataSourceUpdated() = mainCoroutineRule.runBlockingTest {
    val reminder = ReminderDataItem("Title3", "Description3", "New York", 40.0, 74.0)

    saveReminderViewModel.validateAndSaveReminder(reminder)

    assertThat(remindersDataSource.reminders?.size, Matchers.`is`(2))
    val result: Result<ReminderDTO> = remindersDataSource.getReminder(reminder.id)
    assert(result is Result.Success<ReminderDTO>)
    val reminderDTO = (result as Result.Success<ReminderDTO>).data
    assertThat(reminderDTO.title, Matchers.`is`(reminder.title))
    assertThat(reminderDTO.description, Matchers.`is`(reminder.description))
    assertThat(reminderDTO.location, Matchers.`is`(reminder.location))
    assertThat(reminderDTO.latitude, Matchers.`is`(reminder.latitude))
    assertThat(reminderDTO.longitude, Matchers.`is`(reminder.longitude))
  }

  @Test
  fun validateAndSaveReminder_toastAndNavigationCommandUpdated() =
    mainCoroutineRule.runBlockingTest {
      val reminder = ReminderDataItem("Title3", "Description3", "New York", 40.0, 74.0)

      saveReminderViewModel.validateAndSaveReminder(reminder)

      assertThat(saveReminderViewModel.showToast.getOrAwaitValue(),
                 Matchers.`is`(saveReminderViewModel.app.getString(R.string.reminder_saved)))
      assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue(),
                 Matchers.`is`(NavigationCommand.Back))
    }

  @Test
  fun validateAndSaveReminderWithEmptyTitle_snackbarUpdated() = mainCoroutineRule.runBlockingTest {
    val reminder = ReminderDataItem("", "Description3", "New York", 40.0, 74.0)

    saveReminderViewModel.validateAndSaveReminder(reminder)

    assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
               Matchers.`is`(R.string.err_enter_title))
  }

  @Test
  fun validateAndSaveReminderWithoutLocation_snackbarUpdated() = mainCoroutineRule.runBlockingTest {
    val reminder = ReminderDataItem("Title3", "Description3", null, 40.0, 74.0)

    saveReminderViewModel.validateAndSaveReminder(reminder)

    assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
               Matchers.`is`(R.string.err_select_location))
  }

}