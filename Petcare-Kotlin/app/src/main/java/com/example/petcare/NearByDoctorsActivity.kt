package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petcare.utils.LogoutUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

class NearByDoctorsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nearbydoctors)





        //Bottom Navigation bar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_nearby
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home ->  {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_nearby -> true
                R.id.bottom_logout -> {
                    LogoutUtils.showLogoutDialog(this)
                    true
                }


                else -> false
            }
        }







        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nearByDoctorsLayout)) { v, insets ->
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


    }
}