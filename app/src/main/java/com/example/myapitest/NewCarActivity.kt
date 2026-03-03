package com.example.myapitest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapitest.databinding.ActivityNewCarBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.Place
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NewCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewCarBinding

    private lateinit var mMap: GoogleMap
    private var selectedMarker: Marker? = null

    private lateinit var imageUri: Uri
    private var imageFile: File? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            binding.imageUrl.setText(R.string.image_captured)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        setupGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        binding.mapContent.visibility = View.VISIBLE
        requestLocationPermission()

        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Lat: ${latLng.latitude}, Lng: ${latLng.longitude}")
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation()
                } else {
                    Toast.makeText(
                        this,
                        R.string.location_permission_denied,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                }
            }
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.takePictureCta.setOnClickListener {
            takePicture()
        }

        binding.saveCta.setOnClickListener {
            saveCar()
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun requestLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLong = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, 15f))
            }
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
    }

    private fun saveCar() {
        if (!validateForm()) return
        uploadImageToFirebase()
    }

    private fun validateForm(): Boolean {
        var hasError = false

        if (binding.carId.text.isNullOrBlank()) {
            binding.carId.error = getString(R.string.required_field)
            hasError = true
        }

        if (binding.name.text.isNullOrBlank()) {
            binding.name.error = getString(R.string.required_field)
            hasError = true
        }

        if (binding.year.text.isNullOrBlank()) {
            binding.year.error = getString(R.string.required_field)
            hasError = true
        }

        if (binding.licence.text.isNullOrBlank()) {
            binding.licence.error = getString(R.string.required_field)
            hasError = true
        }

        if (selectedMarker == null) {
            Toast.makeText(this, R.string.required_marker, Toast.LENGTH_SHORT).show()
            hasError = true
        }

        if (imageFile == null) {
            Toast.makeText(this, R.string.required_photo, Toast.LENGTH_SHORT).show()
            hasError = true
        }

        return !hasError
    }

    private fun uploadImageToFirebase() {
        val file = imageFile ?: return

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        val imageBitmap = BitmapFactory.decodeFile(file.path)

        if (imageBitmap == null) {
            Toast.makeText(this, R.string.error_upload_image, Toast.LENGTH_LONG).show()
            return
        }

        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        onLoadImage(true)

        imageRef.putBytes(data)
            .addOnFailureListener {
                onLoadImage(false)
                Toast.makeText(this, R.string.error_upload_image, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        binding.imageUrl.setText(uri.toString())
                        saveData(uri.toString())
                    }
                    .addOnFailureListener {
                        onLoadImage(false)
                        Toast.makeText(this, R.string.error_upload_image, Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun saveData(imageUrl: String) {
        val markerPosition = selectedMarker?.position ?: return

        val car = Car(
            id = binding.carId.text.toString(),
            imageUrl = imageUrl,
            year = binding.year.text.toString(),
            name = binding.name.text.toString(),
            licence = binding.licence.text.toString(),
            place = Place(
                lat = markerPosition.latitude,
                long = markerPosition.longitude
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.addCar(car) }

            withContext(Dispatchers.Main) {
                onLoadImage(false)
                when (result) {
                    is Result.Success -> handleOnSuccess()
                    is Result.Error -> handleOnError()
                }
            }
        }
    }

    private fun onLoadImage(isLoading: Boolean) {
        binding.loadImageProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.takePictureCta.isEnabled = !isLoading
        binding.saveCta.isEnabled = !isLoading
    }

    private fun handleOnSuccess() {
        Toast.makeText(this, R.string.success_add_car, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleOnError() {
        Toast.makeText(this, R.string.error_add_car, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002

        fun newIntent(context: Context): Intent {
            return Intent(context, NewCarActivity::class.java)
        }
    }
}
