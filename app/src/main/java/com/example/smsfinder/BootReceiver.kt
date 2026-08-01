package com.example.smsfinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * وقتی گوشی روشن می‌شود این گیرنده اجرا می‌شود.
 * چیز خاصی برای استارت کردن نیاز نیست چون SmsReceiver در مانیفست ثبت شده
 * و به‌صورت خودکار توسط سیستم فعال است، اما این کلاس را برای اطمینان
 * و امکان گسترش آینده (مثلا ارسال پیام "روشن شدم" به شماره مالک) نگه می‌داریم.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // در صورت نیاز، اینجا می‌توان یک پیامک اطلاع‌رسانی "گوشی روشن شد" فرستاد.
        }
    }
}
