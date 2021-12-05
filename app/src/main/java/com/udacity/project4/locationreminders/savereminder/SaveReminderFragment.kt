package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
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
  private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
    android.os.Build.VERSION_CODES.Q

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
      val latitude = _viewModel.latitude.value
      val longitude = _viewModel.longitude.value
      if (latitude != null && longitude != null && !title.isNullOrEmpty() && !description.isNullOrEmpty()) {
        checkPermissionsAndStartGeofencing()
      }

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
    }
  }

  /*
*  When we get the result from asking the user to turn on device location, we call
*  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
*  we don't resolve the check to keep the user from seeing an endless loop.
*/
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
      checkDeviceLocationSettingsAndStartGeofence()
    }
  }

  /**
   * Starts the permission check and Geofence process only if the Geofence associated with the
   * current hint isn't yet active.
   */
  private fun checkPermissionsAndStartGeofencing() {
    if (foregroundAndBackgroundLocationPermissionApproved()) {
      checkDeviceLocationSettingsAndStartGeofence()
    } else {
      requestForegroundAndBackgroundLocationPermissions()
    }
  }

  /*
   *  Uses the Location Client to check the current state of location settings, and gives the user
   *  the opportunity to turn on location services within our app.
   */
  private fun checkDeviceLocationSettingsAndStartGeofence(
    resolve: Boolean = true,
  ) {
    val locationRequest = LocationRequest.create().apply {
      priority = LocationRequest.PRIORITY_LOW_POWER
    }
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val settingsClient = LocationServices.getSettingsClient(this.requireActivity())
    val locationSettingsResponseTask =
      settingsClient.checkLocationSettings(builder.build())
    locationSettingsResponseTask.addOnFailureListener { exception ->
      if (exception is ResolvableApiException && resolve) {
        try {
          val intentSender = exception.resolution.intentSender
          startIntentSenderForResult(intentSender,
                                     REQUEST_TURN_DEVICE_LOCATION_ON,
                                     null,
                                     0,
                                     0,
                                     0,
                                     null)
        } catch (sendEx: IntentSender.SendIntentException) {
          Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
        }
      } else {
        Snackbar.make(
          this.requireView(),
          R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
        ).setAction(android.R.string.ok) {
          checkDeviceLocationSettingsAndStartGeofence(resolve)
        }.show()
      }
    }
    locationSettingsResponseTask.addOnCompleteListener {
      if (it.isSuccessful) {
        createGeofenceRequest()
      }
    }
  }

  /*
   *  Determines whether the app has the appropriate permissions across Android 10+ and all other
   *  Android versions.
   */
  @TargetApi(29)
  private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
    val foregroundLocationApproved = (
      PackageManager.PERMISSION_GRANTED ==
        ActivityCompat.checkSelfPermission(this.requireContext(),
                                           Manifest.permission.ACCESS_FINE_LOCATION))
    val backgroundPermissionApproved =
      if (runningQOrLater) {
        PackageManager.PERMISSION_GRANTED ==
          ActivityCompat.checkSelfPermission(
            this.requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
          )
      } else {
        true
      }
    return foregroundLocationApproved && backgroundPermissionApproved
  }

  /*
   *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
   */
  @TargetApi(29)
  private fun requestForegroundAndBackgroundLocationPermissions() {
    if (foregroundAndBackgroundLocationPermissionApproved())
      return
    var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val resultCode = when {
      runningQOrLater -> {
        permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
      }
      else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
    }
    Log.d(TAG, "Request foreground only location permission")
    ActivityCompat.requestPermissions(
      this.requireActivity(),
      permissionsArray,
      resultCode
    )
  }

  @SuppressLint("MissingPermission")
  private fun createGeofenceRequest() {
    val reminder = ReminderDataItem(_viewModel.reminderTitle.value,
                                    _viewModel.reminderDescription.value,
                                    _viewModel.reminderSelectedLocationStr.value,
                                    _viewModel.latitude.value,
                                    _viewModel.longitude.value)
    val geofence = Geofence.Builder()
      .setRequestId(reminder.id)
      .setCircularRegion(reminder.latitude!!,
                         reminder.longitude!!,
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
        Toast.makeText(requireContext(),
                       "Geofences Added",
                       Toast.LENGTH_SHORT).show()
        _viewModel.validateAndSaveReminder(reminder)
      }
      addOnFailureListener {
        if ((it.message != null)) {
          Log.w(TAG, it.message!!)
        }
        Toast.makeText(requireContext(),
                       "Failed to add location!!! Try again later!",
                       Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    //make sure to clear the view model after destroy, as it's a single view model.
    _viewModel.onClear()
  }

}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
