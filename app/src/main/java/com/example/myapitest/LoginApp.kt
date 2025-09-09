package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import com.example.myapitest.databinding.ActivityLoginAppBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginApp : AppCompatActivity() {
    private lateinit var binding: ActivityLoginAppBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var verification:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configView()
        verifyUserLogged()
    }

    private fun verifyUserLogged() {
        if(FirebaseAuth.getInstance().currentUser!=null){
            startMainAct()
        }
    }

    private fun configView(){
        binding.btnEnviarSMS.setOnClickListener {
            onSendSMS()
        }
        binding.btnVerificar.setOnClickListener {
            onVerifyCode()
        }
        binding.btnGoogle.setOnClickListener {

            onLoginGoogle()
        }
    }
    private fun onLoginGoogle() {
        credentialManager = CredentialManager.create(this)
        auth = FirebaseAuth.getInstance()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginApp
                )
                handleSignIn(result)
            } catch (e: Exception) {
                Toast.makeText(this@LoginApp, "Erro ao obter credencial +${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)

                val tokenString = googleCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(tokenString, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnSuccessListener {
                        startMainAct()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@LoginApp, "Erro ao fazer login com Google", Toast.LENGTH_SHORT).show()
                    }
        } else {
            Toast.makeText(this, "Tipo de credential inválido", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onVerifyCode() {
        val verificationCode = binding.etCode.text.toString()
        val credential = PhoneAuthProvider.getCredential(verification, verificationCode)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {
                startMainAct()
            }
            .addOnFailureListener {
                Toast.makeText(this@LoginApp, "Erro ao fazer login", Toast.LENGTH_SHORT).show()
            }
    }
    private fun onSendSMS() {
        val phoneNum = binding.etSMS.text.toString()
        val auth = FirebaseAuth.getInstance()
        val build = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phoneNum)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                }

                override fun onVerificationFailed(ex: FirebaseException) {
                    Toast.makeText(this@LoginApp, "Erro ao fazer login", Toast.LENGTH_SHORT).show()

                }

                override fun onCodeSent(
                    verification: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verification, token)
                    this@LoginApp.verification = verification
                    Toast.makeText(this@LoginApp, "Codigo enviado por SMS", Toast.LENGTH_SHORT).show()
                    binding.btnVerificar.visibility = View.VISIBLE
                    binding.tilCode.visibility = View.VISIBLE
                    binding.btnEnviarSMS.visibility = View.GONE
                }

            }).build()
        PhoneAuthProvider.verifyPhoneNumber(build)
    }

    private fun startMainAct(){
        startActivity(MainActivity.newIntent(this))
        finish()
    }

    companion object{
        fun newIntent(context: Context) = Intent(context, LoginApp::class.java)
    }
}






