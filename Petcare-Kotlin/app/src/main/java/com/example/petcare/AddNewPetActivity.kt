package com.example.petapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petcare.HomeActivity
import com.example.petcare.NearByDoctorsActivity
import com.example.petcare.R
import com.example.petcare.utils.LogoutUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddNewPetActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var breedNameEditText: EditText
    private lateinit var bloodEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var addButton: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()  // FirebaseAuth instance for user authentication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addnewpet)

        // Bottom Navigation bar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_nearby -> {
                    startActivity(Intent(applicationContext, NearByDoctorsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_logout -> {
                    LogoutUtils.showLogoutDialog(this)
                    true
                }
                else -> false
            }
        }

        nameEditText = findViewById(R.id.nameEditText)
        breedNameEditText = findViewById(R.id.breedNameEditText)
        bloodEditText = findViewById(R.id.bloodEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        addButton = findViewById(R.id.addButton)

        addButton.setOnClickListener { addPetToFirebase() }
    }

    private fun addPetToFirebase() {
        val name = nameEditText.text.toString().trim()
        val breed = breedNameEditText.text.toString().trim()
        val bloodType = bloodEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        if (name.isEmpty() || breed.isEmpty() || bloodType.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current logged-in user ID
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val petId = UUID.randomUUID().toString()
        val petData = hashMapOf<String, Any>(
            "name" to name,
            "breed" to breed,
            "bloodType" to bloodType,
            "description" to description
        )

        savePetData(userId, petId, petData)
    }

    private fun savePetData(userId: String, petId: String, petData: HashMap<String, Any>) {
        // Save the pet data under the user ID in Firestore
        firestore.collection("users").document(userId).collection("pets").document(petId)
            .set(petData)
            .addOnSuccessListener {
                Toast.makeText(this, "Pet added successfully!", Toast.LENGTH_SHORT).show()
                finish()  // Return to home page after successful save.
            }
            .addOnFailureListener { e ->
                showError("Failed to save pet data: ${e.message}")
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }




}
