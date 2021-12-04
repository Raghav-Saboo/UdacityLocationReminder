package com.example.android.locationreminder.locationreminders.reminderslist

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import com.example.android.locationreminder.R
import com.example.android.locationreminder.authentication.AuthenticationActivity
import com.example.android.locationreminder.base.BaseFragment
import com.example.android.locationreminder.base.NavigationCommand
import com.example.android.locationreminder.databinding.FragmentRemindersBinding
import com.example.android.locationreminder.locationreminders.RemindersActivity
import com.example.android.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.example.android.locationreminder.utils.setTitle
import com.example.android.locationreminder.utils.setup
import com.firebase.ui.auth.AuthUI
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
  //use Koin to retrieve the ViewModel instance
  override val _viewModel: RemindersListViewModel by viewModel()
  private lateinit var binding: FragmentRemindersBinding
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    binding =
      DataBindingUtil.inflate(
        inflater,
        R.layout.fragment_reminders, container, false
      )
    binding.viewModel = _viewModel

    setHasOptionsMenu(true)
    setDisplayHomeAsUpEnabled(false)
    setTitle(getString(R.string.app_name))

    binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.lifecycleOwner = this
    setupRecyclerView()
    binding.addReminderFAB.setOnClickListener {
      navigateToAddReminder()
    }
  }

  override fun onResume() {
    super.onResume()
    //load the reminders list on the ui
    _viewModel.loadReminders()
  }

  private fun navigateToAddReminder() {
    //use the navigationCommand live data to navigate between the fragments
    _viewModel.navigationCommand.postValue(
      NavigationCommand.To(
        ReminderListFragmentDirections.toSaveReminder()
      )
    )
  }

  private fun setupRecyclerView() {
    val adapter = RemindersListAdapter {
    }
    binding.remindersRecyclerView.setup(adapter)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.logout -> {
        AuthUI.getInstance().signOut(requireContext())
          .addOnCompleteListener {
            val intent = Intent(this.context, AuthenticationActivity::class.java) // New activity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
          }
//                TODO: add the logout implementation
      }
    }
    return super.onOptionsItemSelected(item)

  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
    inflater.inflate(R.menu.main_menu, menu)
  }

}
