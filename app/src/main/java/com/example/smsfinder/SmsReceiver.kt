package com.example.smsfinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage

/**
 * این گیرنده هر پیامک دریافتی را بررسی می‌کند.
 * اگر متن پیامک برابر با کلمه رمز تنظیم‌شده باشد (مثلا "FINDME")،
 * سرویس مکان‌یابی را برای پیدا کردن و ارسال موقعیت GPS فعال می‌کند.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val keyword = prefs.getString(Prefs.KEY_KEYWORD, "FINDME") ?: "FINDME"

        val messages: Array<SmsMessage> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            @Suppress("DEPRECATION")
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        }

        if (messages.isEmpty()) return

        // پیامک ممکن است در چند بخش رسیده باشد؛ همه را به هم می‌چسبانیم
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val sender = messages[0].originatingAddress ?: return

        if (fullBody.trim().equals(keyword, ignoreCase = true)) {
            // کلمه رمز درست است -> سرویس فورگراند مکان‌یابی را استارت می‌کنیم
            val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                putExtra(LocationForegroundService.EXTRA_REPLY_TO, sender)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
