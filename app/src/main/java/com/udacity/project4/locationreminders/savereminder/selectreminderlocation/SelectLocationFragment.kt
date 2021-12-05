package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

  //Use Koin to get the view model of the SaveReminder
  private val TAG = SelectLocationFragment::class.java.simpleName
  override val _viewModel: SaveReminderViewModel by inject()
  private lateinit var binding: FragmentSelectLocationBinding
  private lateinit var map: GoogleMap
  private val REQUEST_LOCATION_PERMISSION = 1
  private val DEFAULT_ZOOM = 15.0f
  private var poi: PointOfInterest? = null
  private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
    android.os.Build.VERSION_CODES.Q

  private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
  ): View? {
    binding =
      DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
    binding.viewModel = _viewModel
    binding.lifecycleOwner = this
    fusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(this.requireActivity())

    setHasOptionsMenu(true)
    setDisplayHomeAsUpEnabled(true)

    val mapFragment = childFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected

    binding.saveLocation.setOnClickListener {
      poi?.let {
        onLocationSelected()
      } ?: Toast.makeText(context, context?.getString(R.string.select_poi), Toast.LENGTH_SHORT)
        .show()
    }

//        TODO: call this function after the user confirms on the selected location
    return binding.root
  }

  private fun onLocationSelected() {
    poi?.let {
      _viewModel.latitude.value = it.latLng.latitude
      _viewModel.longitude.value = it.latLng.longitude
      _viewModel.selectedPOI.value = it
      _viewModel.reminderSelectedLocationStr.value = it.name
      _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }
    //        TODO: When the user confirms on the selected location,
    //         send back the selected location details to the view model
    //         and navigate back to the previous fragment to save the reminder and add the geofence
  }


  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.map_options, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
    // TODO: Change the map type based on the user's selection.
    R.id.normal_map -> {
      map.mapType = GoogleMap.MAP_TYPE_NORMAL
      true
    }
    R.id.hybrid_map -> {
      map.mapType = GoogleMap.MAP_TYPE_HYBRID
      true
    }
    R.id.satellite_map -> {
      map.mapType = GoogleMap.MAP_TYPE_SATELLITE
      true
    }
    R.id.terrain_map -> {
      map.mapType = GoogleMap.MAP_TYPE_TERRAIN
      true
    }
    else -> super.onOptionsItemSelected(item)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap
    setPoiClick(map)
    setMapLongClick(map)
    setMapStyle(map)
    checkPermissions()
  }

  private fun setPoiClick(map: GoogleMap) {
    map.setOnPoiClickListener { poiSel ->
      poi = poiSel
      val poiMarker = map.addMarker(
        MarkerOptions()
          .position(poiSel.latLng)
          .title(poiSel.name)
      )
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(poiSel.latLng, DEFAULT_ZOOM))
      poiMarker.showInfoWindow()
    }
  }

  private fun setMapLongClick(map: GoogleMap) {
    map.setOnMapLongClickListener { latLng ->
      map.addMarker(
        MarkerOptions()
          .position(latLng)
          .title(getString(R.string.dropped_pin))
      )
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
      poi = PointOfInterest(latLng, null, getString(R.string.dropped_pin))
    }
  }

  private fun setMapStyle(map: GoogleMap) {
    try {
      // Customize the styling of the base map using a JSON object defined
      // in a raw resource file.
      val success = map.setMapStyle(
        MapStyleOptions.loadRawResourceStyle(
          this.requireContext(),
          R.raw.map_style
        )
      )
      if (!success) {
        Log.e(TAG, "Style parsing failed.")
      }
    } catch (e: Resources.NotFoundException) {
      Log.e(TAG, "Can't find style. Error: ", e)
    }
  }


  /*
   * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
   * the background permission as well.
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
  ) {
    Log.d(TAG, "onRequestPermissionResult")

    if (
      grantResults.isEmpty() ||
      grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
      (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
        PackageManager.PERMISSION_DENIED)
    ) {
      Snackbar.make(
        this.requireView(),
        R.string.permission_denied_explanation,
        Snackbar.LENGTH_INDEFINITE
      )
        .setAction(R.string.settings) {
          startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          })
        }.show()
    } else {
      checkDeviceLocationSettings()
    }
    getDeviceLocation()
  }

  private fun checkPermissions() {
    if (foregroundAndBackgroundLocationPermissionApproved()) {
      checkDeviceLocationSettings()
    } else {
      requestForegroundAndBackgroundLocationPermissions()
    }
    getDeviceLocation()
  }

  @SuppressLint("MissingPermission")
  private fun getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
    try {
      if (foregroundAndBackgroundLocationPermissionApproved()) {
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener { task ->
          if (task.isSuccessful) {
            // Set the map's camera position to the current location of the device.
            val lastKnownLocation = task.result
            Log.i(TAG,
                  "Current location is ${lastKnownLocation?.latitude} ${lastKnownLocation?.longitude}")
            if (lastKnownLocation != null) {
              map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(lastKnownLocation.latitude,
                       lastKnownLocation.longitude), DEFAULT_ZOOM))
            }
          } else {
            Log.d(TAG, "Current location is null. Using defaults.")
            Log.e(TAG, "Exception: %s", task.exception)
            map.moveCamera(CameraUpdateFactory
                             .newLatLngZoom(LatLng(37.4, -122.0), DEFAULT_ZOOM))
            false.also { map.uiSettings?.isMyLocationButtonEnabled = it }
          }
        }
      }
    } catch (e: SecurityException) {
      Log.e("Exception: %s", e.message, e)
    }
  }

  /*
   *  Uses the Location Client to check the current state of location settings, and gives the user
   *  the opportunity to turn on location services within our app.
   */
  private fun checkDeviceLocationSettings(resolve: Boolean = true) {
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
          exception.startResolutionForResult(this.requireActivity(),
                                             REQUEST_TURN_DEVICE_LOCATION_ON)
        } catch (sendEx: IntentSender.SendIntentException) {
          Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
        }
      } else {
        Snackbar.make(
          this.requireView(),
          R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
        ).setAction(android.R.string.ok) {
          checkDeviceLocationSettings()
        }.show()
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
    requestPermissions(permissionsArray, resultCode)
  }

}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
