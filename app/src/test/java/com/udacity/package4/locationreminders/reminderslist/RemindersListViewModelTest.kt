package com.udacity.package4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.package4.MainCoroutineRule
import com.udacity.package4.source.FakeDataSource
import com.udacity.package4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
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
class RemindersListViewModelTest {

  @get:Rule
  var mainCoroutineRule = MainCoroutineRule()

  @get:Rule
  var instantExecutorRule = InstantTaskExecutorRule()

  private lateinit var remindersDataSource: FakeDataSource

  private lateinit var remindersListViewModel: RemindersListViewModel

  @Before
  fun setupViewModel() {
    val reminder1 = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    val reminder2 = ReminderDTO("Title2", "Description2", "New York", 40.0, 74.0)
    remindersDataSource = FakeDataSource(mutableListOf(reminder1, reminder2))

    remindersListViewModel =
      RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersDataSource)
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun addNewReminder_dataUpdated() = mainCoroutineRule.runBlockingTest {
    remindersDataSource.saveReminder(ReminderDTO("Title3", "Description3", "New York", 40.0, 74.0))

    remindersListViewModel.loadReminders()

    val value = remindersListViewModel.remindersList.getOrAwaitValue()
    assertThat(value, (Matchers.not(Matchers.nullValue())))
    assertThat(value.size, Matchers.`is`(3))
  }


  @Test
  fun loadReminders_loading() {
    mainCoroutineRule.pauseDispatcher()

    remindersListViewModel.loadReminders()

    assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), Matchers.`is`(true))

    mainCoroutineRule.resumeDispatcher()

    assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), Matchers.`is`(false))
  }

  @Test
  fun loadRemindersOnError_snackbarUpdated() = mainCoroutineRule.runBlockingTest {
    remindersDataSource.setReturnError(true)

    remindersListViewModel.loadReminders()

    assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(),
               Matchers.`is`("Reminders not found!"))
    assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), Matchers.`is`(true))
  }

}