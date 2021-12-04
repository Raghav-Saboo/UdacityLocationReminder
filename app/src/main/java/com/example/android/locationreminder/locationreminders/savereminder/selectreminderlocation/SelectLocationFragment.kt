package com.example.android.locationreminder.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.example.android.locationreminder.R;
import com.example.android.locationreminder.base.BaseFragment
import com.example.android.locationreminder.base.NavigationCommand
import com.example.android.locationreminder.databinding.FragmentSelectLocationBinding
import com.example.android.locationreminder.locationreminders.savereminder.SaveReminderFragmentDirections
import com.example.android.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.example.android.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import java.util.Locale
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

  //Use Koin to get the view model of the SaveReminder
  private val TAG = SelectLocationFragment::class.java.simpleName
  override val _viewModel: SaveReminderViewModel by inject()
  private lateinit var binding: FragmentSelectLocationBinding
  private lateinit var map: GoogleMap
  private val REQUEST_LOCATION_PERMISSION = 1
  private val DEFAULT_ZOOM = 15.0f

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
  ): View? {
    binding =
      DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

    binding.viewModel = _viewModel
    binding.lifecycleOwner = this

    setHasOptionsMenu(true)
    setDisplayHomeAsUpEnabled(true)

    val mapFragment = childFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
    return binding.root
  }

  private fun onLocationSelected(poi: PointOfInterest) {
    _viewModel.latitude.value = poi.latLng.latitude
    _viewModel.longitude.value = poi.latLng.longitude
    _viewModel.selectedPOI.value = poi
    _viewModel.reminderSelectedLocationStr.value = poi.name
    NavigationCommand.To(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
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

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    // Check if location permissions are granted and if so enable the
    // location data layer.
    if (requestCode == REQUEST_LOCATION_PERMISSION) {
      if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
        enableMyLocation()
      }
    }
  }

  private fun isPermissionGranted(): Boolean {
    return (ActivityCompat.checkSelfPermission(this.requireContext(),
                                               Manifest.permission.ACCESS_FINE_LOCATION)
      == PackageManager.PERMISSION_GRANTED &&
      ActivityCompat.checkSelfPermission(this.requireContext(),
                                         Manifest.permission.ACCESS_COARSE_LOCATION)
      == PackageManager.PERMISSION_GRANTED)
  }

  @SuppressLint("MissingPermission")
  private fun enableMyLocation() {
    if (isPermissionGranted()) {
      map.isMyLocationEnabled = true
    } else {
      ActivityCompat.requestPermissions(
        this.requireActivity(),
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
        REQUEST_LOCATION_PERMISSION
      )
    }
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap
    setPoiClick(map)
    setMapStyle(map)
    enableMyLocation()
  }

  private fun setPoiClick(map: GoogleMap) {
    map.setOnPoiClickListener { poi ->
      val poiMarker = map.addMarker(
        MarkerOptions()
          .position(poi.latLng)
          .title(poi.name)
      )
      poiMarker.showInfoWindow()
      onLocationSelected(poi)
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

}
