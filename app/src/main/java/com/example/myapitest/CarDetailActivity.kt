package com.example.myapitest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarDetailActivity: AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailBinding
    private lateinit var car: Car
    private lateinit var mMap: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadCar()
        setupGoogleMaps()
    }

    override fun onMapReady(loc: GoogleMap){
        mMap = loc
        if (::car.isInitialized){
            loadCarOnMap()
        }

    }

    private fun loadCarOnMap() {
        car.place.apply {
            val latLong = LatLng(lat,long)
            mMap.addMarker(
                MarkerOptions()
                        .position(latLong)
                        .title(car.name)
            )
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(latLong, 15f)
            )
        }

    }

    private fun setupGoogleMaps() {
        val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }

    private fun loadCar() {
       val carId = intent.getStringExtra(ARG_ID) ?:""

        CoroutineScope(Dispatchers.IO).launch{
            val result = safeApiCall { RetrofitClient.apiService.getCar(carId) }

            withContext(Dispatchers.Main){
                when(result){
                   is com.example.myapitest.service.Result.Success-> {
                       car = result.data.value
                       handleSuccess()
                   }
                    is com.example.myapitest.service.Result.Error -> {

                    }
                }
            }
        }

    }

    private fun handleSuccess() {
        binding.tvModelo.text = car.name +" / " +car.licence
        binding.year.text = car.year

        val url = car.imageUrl
        if (url.isNullOrEmpty()) {
            binding.imageUrl.setImageResource(R.drawable.img_no_car_foreground)
        } else {
            binding.imageUrl.loadUrl(url)
        }
        loadCarOnMap()
    }
    private fun handleError() {

    }

    companion object{
        private const val ARG_ID = "arg_id"
        fun newIntent(
            context: Context,
            carId: String
        ) = Intent(context, CarDetailActivity::class.java).apply { putExtra(ARG_ID, carId) }
    }
}
