package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapitest.adapter.CarAdapter
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    override fun onResume() {
        super.onResume()
        if (!verifyLoggedUser()) return
        fetchCars()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_loggout -> {
                onLoggout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun verifyLoggedUser(): Boolean {
        if (FirebaseAuth.getInstance().currentUser == null) {
            navigateToLogin()
            return false
        }
        return true
    }

    private fun onLoggout() {
        FirebaseAuth.getInstance().signOut()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = LoginActivity.newIntent(this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun setupView() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchCars()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.addCta.setOnClickListener {
            navigateToNewCar()
        }

        binding.message.setOnClickListener {
            fetchCars()
        }
    }

    private fun navigateToNewCar() {
        startActivity(NewCarActivity.newIntent(this))
    }

    private fun fetchCars() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCars() }

            withContext(Dispatchers.Main) {
                binding.swipeRefreshLayout.isRefreshing = false

                when (result) {
                    is Result.Success -> handleOnSuccess(result.data)
                    is Result.Error -> handleOnError()
                }
            }
        }
    }

    private fun handleOnSuccess(cars: List<Car>) {
        if (cars.isEmpty()) {
            binding.message.visibility = View.VISIBLE
            binding.message.setText(R.string.no_items)
            binding.recyclerView.visibility = View.GONE
            return
        }

        binding.message.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE

        binding.recyclerView.adapter = CarAdapter(cars) { car ->
            startActivity(CarDetailActivity.newIntent(this, car))
        }
    }

    private fun handleOnError() {
        binding.message.visibility = View.VISIBLE
        binding.message.setText(R.string.generical_error)
        binding.recyclerView.visibility = View.GONE
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}
