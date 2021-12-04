package com.example.android.locationreminder

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.android.locationreminder.locationreminders.RemindersActivity
import com.example.android.locationreminder.locationreminders.data.ReminderDataSource
import com.example.android.locationreminder.locationreminders.data.dto.ReminderDTO
import com.example.android.locationreminder.locationreminders.data.local.LocalDB
import com.example.android.locationreminder.locationreminders.data.local.RemindersLocalRepository
import com.example.android.locationreminder.locationreminders.reminderslist.RemindersListViewModel
import com.example.android.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.example.android.locationreminder.util.DataBindingIdlingResource
import com.example.android.locationreminder.util.monitorActivity
import com.example.android.locationreminder.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.android.locationreminder.locationreminders.reminderslist.ReminderDataItem

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
  AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

  private lateinit var repository: ReminderDataSource
  private lateinit var appContext: Application

  private val dataBindingIdlingResource = DataBindingIdlingResource()

  /**
   * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
   * are not scheduled in the main Looper (for example when executed on a different thread).
   */
  @Before
  fun registerIdlingResource() {
    IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    IdlingRegistry.getInstance().register(dataBindingIdlingResource)
  }

  /**
   * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
   */
  @After
  fun unregisterIdlingResource() {
    IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
  }

  /**
   * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
   * at this step we will initialize Koin related code to be able to use it in out testing.
   */
  @Before
  fun init() {
    stopKoin()//stop the original app koin
    appContext = getApplicationContext()
    val myModule = module {
      viewModel {
        RemindersListViewModel(
          appContext,
          get() as ReminderDataSource
        )
      }
      single {
        SaveReminderViewModel(
          appContext,
          get() as ReminderDataSource
        )
      }
      single<ReminderDataSource> { RemindersLocalRepository(get()) }
      single { LocalDB.createRemindersDao(appContext) }
    }
    //declare a new koin module
    startKoin {
      modules(listOf(myModule))
    }
    //Get our real repository
    repository = get()

    //clear the data to start fresh
    runBlocking {
      repository.deleteAllReminders()
    }
  }

  @Test
  fun remindersWithOneReminder() = runBlocking {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    repository.saveReminder(reminder)

    // Start up Reminders screen
    val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
    dataBindingIdlingResource.monitorActivity(activityScenario)

    onView(ViewMatchers.withText(reminder.title)).check(ViewAssertions.matches(isDisplayed()))
    onView(ViewMatchers.withText(reminder.description)).check(ViewAssertions.matches(isDisplayed()))
    onView(ViewMatchers.withText(reminder.location)).check(ViewAssertions.matches(isDisplayed()))

    // Make sure the activity is closed before resetting the db:
    activityScenario.close()
  }

}
