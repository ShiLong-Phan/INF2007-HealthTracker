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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.inf2007.healthtracker.ui.theme.Primary
import com.inf2007.healthtracker.ui.theme.Tertiary
import com.inf2007.healthtracker.ui.theme.Unfocused

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

    val roundedShape = MaterialTheme.shapes.small

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Sign Up".uppercase(),
            color = Primary,
            style = MaterialTheme.typography.headlineLarge.copy(
                letterSpacing = 2.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Name Text Field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Person Icon"
                )
            },
            shape = roundedShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Unfocused
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (nameErrorMessage.isNotEmpty()) {
            Text(
                text = nameErrorMessage,
                color = Color.Red,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email Text Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Icon"
                )
            },
            shape = roundedShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Unfocused
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (emailErrorMessage.isNotEmpty()) {
            Text(
                text = emailErrorMessage,
                color = Color.Red,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password Text Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password Icon"
                )
            },
            shape = roundedShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Unfocused
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (passwordErrorMessage.isNotEmpty()) {
            Text(
                text = passwordErrorMessage,
                color = Color.Red,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                                val userData = hashMapOf(
                                    "name" to name,
                                    "uid" to user?.uid,
                                    "password" to password,
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
            shape = roundedShape,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Have an account? ")
            Text(
                text = "Login",
                color = Tertiary,
                modifier = Modifier.clickable { navController.navigate("login_screen") },
                fontWeight = FontWeight.Bold
            )
        }
    }
}