package com.example.android.locationreminder.locationreminders.reminderslist

import com.example.android.locationreminder.R
import com.example.android.locationreminder.base.BaseRecyclerViewAdapter
import com.example.android.locationreminder.locationreminders.reminderslist.ReminderDataItem


//Use data binding to show the reminder on the item
class RemindersListAdapter(callBack: (selectedReminder: ReminderDataItem) -> Unit) :
  BaseRecyclerViewAdapter<ReminderDataItem>(callBack) {
  override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}