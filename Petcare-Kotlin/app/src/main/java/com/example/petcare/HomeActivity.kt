package com.example.petcare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petapp.AddNewPetActivity
import com.example.petcare.utils.LogoutUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage

class HomeActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var noPetsTextView: TextView
    private lateinit var gridLayout: GridLayout

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val SELECT_PHOTOS_REQUEST = 1
    private var selectedPetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Bottom Navigation bar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> true
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

        titleTextView = findViewById(R.id.titleTextView)
        noPetsTextView = findViewById(R.id.noPetsTextView)
        gridLayout = findViewById(R.id.gridLayout)

        // Ensure the user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch the pets for the current user
        fetchUserPets(currentUser.uid)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileImageView = findViewById<ImageView>(R.id.profileImageView)
        profileImageView.setOnClickListener {
            // Navigate to EditUserActivity
            val intent = Intent(this, EdituserActivity::class.java)
            startActivity(intent)
        }

        val addPetButton = findViewById<Button>(R.id.addPetButton)
        addPetButton.setOnClickListener {
            // Navigate to AddPetActivity
            val intent = Intent(this, AddNewPetActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHOTOS_REQUEST && resultCode == RESULT_OK) {
            data?.clipData?.let { clipData ->
                val uris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
                selectedPetId?.let { petId ->
                    uploadImages(petId, uris)
                }
            } ?: data?.data?.let { uri ->
                selectedPetId?.let { petId ->
                    uploadImages(petId, listOf(uri))
                }
            }
        }
    }

    private fun fetchUserPets(userId: String) {
        firestore.collection("users").document(userId).collection("pets")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("HomeActivity", "No pets found for user: $userId")
                    noPetsTextView.visibility = TextView.VISIBLE
                } else {
                    Log.d("HomeActivity", "Pets found: ${result.size()}")
                    noPetsTextView.visibility = TextView.GONE
                    displayPetNames(result)
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error fetching pets", e)
                Toast.makeText(this, "Error fetching pets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayPetNames(pets: QuerySnapshot) {
        // Clear any existing views before adding new ones
        gridLayout.removeAllViews()

        for (document in pets) {
            val petName = document.getString("name") ?: "No name"
            val petId = document.id
            Log.d("HomeActivity", "Pet name: $petName, Pet ID: $petId")

            // Create a new FrameLayout
            val frameLayout = layoutInflater.inflate(R.layout.pet_frame_layout, null) as FrameLayout

            // Find the pet name TextView and set the name dynamically
            val petNameTextView: TextView = frameLayout.findViewById(R.id.petNameTextView)
            petNameTextView.text = petName
            petNameTextView.setOnClickListener {
                val intent = Intent(this, ViewPetActivity::class.java)
                intent.putExtra("PET_ID", petId)
                startActivity(intent)
            }

            // Set the delete button click listener
            val deleteButton: Button = frameLayout.findViewById(R.id.deleteButton)
            deleteButton.setOnClickListener {
                deletePet(petId)
            }

            // Set the edit button click listener
            val editButton: Button = frameLayout.findViewById(R.id.editButton)
            editButton.setOnClickListener {
                selectedPetId = petId
                openGallery()
            }

            // Add the FrameLayout to the GridLayout
            gridLayout.addView(frameLayout)
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, SELECT_PHOTOS_REQUEST)
    }

    fun uploadImages(petId: String, uris: List<Uri>) {
        val currentUser = auth.currentUser ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("medicalDocuments/${currentUser.uid}/$petId")

        uris.forEach { uri ->
            val fileRef = storageRef.child("${System.currentTimeMillis()}.jpg")
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveImageUrl(petId, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error uploading photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun saveImageUrl(petId: String, imageUrl: String) {
        val currentUser = auth.currentUser ?: return
        val petDocRef = firestore.collection("users").document(currentUser.uid).collection("pets").document(petId)

        petDocRef.update("medicalDocuments", FieldValue.arrayUnion(imageUrl))
            .addOnSuccessListener {
                Toast.makeText(this, "Image added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving image URL: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletePet(petId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).collection("pets").document(petId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Pet deleted successfully", Toast.LENGTH_SHORT).show()
                    // Refresh the pet list
                    fetchUserPets(currentUser.uid)
                }
                .addOnFailureListener { e ->
                    Log.e("HomeActivity", "Error deleting pet", e)
                    Toast.makeText(this, "Error deleting pet: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}