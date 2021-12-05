package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
  //Get the view model this time as a single to be shared with the another fragment
  private val TAG = SaveReminderFragment::class.java.toString()
  override val _viewModel: SaveReminderViewModel by inject()
  private lateinit var binding: FragmentSaveReminderBinding
  private lateinit var geofencingClient: GeofencingClient
  private val geofencePendingIntent: PendingIntent by lazy {
    val intent = Intent(this.requireContext(), GeofenceBroadcastReceiver::class.java)
    PendingIntent.getBroadcast(this.requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    binding =
      DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

    setDisplayHomeAsUpEnabled(true)

    binding.viewModel = _viewModel
    geofencingClient = LocationServices.getGeofencingClient(this.requireActivity())

    return binding.root
  }

  @SuppressLint("MissingPermission")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.lifecycleOwner = this
    binding.selectLocation.setOnClickListener {
      //            Navigate to another fragment to get the user location
      _viewModel.navigationCommand.value =
        NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    }

    binding.saveReminder.setOnClickListener {
      val title = _viewModel.reminderTitle.value
      val description = _viewModel.reminderDescription.value
      val location = _viewModel.reminderSelectedLocationStr.value
      val latitude = _viewModel.latitude.value
      val longitude = _viewModel.longitude.value
      _viewModel.validateAndSaveReminder(ReminderDataItem(title,
                                                          description,
                                                          location,
                                                          latitude,
                                                          longitude))
      if (latitude != null && longitude != null && !title.isNullOrEmpty() && !description.isNullOrEmpty()) {
        createGeofenceRequest(latitude, longitude)
      }

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
    }
  }

  @SuppressLint("MissingPermission")
  private fun createGeofenceRequest(latitude: Double, longitude: Double) {
    val id = UUID.randomUUID().toString()
    val geofence = Geofence.Builder()
      .setRequestId(id)
      .setCircularRegion(latitude,
                         longitude,
                         GEOFENCE_RADIUS_IN_METERS
      )
      .setExpirationDuration(Geofence.NEVER_EXPIRE)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
      .build()

    val geofencingRequest = GeofencingRequest.Builder()
      .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
      .addGeofence(geofence)
      .build()
    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
      addOnSuccessListener {
        Log.d(TAG, "Geofence Added")
      }
      addOnFailureListener {
        if ((it.message != null)) {
          Log.w(TAG, it.message!!)
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    //make sure to clear the view model after destroy, as it's a single view model.
    _viewModel.onClear()
  }

}
