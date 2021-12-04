package com.udacity.project4.reminderslist


import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentDirections
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.reminderslist.ReminderListFragmentTest.CustomAssertions.Companion.hasLocationReminder
import com.udacity.project4.source.FakeAndroidDataSource
import kotlinx.android.synthetic.main.it_reminder.view.description
import kotlinx.android.synthetic.main.it_reminder.view.title
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito


@RunWith(AndroidJUnit4::class)
@MediumTest
@ExperimentalCoroutinesApi
class ReminderListFragmentTest {

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.

  private lateinit var fakeAndroidDataSource: FakeAndroidDataSource
  private lateinit var reminderListViewModel: RemindersListViewModel

  @Before
  fun setup() {
    fakeAndroidDataSource = FakeAndroidDataSource()
    reminderListViewModel =
      RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeAndroidDataSource)
    stopKoin()

    val myModule = module {
      single {
        reminderListViewModel
      }
    }

    startKoin {
      modules(listOf(myModule))
    }
  }

  @Test
  fun reminders_displayedAsRemindersList() = runBlockingTest {
    val reminder1 = ReminderDTO("Title1", "Description1", "California", 36.0, 119.0)
    val reminder2 = ReminderDTO("Title2", "Description2", "New York", 40.0, 74.0)
    fakeAndroidDataSource.saveReminder(reminder1)
    fakeAndroidDataSource.saveReminder(reminder2)

    launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

    onView(withId(R.id.remindersRecyclerView))
      .check(hasLocationReminder(0, reminder1))
    onView(withId(R.id.remindersRecyclerView))
      .check(hasLocationReminder(1, reminder2))
  }

  @Test
  fun noReminders_displayedError() = runBlockingTest {
    fakeAndroidDataSource.deleteAllReminders()

    launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

    onView(ViewMatchers.withText("No Data"))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun clickAddReminderButton_navigateToAddSaveReminderFragment() = runBlockingTest {
    // GIVEN - On the reminders list screen
    val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
    val navController = Mockito.mock(NavController::class.java)
    scenario.onFragment {
      Navigation.setViewNavController(it.view!!, navController)
    }

    // WHEN - Click on the "+" button
    onView(withId(R.id.addReminderFAB)).perform(click())

    // THEN - Verify that we navigate to the save reminder screen
    Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
  }

  class CustomAssertions {
    companion object {
      fun hasLocationReminder(position: Int, reminder: ReminderDTO): ViewAssertion {
        return RecyclerViewItemCountAssertion(position, reminder)
      }
    }

    private class RecyclerViewItemCountAssertion(
      private val position: Int,
      private val reminder: ReminderDTO,
    ) :
      ViewAssertion {

      override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
        if (noViewFoundException != null) {
          throw noViewFoundException
        }

        if (view !is RecyclerView) {
          throw IllegalStateException("The asserted view is not RecyclerView")
        }

        if (view.adapter == null) {
          throw IllegalStateException("No adapter is assigned to RecyclerView")
        }

        val itemView = view.findViewHolderForAdapterPosition(position)?.itemView!!

        ViewMatchers.assertThat("RecyclerView item title",
                                itemView.title.text,
                                CoreMatchers.equalTo(reminder.title))
        ViewMatchers.assertThat("RecyclerView item description",
                                itemView.description.text,
                                CoreMatchers.equalTo(reminder.description))
      }
    }
  }

}