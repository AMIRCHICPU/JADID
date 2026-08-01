package com.example.smsfinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * صفحه اصلی: اینجا کلمه رمز (کد فعال‌سازی پیامکی) و وضعیت مجوزها را تنظیم می‌کنیم.
 * این اپ باید *قبل از گم شدن گوشی* نصب و راه‌اندازی شود.
 */
class MainActivity : AppCompatActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)

        val editKeyword = findViewById<EditText>(R.id.editKeyword)
        val statusText = findViewById<TextView>(R.id.textStatus)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnGrant = findViewById<Button>(R.id.btnGrantPermissions)
        val btnBackgroundLocation = findViewById<Button>(R.id.btnBackgroundLocation)

        editKeyword.setText(prefs.getString(Prefs.KEY_KEYWORD, "FINDME"))

        fun refreshStatus() {
            val granted = hasAllBasicPermissions()
            statusText.text = if (granted) {
                "\u2705 \u0647\u0645\u0647 \u0645\u062c\u0648\u0632\u0647\u0627 \u0641\u0639\u0627\u0644 \u0647\u0633\u062a\u0646\u062f. \u0627\u067e \u0622\u0645\u0627\u062f\u0647 \u0627\u0633\u062a."
            } else {
                "\u26a0\ufe0f \u0645\u062c\u0648\u0632\u0647\u0627 \u0647\u0646\u0648\u0632 \u06a9\u0627\u0645\u0644 \u0646\u06cc\u0633\u062a. \u062f\u06a9\u0645\u0647 '\u0627\u0639\u0637\u0627\u06cc \u0645\u062c\u0648\u0632' \u0631\u0627 \u0628\u0632\u0646."
            }
        }

        btnSave.setOnClickListener {
            val keyword = editKeyword.text.toString().trim()
            if (keyword.isEmpty()) {
                Toast.makeText(this, "\u06a9\u0644\u0645\u0647 \u0631\u0645\u0632 \u0631\u0627 \u0648\u0627\u0631\u062f \u06a9\u0646", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString(Prefs.KEY_KEYWORD, keyword).apply()
            Toast.makeText(this, "\u0630\u062e\u06cc\u0631\u0647 \u0634\u062f. \u0627\u06a9\u0646\u0648\u0646 \u0628\u0631\u0627\u06cc \u067e\u06cc\u062f\u0627 \u06a9\u0631\u062f\u0646 \u06af\u0648\u0634\u06cc\u060c \u067e\u06cc\u0627\u0645\u06a9\u06cc \u062d\u0627\u0648\u06cc \u0627\u06cc\u0646 \u06a9\u0644\u0645\u0647 \u0628\u0641\u0631\u0633\u062a", Toast.LENGTH_LONG).show()
        }

        btnGrantPermissions.setOnClickListener {
            ActivityCompat.requestPermissions(this, requiredPermissions, 1001)
        }

        btnBackgroundLocation.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    1002
                )
            } else {
                Toast.makeText(this, "\u0646\u06cc\u0627\u0632\u06cc \u0646\u06cc\u0633\u062a", Toast.LENGTH_SHORT).show()
            }
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.textStatus)
        statusText.text = if (hasAllBasicPermissions()) {
            "\u2705 \u0647\u0645\u0647 \u0645\u062c\u0648\u0632\u0647\u0627 \u0641\u0639\u0627\u0644 \u0647\u0633\u062a\u0646\u062f. \u0627\u067e \u0622\u0645\u0627\u062f\u0647 \u0627\u0633\u062a."
        } else {
            "\u26a0\ufe0f \u0645\u062c\u0648\u0632\u0647\u0627 \u0647\u0646\u0648\u0632 \u06a9\u0627\u0645\u0644 \u0646\u06cc\u0633\u062a. \u062f\u06a9\u0645\u0647 '\u0627\u0639\u0637\u0627\u06cc \u0645\u062c\u0648\u0632' \u0631\u0627 \u0628\u0632\u0646."
        }
    }

    private fun hasAllBasicPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
