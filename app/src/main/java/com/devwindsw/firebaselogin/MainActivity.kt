package com.devwindsw.firebaselogin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devwindsw.firebaselogin.ui.theme.FirebaseLoginTheme
import com.google.common.base.Strings.isNullOrEmpty
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private val userViewModel by viewModels<UserViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        // Initialize Firebase Auth
        auth = Firebase.auth
        enableEdgeToEdge()
        setContent {
            FirebaseLoginTheme {
                SignInView (userViewModel,
                    { email: String, passwd: String -> onSignin(email, passwd) },
                    { email: String, passwd: String -> onJoin(email, passwd) },
                    { onVerify() }, { onSignout() })
            }
        }
    }

    private fun onSignin(email: String, password: String) {
        val msg: String = "E-mail address is ${email} and password is ${password}."
        Toast.makeText(baseContext, msg, Toast.LENGTH_LONG,).show()
        Log.i(TAG, "onSignin " + msg)
        signIn(email, password)
    }


    private fun onJoin(email: String, password: String) {
        val msg: String = "E-mail address is ${email} and password is ${password}."
        Toast.makeText(baseContext, msg, Toast.LENGTH_LONG,).show()
        Log.i(TAG, "onJoin " + msg)
        join(email, password)
    }

    private fun onVerify() {
        Log.i(TAG, "onVerify")
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.i(TAG, "onVerify no user")
            Toast.makeText(baseContext, "No user to verify email.", Toast.LENGTH_SHORT,).show()
            return
        }
        if (currentUser.isEmailVerified) {
            Log.i(TAG, "onVerify already verified")
            Toast.makeText(baseContext, "Already verified email", Toast.LENGTH_LONG)
            return
        }
        verifyEmail(currentUser!!)
    }

    private fun onSignout() {
        Log.i(TAG, "onSignout")
        signOut()
    }

    public override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.i(TAG, "onStart no user")
            return
        }
        currentUser.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i(TAG, "onStart task completes successfully.")
                userViewModel.updateUser(currentUser)
            } else {
                Log.i(TAG, "onStart task fails.")
                userViewModel.updateUserFailed()
            }
            logIdentification(auth.currentUser)
        }
    }

    private fun signIn(email: String, password: String) {
        Log.i(TAG, "signIn:$email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                val currentUser = auth.currentUser
                if (task.isSuccessful && currentUser != null) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    userViewModel.updateUser(currentUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT,).show()
                    //checkForMultiFactorFailure(task.exception!!)
                }
                logIdentification(auth.currentUser)
            }
    }


    private fun signOut() {
        auth.signOut()
        logIdentification(auth.currentUser)
    }

    private fun join(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    userViewModel.updateUser(auth.currentUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT,).show()
                }
                logIdentification(auth.currentUser)
            }
    }

    private fun verifyEmail(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "sendEmailVerification:success")
                } else {
                    Log.e(TAG, "sendEmailVerification", task.exception)
                    Toast.makeText(baseContext, "Failed to send verification email.", Toast.LENGTH_LONG,).show()
                }
            }
    }
}

class UserViewModel : ViewModel() {
    private val userLiveData = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = userLiveData
    private val userFailed = MutableLiveData<Boolean>()
    val failed: LiveData<Boolean> get() = userFailed

    fun updateUser(newUser: FirebaseUser?) {
        Log.i(TAG, "updateUser")
        if (newUser == null) {
            Log.i(TAG, "updateUser null user")
            updateUserFailed()
        }
        userLiveData.value = newUser
    }

    fun updateUserFailed() {
        Log.i(TAG, "updateUserFailed")
        userFailed.value = true
    }
}

@Composable
fun SignInView(viewModel: UserViewModel,
               onSignin: (String, String) -> Unit,
               onJoin: (String, String) -> Unit,
               onVerify: () -> Unit, onSignout: () -> Unit) {
    val user by viewModel.user.observeAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    //var showPassword by remember { mutableStateOf(value = false) }

    Log.i(TAG, "SignInView email=${email}")

    Column {
        OutlinedTextField (
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail Address") },
            placeholder = { Text(text = "Type e-mail address here") },
            shape = RoundedCornerShape(percent = 30),
        )
        OutlinedTextField (
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            placeholder = { Text(text = "Type password here") },
            shape = RoundedCornerShape(percent = 30),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Row {
            FilledTonalButton(onClick = { onSignin(email, password) }) {
                Text("Sing in")
            }
            FilledTonalButton(onClick = { onJoin(email, password) }) {
                Text("Join")
            }
            FilledTonalButton(onClick = { onVerify() }) {
                Text("Verify")
            }
            FilledTonalButton(onClick = { onSignout() }) {
                Text("Sign out")
            }
        }
    }
}

fun logIdentification(user: FirebaseUser?) {
    if (user != null) {
        Log.i(TAG, "logIdentification ${user.email} ${user.uid} " + user.isEmailVerified)
    } else {
        Log.i(TAG, "logIdentification null user")
    }
}