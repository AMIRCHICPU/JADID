package com.example.smsfinder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * سرویسی که وقتی کلمه رمز از پیامک دریافت شود فعال می‌شود.
 * موقعیت GPS فعلی را می‌گیرد و آن را به همراه لینک نقشه گوگل،
 * از طریق پیامک به شماره‌ای که درخواست را فرستاده برمی‌گرداند.
 *
 * توجه: این کار فقط به آنتن‌دهی (شبکه GSM) نیاز دارد، نه به بسته اینترنت،
 * چون SMS از طریق شبکه سیگنالینگ اپراتور ارسال می‌شود.
 */
class LocationForegroundService : Service() {

    companion object {
        const val EXTRA_REPLY_TO = "reply_to"
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIF_ID = 42
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val replyTo = intent?.getStringExtra(EXTRA_REPLY_TO)

        startForeground(NOTIF_ID, buildNotification())

        if (replyTo != null) {
            fetchLocationAndReply(replyTo)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "\u0645\u06a9\u0627\u0646\u200c\u06cc\u0627\u0628\u06cc \u0627\u0632 \u0631\u0627\u0647 \u067e\u06cc\u0627\u0645\u06a9",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("\u062f\u0631 \u062d\u0627\u0644 \u067e\u06cc\u062f\u0627 \u06a9\u0631\u062f\u0646 \u0645\u0648\u0642\u0639\u06cc\u062a...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun fetchLocationAndReply(replyTo: String) {
        val hasFine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            sendSms(replyTo, "\u062e\u0637\u0627: \u0645\u062c\u0648\u0632 \u0645\u06a9\u0627\u0646 \u062f\u0627\u062f\u0647 \u0646\u0634\u062f\u0647.")
            stopSelf()
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        try {
            fusedClient.getCurrentLocation(request, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        replyWithLocation(replyTo, location)
                    } else {
                        // Fallback: آخرین موقعیت شناخته‌شده از LocationManager
                        val lastKnown = getLastKnownLocationFallback()
                        if (lastKnown != null) {
                            replyWithLocation(replyTo, lastKnown)
                        } else {
                            sendSms(replyTo, "\u0645\u0648\u0642\u0639\u06cc\u062a \u067e\u06cc\u062f\u0627 \u0646\u0634\u062f. GPS \u0631\u0648\u0634\u0646 \u0646\u06cc\u0633\u062a \u06cc\u0627 \u0633\u06cc\u06af\u0646\u0627\u0644 \u0636\u0639\u06cc\u0641 \u0627\u0633\u062a.")
                        }
                    }
                    stopSelf()
                }
                .addOnFailureListener {
                    val lastKnown = getLastKnownLocationFallback()
                    if (lastKnown != null) {
                        replyWithLocation(replyTo, lastKnown)
                    } else {
                        sendSms(replyTo, "\u062e\u0637\u0627 \u062f\u0631 \u067e\u06cc\u062f\u0627 \u06a9\u0631\u062f\u0646 \u0645\u0648\u0642\u0639\u06cc\u062a.")
                    }
                    stopSelf()
                }
        } catch (e: SecurityException) {
            sendSms(replyTo, "\u062e\u0637\u0627: \u062f\u0633\u062a\u0631\u0633\u06cc \u0628\u0647 \u0645\u06a9\u0627\u0646 \u0645\u0645\u06a9\u0646 \u0646\u0634\u062f.")
            stopSelf()
        }
    }

    private fun getLastKnownLocationFallback(): Location? {
        return try {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var best: Location? = null
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (best == null || loc.time > (best?.time ?: 0)) {
                    best = loc
                }
            }
            best
        } catch (e: SecurityException) {
            null
        }
    }

    private fun replyWithLocation(replyTo: String, location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val mapsLink = "https://maps.google.com/?q=$lat,$lon"
        val message = "\u0645\u0648\u0642\u0639\u06cc\u062a \u0641\u0639\u0644\u06cc \u06af\u0648\u0634\u06cc:\n$mapsLink\n(\u062f\u0642\u062a: ${location.accuracy}\u0645\u062a\u0631)"
        sendSms(replyTo, message)
    }

    private fun sendSms(to: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            // پیامک‌های طولانی را خودکار تقسیم می‌کند
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(to, null, parts, null, null)
        } catch (e: Exception) {
            // در صورت خطا در ارسال، کاری از دستمان برنمی‌آید چون لاگ هم قابل مشاهده نیست
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
