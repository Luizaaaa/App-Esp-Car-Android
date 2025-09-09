package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityNewCarBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.example.myapitest.model.Car
import com.example.myapitest.model.Place
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class NewCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewCarBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var imageUri: Uri
    private var selectedMarker: Marker?=null

    private var imageFile:File?=null
    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode== Activity.RESULT_OK){
            imageFile?.let{
                uploadImgToFirebase()
            }

        }
    }

    private fun uploadImgToFirebase(){
        val storageRef = FirebaseStorage.getInstance().reference

        val imagesRef = storageRef.child("image/${UUID.randomUUID()}.jgp")
        val baos = ByteArrayOutputStream()
        val imgBitmap = BitmapFactory.decodeFile(imageFile!!.path)
        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        progressImgBar(true)
        imagesRef.putBytes(data)
            .addOnCompleteListener {
                progressImgBar(false)
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao carregar imagem no Firebase!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnSuccessListener {
                imagesRef.downloadUrl.addOnSuccessListener { uri->
                    binding.imgCarPreview.setImageURI(imageUri)
                    binding.edtUrlImagem.setText(uri.toString())
                }

            }
    }

    private fun progressImgBar(isLoading: Boolean){
        binding.loadImageProgress.isVisible= isLoading
        binding.takePicture.isEnabled = !isLoading
        binding.saveCta.isEnabled = !isLoading
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setupGoogleMaps()
        binding.saveCta.setOnClickListener {
            saveCar()
        }
        binding.takePicture.setOnClickListener {
            onTakePicture()
        }
    }

    private fun onTakePicture(){
        if(checkSelfPermission(this, CAMERA)==PERMISSION_GRANTED){
            openCamera()
        }else{
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(CAMERA),
            REQUEST_CODE_CAM)


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE_CAM->{
                if(grantResults.isNotEmpty()&&grantResults[0]==PERMISSION_GRANTED){
                    openCamera()
                }
                else{
                    Toast.makeText(
                        this,
                        "Permissão de camera negada!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val time:String = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val imageFileName = "Foto_$time"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File.createTempFile(imageFileName,".jpg", storageDir)
        return FileProvider.getUriForFile(this,"com.example.myapitest.fileprovider",imageFile!!)
    }

    private fun saveCar(){
        if(!validateForm()) return
        loadImg()
    }

    private fun loadImg(){
        val modelo = binding.edtModelo.text.toString()
        val placa = binding.edtPlaca.text.toString()
        val ano = binding.edtAno.text.toString()
        val imageurl = binding.edtUrlImagem.text.toString()
        val loc = selectedMarker?.position?.let {loc->
            Place(loc.latitude, loc.longitude)
        }?: throw IllegalArgumentException("precisa selecionar a localização!")
        CoroutineScope(Dispatchers.IO).launch {
            val carId = Car(
                SecureRandom().nextInt().toString(),
                imageurl,
                ano,
                modelo,
                placa,
                loc
            )
            val save = safeApiCall { RetrofitClient.apiService.addCar(carId) }
            withContext(Dispatchers.Main){
                when(save){
                    is Result.Error ->{
                        Toast.makeText(this@NewCarActivity, "Erro ao salvar", Toast.LENGTH_SHORT ).show()
                    }
                    is Result.Success ->{
                        Toast.makeText(this@NewCarActivity, "Salvo com sucesso", Toast.LENGTH_SHORT ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun validateField(field: EditText, label: String): Boolean {
        if (field.text.toString().isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.error_form, label),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun validateLoc(mark: Marker?): Boolean{
        if (mark==null){
            Toast.makeText(
                this,
                getString(R.string.error_form_loc),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun validateForm(): Boolean {
        return validateField(binding.edtUrlImagem, "Modelo")&&
                validateField(binding.edtModelo, "Modelo") &&
                validateField(binding.edtPlaca, "Placa") &&
                validateField(binding.edtAno, "Ano") &&
                validateLoc(selectedMarker)

    }

    private fun setupGoogleMaps() {
        val fragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        fragment.getMapAsync(this)

    }

    override fun onMapReady(loc: GoogleMap) {
        mMap=loc
        mMap.setOnMapClickListener { latLong ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLong)
                    .draggable(true)
                    .title("Lat: ${latLong.latitude} Long: ${latLong.longitude}" )
            )
        }
        getDeviceLocation()
    }

    private fun getDeviceLocation() {
        if(checkSelfPermission(this, ACCESS_FINE_LOCATION)==PERMISSION_GRANTED){
            loadCurrentLocation()
        }else{

        }
    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation() {
        mMap.isMyLocationEnabled=true
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { loc->
            val current = LatLng(loc.latitude, loc.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15f))
        }
    }

    companion object{
        const val REQUEST_CODE_CAM = 100
        fun newIntent(ctx: Context) = Intent(ctx, NewCarActivity::class.java)
    }
}