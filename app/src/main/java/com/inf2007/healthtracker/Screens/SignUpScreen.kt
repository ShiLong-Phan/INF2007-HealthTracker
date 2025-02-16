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
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignUpScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var nameErrorMessage by rememberSaveable { mutableStateOf("") }
    var emailErrorMessage by rememberSaveable { mutableStateOf("") }
    var passwordErrorMessage by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
        )
        if (nameErrorMessage.isNotEmpty()) {
            Text(
                text = nameErrorMessage,
                color = Color.Red,
            )
        }
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
        )
        if (emailErrorMessage.isNotEmpty()) {
            Text(
                text = emailErrorMessage,
                color = Color.Red,
            )
        }
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
        )
        if (passwordErrorMessage.isNotEmpty()) {
            Text(
                text = passwordErrorMessage,
                color = Color.Red,
            )
        }
        Button(
            onClick = {
                nameErrorMessage = if (name.isEmpty()) "Name cannot be empty" else ""
                emailErrorMessage = if (email.isEmpty()) "Email cannot be empty" else ""
                passwordErrorMessage = if (password.isEmpty()) "Password cannot be empty" else ""

                if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = task.result?.user
                                val reward = (1..100).random().toFloat()
                                val userData = hashMapOf(
                                    "name" to name,
                                    "uid" to user?.uid,
                                    "email" to email,
                                )
                                FirebaseFirestore.getInstance().collection("users")
                                    .document(user?.uid ?: "")
                                    .set(userData)
                                    .addOnSuccessListener {
                                        navController.navigate("login_screen")
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(context, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            },
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Sign Up")
            }
        }
    }
}