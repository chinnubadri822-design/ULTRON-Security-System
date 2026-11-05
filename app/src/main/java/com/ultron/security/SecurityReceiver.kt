package com.ultron.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class SecurityReceiver : BroadcastReceiver() {
 companion object { const val CHANNEL_ID = "jarvis_security_alerts" }
 override fun onReceive(context: Context, intent: Intent) {
  if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
  val pkg = intent.data?.schemeSpecificPart ?: return
  if (pkg == context.packageName) return
  val name = try {
   val pm = context.packageManager
   val info = if (Build.VERSION.SDK_INT >= 33) pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0)) else {
    @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
   }
   pm.getApplicationLabel(info).toString()
  } catch (_: Exception) { pkg }
  val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
   nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "JARVIS security alerts", NotificationManager.IMPORTANCE_HIGH))
  }
  val pending = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
   PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
  val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL_ID) else {
   @Suppress("DEPRECATION") Notification.Builder(context)
  }
  val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle("New app installed: " + name)
   .setContentText("Review its permissions before using it.")
   .setStyle(Notification.BigTextStyle().bigText("A new app was installed: " + name + ". JARVIS cannot verify it is safe. Open JARVIS and review its permissions."))
   .setAutoCancel(true).setContentIntent(pending).build()
  nm.notify(pkg.hashCode() and 0x7fffffff, notification)
 }
}