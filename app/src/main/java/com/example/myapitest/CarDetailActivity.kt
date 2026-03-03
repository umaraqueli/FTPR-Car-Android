package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.model.Car
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CarDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailBinding

    private var carName: String = ""
    private var carYear: String = ""
    private var carLicence: String = ""
    private var carImageUrl: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        loadDataFromIntent()
        setupGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val location = LatLng(latitude, longitude)

        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(carName)
        )

        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                location,
                15f
            )
        )
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadDataFromIntent() {
        carName = intent.getStringExtra(ARG_NAME).orEmpty()
        carYear = intent.getStringExtra(ARG_YEAR).orEmpty()
        carLicence = intent.getStringExtra(ARG_LICENCE).orEmpty()
        carImageUrl = intent.getStringExtra(ARG_IMAGE_URL).orEmpty()
        latitude = intent.getDoubleExtra(ARG_LAT, 0.0)
        longitude = intent.getDoubleExtra(ARG_LONG, 0.0)

        binding.name.text = carName
        binding.year.text = getString(R.string.car_label_year, carYear)
        binding.licence.text = getString(R.string.car_label_licence, carLicence)
        binding.image.loadUrl(carImageUrl)
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    companion object {
        private const val ARG_NAME = "arg_name"
        private const val ARG_YEAR = "arg_year"
        private const val ARG_LICENCE = "arg_licence"
        private const val ARG_IMAGE_URL = "arg_image_url"
        private const val ARG_LAT = "arg_lat"
        private const val ARG_LONG = "arg_long"

        fun newIntent(context: Context, car: Car): Intent {
            return Intent(context, CarDetailActivity::class.java).apply {
                putExtra(ARG_NAME, car.name)
                putExtra(ARG_YEAR, car.year)
                putExtra(ARG_LICENCE, car.licence)
                putExtra(ARG_IMAGE_URL, car.imageUrl)
                putExtra(ARG_LAT, car.place.lat)
                putExtra(ARG_LONG, car.place.long)
            }
        }
    }
}
