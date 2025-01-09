package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petcare.utils.LogoutUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewPetActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var breedNameEditText: EditText
    private lateinit var bloodTypeEditText: EditText
    private lateinit var descriptionEditText: EditText

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_viewpet)

        nameEditText = findViewById(R.id.nameEditText)
        breedNameEditText = findViewById(R.id.breedNameEditText)
        bloodTypeEditText = findViewById(R.id.bloodEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)

        val petId = intent.getStringExtra("PET_ID")
        if (petId != null) {
            fetchPetData(petId)
        } else {
            Toast.makeText(this, "Pet ID not found", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation bar setup
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewPetLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchPetData(petId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).collection("pets").document(petId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        nameEditText.setText(document.getString("name"))
                        breedNameEditText.setText(document.getString("breed"))
                        bloodTypeEditText.setText(document.getString("bloodType"))
                        descriptionEditText.setText(document.getString("description"))
                    } else {
                        Toast.makeText(this, "No such pet found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching pet data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}