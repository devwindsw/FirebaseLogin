package com.devwindsw.firebaselogin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
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
                //userViewModel.user.observe(this) { user: FirebaseUser -> updateIdentification(user) }
                SignIn (userViewModel, { email: String, passwd: String -> onClick(email, passwd) })
            }
        }
    }

    private fun onClick(email: String, passwd: String) {
        println("E-mail address is ${email} and password is ${passwd}.")
    }

    public override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.i(TAG, "onStart no user")
            userViewModel.updateUserFailed()
            return
        }
        currentUser.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i(TAG, "onStart task completes successfully.")
                userViewModel.updateUser(currentUser)
            } else {
                userViewModel.updateUserFailed()
            }
        }
    }
}

class UserViewModel : ViewModel() {
    private val userLiveData = MutableLiveData<FirebaseUser>()
    val user: LiveData<FirebaseUser> get() = userLiveData
    private val userFailed = MutableLiveData<Boolean>()
    val failed: LiveData<Boolean> get() = userFailed

    fun updateUser(newUser: FirebaseUser) {
        userLiveData.value = newUser
    }

    fun updateUserFailed() {
        userFailed.value = true
    }
}

@Composable
fun SignIn(viewModel: UserViewModel, onClick: (String, String) -> Unit) {
    val user by viewModel.user.observeAsState()
    val userFailed by viewModel.failed.observeAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    //var showPassword by remember { mutableStateOf(value = false) }

    if (user != null) updateIdentification(user = user)
    else if (userFailed == true) updateIdentification(null)

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
        FilledTonalButton(onClick = { onClick(email, password) }) {
            Text("Sing in")
        }
    }
}

@Composable
fun updateIdentification(user: FirebaseUser?) {
    var userId = "None"
    if (user != null) {
        userId = user.email + if (user.isEmailVerified) " not verified" else " verified" + " (${user.uid})"
    }
    Toast.makeText(LocalContext.current, userId, Toast.LENGTH_LONG).show()
}