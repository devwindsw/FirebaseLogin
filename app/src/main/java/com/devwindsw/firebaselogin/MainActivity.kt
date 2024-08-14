package com.devwindsw.firebaselogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.devwindsw.firebaselogin.ui.theme.FirebaseLoginTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebaseLoginTheme {
                SignIn { email: String, passwd: String -> onClick(email, passwd) }
            }
        }
    }

    private fun onClick(email: String, passwd: String) {
        println("E-mail address is ${email} and password is ${passwd}.")
    }
}

@Composable
fun SignIn(onClick: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(value = false) }

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
