package com.example.petcare

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petcare.utils.LogoutUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EdituserActivity : AppCompatActivity() {

    private lateinit var photoImageView: ImageView
    private lateinit var selectPhotoButton: Button
    private val SELECT_PHOTO_REQUEST = 1

    private lateinit var accountTypeSpinner: Spinner
    private lateinit var emailEditText: EditText
    private lateinit var userNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var saveButton: Button

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = firebaseAuth.currentUser
    private var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edituser)

        // Initialize Views
        photoImageView = findViewById(R.id.photoImageView)
        selectPhotoButton = findViewById(R.id.selectPhotoButton)
        emailEditText = findViewById(R.id.emailEditText)
        userNameEditText = findViewById(R.id.userNameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        locationEditText = findViewById(R.id.locationEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        accountTypeSpinner = findViewById(R.id.spinner1)
        saveButton = findViewById(R.id.saveButton)

        // Spinner setup
        val accountTypes = resources.getStringArray(R.array.account_types)
        accountTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, accountTypes)

        // Button listeners
        selectPhotoButton.setOnClickListener { openGallery() }
        saveButton.setOnClickListener { saveUserData() }

        setupBottomNavigation()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, SELECT_PHOTO_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHOTO_REQUEST && resultCode == RESULT_OK) {
            selectedPhotoUri = data?.data
            photoImageView.setImageURI(selectedPhotoUri)
        }
    }

    private fun saveUserData() {
        val email = emailEditText.text.toString().trim()
        val userName = userNameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()
        val accountType = accountTypeSpinner.selectedItem.toString()

        if (email.isEmpty() || userName.isEmpty() || password.isEmpty() || location.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Saving data...")
            setCancelable(false)
            show()
        }

        if (selectedPhotoUri != null) {
            uploadPhotoAndSaveData(userId, email, userName, password, location, phoneNumber, accountType, progressDialog)
        } else {
            saveDataToFirestore(userId, email, userName, password, location, phoneNumber, accountType, null, progressDialog)
        }
    }

    private fun uploadPhotoAndSaveData(
        userId: String,
        email: String,
        userName: String,
        password: String,
        location: String,
        phoneNumber: String,
        accountType: String,
        progressDialog: ProgressDialog
    ) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profilePhotos/$userId.jpg")

        storageRef.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveDataToFirestore(userId, email, userName, password, location, phoneNumber, accountType, downloadUri.toString(), progressDialog)
                }.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    showErrorToast("Failed to retrieve photo URL: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                showErrorToast("Photo upload failed: ${e.message}")
            }
    }

    private fun saveDataToFirestore(
        userId: String,
        email: String,
        userName: String,
        password: String,
        location: String,
        phoneNumber: String,
        accountType: String,
        photoUrl: String?,
        progressDialog: ProgressDialog
    ) {
        val userData = hashMapOf(
            "email" to email,
            "userName" to userName,
            "password" to password,
            "location" to location,
            "phoneNumber" to phoneNumber,
            "accountType" to accountType
        ).apply {
            photoUrl?.let { this["photoUrl"] = it }
        }

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Data saved successfully.", Toast.LENGTH_SHORT).show()
                finish()
                navigateTo(HomeActivity::class.java)

            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                showErrorToast("Data save failed: ${e.message}")
                finish()
                navigateTo(HomeActivity::class.java)

            }
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.bottom_nearby -> {
                    navigateTo(NearByDoctorsActivity::class.java)
                    true
                }
                R.id.bottom_logout -> {
                    LogoutUtils.showLogoutDialog(this)
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editUserLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(applicationContext, activityClass))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}