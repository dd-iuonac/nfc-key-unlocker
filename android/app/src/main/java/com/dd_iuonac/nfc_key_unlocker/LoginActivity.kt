package com.dd_iuonac.nfc_key_unlocker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.dd_iuonac.nfc_key_unlocker.utils.toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private var verificationId : String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        layoutPhone.visibility = View.VISIBLE
        layoutVerification.visibility = View.GONE

        btnSendVerificationCode.setOnClickListener {sendVerificationCode() }

        btnVerify.setOnClickListener { verifyCode() }
    }

    private fun verifyCode(){
        val code = etVerificationCode.text.toString().trim()

        if(code.isEmpty()){
            etVerificationCode.error = "Code required"
            etVerificationCode.requestFocus()
            return
        }

        verificationId?.let{
            val credential = PhoneAuthProvider.getCredential(it, code)
            addPhoneNumber(credential)
        }
    }

    private fun sendVerificationCode(){
        val phone = etPhone.text.toString().trim()

        if (phone.isEmpty() || phone.length != 10) {
            etPhone.error = "Enter a valid phone"
            etPhone.requestFocus()
            return
        }

        val phoneNumber = '+' + ccp.selectedCountryCode + phone

        PhoneAuthProvider.getInstance()
            .verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                phoneAuthCallbacks
            )


        layoutPhone.visibility = View.GONE
        layoutVerification.visibility = View.VISIBLE
    }


    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(p0)
        }

        override fun onVerificationFailed(p0: FirebaseException) {
            toast(p0.message!!)
        }

        override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(p0, p1)
            verificationId = p0
        }
    }

    private fun addPhoneNumber(phoneAuthCredential: PhoneAuthCredential) {
        FirebaseAuth.getInstance()
            .currentUser?.updatePhoneNumber(phoneAuthCredential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    toast("Phone Added")
                } else {
                    toast(task.exception?.message!!)
                }
            }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()

                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        toast("The verification code entered was invalid")
                    }
                }
            }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
