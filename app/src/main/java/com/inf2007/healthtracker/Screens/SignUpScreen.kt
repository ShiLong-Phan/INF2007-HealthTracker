package com.inf2007.healthtracker.Screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns
import androidx.compose.foundation.layout.Box

@Composable
fun SignUpScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                signUpUser(
                    username,
                    email,
                    password,
                    navController,
                    setErrorMessage = { errorMessage = it })
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun signUpUser(
    username: String,
    email: String,
    password: String,
    navController: NavController,
    setErrorMessage: (String?) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users")
        .whereEqualTo("email", email)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                setErrorMessage("User already registered")
            } else {
                val user = hashMapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password
                )
                db.collection("users")
                    .add(user)
                    .addOnSuccessListener {
                        setErrorMessage(null)
                        navController.navigate("login_screen")
                    }
                    .addOnFailureListener { exception ->
                        setErrorMessage("Error: ${exception.message}")
                    }
            }
        }
        .addOnFailureListener { exception ->
            setErrorMessage("Error: ${exception.message}")
        }
}