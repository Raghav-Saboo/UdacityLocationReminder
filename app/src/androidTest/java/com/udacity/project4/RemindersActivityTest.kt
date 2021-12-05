package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers
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

    onView(withText(reminder.title)).check(matches(isDisplayed()))
    onView(withText(reminder.description)).check(matches(isDisplayed()))
    onView(withText(reminder.location)).check(matches(isDisplayed()))

    // Make sure the activity is closed before resetting the db:
    activityScenario.close()
  }

  @Test
  fun validateAndSaveReminderWithEmptyTitle() = runBlocking {
    // Start up Reminders screen
    val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
    dataBindingIdlingResource.monitorActivity(activityScenario)

    onView(withId(R.id.addReminderFAB)).perform(click())

    onView(withId(R.id.saveReminder)).perform(click())

    onView(withId(com.google.android.material.R.id.snackbar_text))
      .check(matches(withText(R.string.err_enter_title)))

    // Make sure the activity is closed before resetting the db:
    activityScenario.close()
  }

  @Test
  fun validateAndSaveReminderWithEmptyLocation() = runBlocking {
    // Start up Reminders screen
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)

    val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
    dataBindingIdlingResource.monitorActivity(activityScenario)

    onView(withId(R.id.addReminderFAB)).perform(click())

    onView(withId(R.id.reminderTitle)).perform(typeText(reminder.title))
    onView(withId(R.id.reminderDescription)).perform(typeText(reminder.description))

    Espresso.closeSoftKeyboard()

    onView(withId(R.id.saveReminder)).perform(click())

    onView(withId(com.google.android.material.R.id.snackbar_text))
      .check(matches(withText(R.string.err_select_location)))

    // Make sure the activity is closed before resetting the db:
    activityScenario.close()
  }

  @Test
  fun validateSaveReminderToast() = runBlocking {
    val reminder = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)

    // Start up Reminders screen
    val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
    dataBindingIdlingResource.monitorActivity(activityScenario)

    onView(withId(R.id.addReminderFAB)).perform(click())

    onView(withId(R.id.reminderTitle)).perform(typeText(reminder.title))
    onView(withId(R.id.reminderDescription)).perform(typeText(reminder.description))

    Espresso.closeSoftKeyboard()

    val viewModel =
      (dataBindingIdlingResource.getBindings()[0] as FragmentSaveReminderBinding).viewModel

    viewModel?.reminderSelectedLocationStr?.postValue(reminder.location)
    viewModel?.latitude?.postValue(reminder.latitude)
    viewModel?.longitude?.postValue(reminder.longitude)

    onView(withId(R.id.saveReminder)).perform(click())


    onView(withText(R.string.reminder_saved))
      .inRoot(withDecorView(Matchers.not(Matchers.`is`(dataBindingIdlingResource.activity.window.decorView))))
      .check(matches(isDisplayed()))

    onView(withText(reminder.title)).check(matches(isDisplayed()))
    onView(withText(reminder.description)).check(matches(isDisplayed()))

    // Make sure the activity is closed before resetting the db:
    activityScenario.close()
  }

}
