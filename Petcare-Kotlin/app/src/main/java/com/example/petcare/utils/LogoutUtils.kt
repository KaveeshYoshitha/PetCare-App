package com.example.petcare.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.example.petcare.LoginActivity
import com.google.firebase.auth.FirebaseAuth

object LogoutUtils {

    fun showLogoutDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Logout")
        builder.setMessage("Do you want to logout?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            signOutUser(context) // Call the sign-out function
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun signOutUser(context: Context) {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Navigate to LoginActivity
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        context.startActivity(intent)
    }
}
