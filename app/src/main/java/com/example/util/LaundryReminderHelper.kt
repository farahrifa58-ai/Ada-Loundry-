package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ThemePreferences
import com.example.data.model.LaundryOrder
import com.example.data.model.OrderStatus
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

internal data class PendingAlarmOrder(
  val id: String,
  val notaNumber: String,
  val customerName: String,
  val orderStatus: String,
  val estimatedFinishAtMillis: Long
)

class DueLaundryAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    if (action == Intent.ACTION_BOOT_COMPLETED ||
      action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    ) {
      LaundryReminderHelper.rescheduleFromCache(context)
      return
    }

    val pendingResult = try {
      goAsync()
    } catch (_: Throwable) {
      null
    }

    val wakeLock = try {
      val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdaLaundry::DuePickupWakeLock")?.apply {
        setReferenceCounted(false)
        acquire(12_000L)
      }
    } catch (_: Throwable) {
      null
    }

    val work = Runnable {
      var soundPlayed = false
      try {
        val now = System.currentTimeMillis()
        val playSound = LaundryReminderHelper.isDueSoundEnabledSync(
          context = context,
          fallback = intent.getBooleanExtra(EXTRA_PLAY_SOUND, true)
        )
        val lang = ThemePreferences.getSavedLanguageSync(context)
        val isEn = lang == AppLanguage.EN

        val cachedDue = LaundryReminderHelper.consumeDueCachedOrders(context, now + 10_000L)
        if (cachedDue.isNotEmpty()) {
          LaundryReminderHelper.notifyCachedDueOrders(
            context = context,
            dueOrders = cachedDue,
            playSound = playSound,
            isEn = isEn
          )
          soundPlayed = playSound
        } else {
          val nota = intent.getStringExtra(EXTRA_NOTA).orEmpty()
          val customer = intent.getStringExtra(EXTRA_CUSTOMER).orEmpty()
          val orderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
          val estMillis = intent.getLongExtra(EXTRA_EST_MILLIS, 0L)
          if (nota.isNotBlank() && orderId.isNotBlank()) {
            LaundryReminderHelper.markOrderNotified(context, orderId, estMillis)
            val title = if (isEn) "Laundry Pickup Time Arrived" else "Waktu Pengambilan Cucian Tiba"
            val body = if (isEn) {
              "Receipt $nota ($customer) has reached its scheduled pickup time."
            } else {
              "Nota $nota ($customer) sudah mencapai jadwal pengambilan."
            }
            LaundryReminderHelper.postSystemNotification(
              context = context,
              notificationId = (orderId.hashCode() and 0x7FFFFFFF),
              title = title,
              body = body,
              playSound = playSound
            )
            if (playSound) {
              LaundryReminderHelper.playAlertSound(context)
              soundPlayed = true
            }
          }
        }

        LaundryReminderHelper.rescheduleFromCache(context, now + 10_000L)

        // Keep CPU awake briefly so alarm audio stream finishes cleanly while screen is off
        if (soundPlayed && pendingResult != null) {
          Thread.sleep(3_500L)
        }
      } catch (_: Throwable) {
      } finally {
        try {
          if (wakeLock?.isHeld == true) wakeLock.release()
        } catch (_: Throwable) {
        }
        try {
          pendingResult?.finish()
        } catch (_: Throwable) {
        }
      }
    }

    if (pendingResult != null) {
      executor.execute(work)
    } else {
      work.run()
    }
  }

  companion object {
    const val EXTRA_ORDER_ID = "extra_order_id"
    const val EXTRA_NOTA = "extra_nota"
    const val EXTRA_CUSTOMER = "extra_customer"
    const val EXTRA_EST_MILLIS = "extra_est_millis"
    const val EXTRA_PLAY_SOUND = "extra_play_sound"

    private val executor = Executors.newSingleThreadExecutor()
  }
}

object LaundryReminderHelper {
  private const val PREFS_NAME = "ada_laundry_due_reminders"
  private const val KEY_NOTIFIED_SET = "notified_order_keys"
  private const val KEY_PENDING_ORDERS_JSON = "pending_alarm_orders_json"
  private const val KEY_DUE_SOUND_ENABLED = "due_sound_enabled_sync"
  private const val CHANNEL_ID = "ada_laundry_due_pickup_v3"
  private const val ALARM_REQUEST_CODE = 4401

  @Volatile
  private var activePlayer: MediaPlayer? = null

  fun isPowerSaveRestricted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
    return try {
      val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
      !pm.isIgnoringBatteryOptimizations(context.packageName)
    } catch (_: Throwable) {
      false
    }
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
      alarmManager.canScheduleExactAlarms()
    } catch (_: Throwable) {
      false
    }
  }

  fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      try {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = Uri.parse("package:${context.packageName}")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return
      } catch (_: Throwable) {
      }
    }
    openAppNotificationSettings(context)
  }

  fun openBatteryOptimizationSettings(context: Context) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return
      }
    } catch (_: Throwable) {
    }
    openAppNotificationSettings(context)
  }

  fun openAppNotificationSettings(context: Context) {
    try {
      val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
          putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
      } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.parse("package:${context.packageName}")
        }
      }
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(intent)
    } catch (_: Throwable) {
    }
  }

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    try {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val isEn = ThemePreferences.getSavedLanguageSync(context) == AppLanguage.EN
      val channelName = if (isEn) "Laundry Pickup Alarm" else "Pengingat Cucian Tepat Waktu"
      val channelDesc = if (isEn) {
        "Audible alarm when customer laundry reaches its scheduled pickup time, even when locked."
      } else {
        "Notifikasi bersuara ketika cucian pelanggan mencapai waktu pengambilan, termasuk saat layar mati."
      }
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing != null) {
        existing.name = channelName
        existing.description = channelDesc
        manager.createNotificationChannel(existing)
        return
      }
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      val channel = NotificationChannel(
        CHANNEL_ID,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = channelDesc
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 400L, 200L, 450L)
        enableLights(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setBypassDnd(true)
        setSound(soundUri, audioAttrs)
      }
      manager.createNotificationChannel(channel)
    } catch (_: Throwable) {
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun isDueSoundEnabledSync(context: Context, fallback: Boolean = true): Boolean {
    return try {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_DUE_SOUND_ENABLED, fallback)
    } catch (_: Throwable) {
      fallback
    }
  }

  fun findNewlyDueOrders(
    context: Context,
    orders: List<LaundryOrder>,
    nowMillis: Long = System.currentTimeMillis()
  ): List<LaundryOrder> {
    val notifiedKeys = getNotifiedKeys(context)
    return orders.filter { order ->
      !order.orderStatusEnum.isTerminal &&
        order.estimatedFinishAtMillis > 0L &&
        order.estimatedFinishAtMillis <= nowMillis &&
        !notifiedKeys.contains(dueKey(order.id, order.estimatedFinishAtMillis))
    }
  }

  fun markOrderNotified(context: Context, orderId: String, estimatedFinishAtMillis: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val updated = getNotifiedKeys(context).toMutableSet()
    updated.add(dueKey(orderId, estimatedFinishAtMillis))
    val trimmed = if (updated.size > 300) updated.toList().takeLast(200).toSet() else updated
    prefs.edit().putStringSet(KEY_NOTIFIED_SET, trimmed).apply()
  }

  fun markOrdersNotified(context: Context, orders: List<LaundryOrder>) {
    if (orders.isEmpty()) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val updated = getNotifiedKeys(context).toMutableSet()
    orders.forEach { order ->
      updated.add(dueKey(order.id, order.estimatedFinishAtMillis))
    }
    val trimmed = if (updated.size > 300) updated.toList().takeLast(200).toSet() else updated
    prefs.edit().putStringSet(KEY_NOTIFIED_SET, trimmed).apply()
  }

  private fun getNotifiedKeys(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_NOTIFIED_SET, emptySet()) ?: emptySet()
  }

  private fun dueKey(orderId: String, estimatedMillis: Long): String = "${orderId}_$estimatedMillis"

  private fun vibrateAlert(context: Context) {
    try {
      val pattern = longArrayOf(0L, 400L, 200L, 450L)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vm?.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(pattern, -1)
        }
      }
    } catch (_: Throwable) {
    }
  }

  fun playAlertSound(context: Context) {
    vibrateAlert(context)
    val appCtx = context.applicationContext ?: context
    val soundUri = try {
      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    } catch (_: Throwable) {
      null
    }

    // Play explicitly on AudioAttributes.USAGE_ALARM so lock-screen / screen-off notification stream gating cannot silence it
    if (soundUri != null) {
      try {
        activePlayer?.run {
          try {
            if (isPlaying) stop()
            release()
          } catch (_: Throwable) {
          }
        }
        val player = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          setDataSource(appCtx, soundUri)
          isLooping = false
          setOnCompletionListener { mp ->
            try {
              mp.release()
            } catch (_: Throwable) {
            }
            if (activePlayer === mp) activePlayer = null
          }
          prepare()
          start()
        }
        activePlayer = player
        return
      } catch (_: Throwable) {
      }

      try {
        val ringtone = RingtoneManager.getRingtone(appCtx, soundUri)
        if (ringtone != null) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ringtone.audioAttributes = AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          }
          ringtone.play()
          return
        }
      } catch (_: Throwable) {
      }
    }

    try {
      val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
      toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
    } catch (_: Throwable) {
    }
  }

  fun postSystemNotification(
    context: Context,
    notificationId: Int,
    title: String,
    body: String,
    playSound: Boolean
  ) {
    try {
      if (!hasNotificationPermission(context)) return
      ensureNotificationChannel(context)
      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val soundUri = if (playSound) {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      } else null

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setOnlyAlertOnce(false)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      if (soundUri != null) {
        builder.setSound(soundUri, AudioManager.STREAM_ALARM)
        builder.setVibrate(longArrayOf(0L, 400L, 200L, 450L))
      } else {
        builder.setSilent(true)
      }

      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      manager.notify(notificationId, builder.build())
    } catch (_: Throwable) {
    }
  }

  fun notifyDueOrders(
    context: Context,
    dueOrders: List<LaundryOrder>,
    playSound: Boolean
  ) {
    if (dueOrders.isEmpty()) return
    markOrdersNotified(context, dueOrders)
    if (playSound) {
      playAlertSound(context)
    }
    val isEn = ThemePreferences.getSavedLanguageSync(context) == AppLanguage.EN
    val title = if (dueOrders.size == 1) {
      if (isEn) "Pickup Time Arrived • ${dueOrders.first().notaNumber}"
      else "Waktu Pengambilan Tiba • ${dueOrders.first().notaNumber}"
    } else {
      if (isEn) "${dueOrders.size} Laundry Orders Are Due"
      else "${dueOrders.size} Cucian Sudah Tepat Waktu"
    }
    val body = if (dueOrders.size == 1) {
      val o = dueOrders.first()
      if (isEn) {
        "Laundry for ${o.customerName} (${o.orderStatusEnum.localizedLabel(true)}) has reached pickup time."
      } else {
        "Cucian ${o.customerName} (${o.orderStatusEnum.label}) telah mencapai jadwal pengambilan."
      }
    } else {
      dueOrders.take(3).joinToString(", ") { "${it.notaNumber} (${it.customerName})" }
    }
    postSystemNotification(
      context = context,
      notificationId = (dueOrders.first().id.hashCode() and 0x7FFFFFFF),
      title = title,
      body = body,
      playSound = playSound
    )
  }

  internal fun notifyCachedDueOrders(
    context: Context,
    dueOrders: List<PendingAlarmOrder>,
    playSound: Boolean,
    isEn: Boolean
  ) {
    if (dueOrders.isEmpty()) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val updated = getNotifiedKeys(context).toMutableSet()
    dueOrders.forEach { o ->
      updated.add(dueKey(o.id, o.estimatedFinishAtMillis))
    }
    val trimmed = if (updated.size > 300) updated.toList().takeLast(200).toSet() else updated
    prefs.edit().putStringSet(KEY_NOTIFIED_SET, trimmed).apply()

    if (playSound) {
      playAlertSound(context)
    }
    val title = if (dueOrders.size == 1) {
      if (isEn) "Pickup Time Arrived • ${dueOrders.first().notaNumber}"
      else "Waktu Pengambilan Tiba • ${dueOrders.first().notaNumber}"
    } else {
      if (isEn) "${dueOrders.size} Laundry Orders Are Due"
      else "${dueOrders.size} Cucian Sudah Tepat Waktu"
    }
    val body = if (dueOrders.size == 1) {
      val o = dueOrders.first()
      val statusLabel = OrderStatus.fromString(o.orderStatus).localizedLabel(isEn)
      if (isEn) {
        "Laundry for ${o.customerName} ($statusLabel) has reached pickup time."
      } else {
        "Cucian ${o.customerName} ($statusLabel) telah mencapai jadwal pengambilan."
      }
    } else {
      dueOrders.take(3).joinToString(", ") { "${it.notaNumber} (${it.customerName})" }
    }
    postSystemNotification(
      context = context,
      notificationId = (dueOrders.first().id.hashCode() and 0x7FFFFFFF),
      title = title,
      body = body,
      playSound = playSound
    )
  }

  private fun savePendingOrdersCache(
    context: Context,
    pendingOrders: List<PendingAlarmOrder>,
    playSound: Boolean
  ) {
    try {
      val array = JSONArray()
      pendingOrders.take(100).forEach { entry ->
        val obj = JSONObject().apply {
          put("id", entry.id)
          put("nota", entry.notaNumber)
          put("customer", entry.customerName)
          put("status", entry.orderStatus)
          put("est", entry.estimatedFinishAtMillis)
        }
        array.put(obj)
      }
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PENDING_ORDERS_JSON, array.toString())
        .putBoolean(KEY_DUE_SOUND_ENABLED, playSound)
        .apply()
    } catch (_: Throwable) {
    }
  }

  private fun loadPendingOrdersCache(context: Context): List<PendingAlarmOrder> {
    return try {
      val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_PENDING_ORDERS_JSON, "[]") ?: "[]"
      val array = JSONArray(raw)
      buildList {
        for (i in 0 until array.length()) {
          val obj = array.optJSONObject(i) ?: continue
          val id = obj.optString("id")
          val nota = obj.optString("nota")
          val est = obj.optLong("est", 0L)
          if (id.isNotBlank() && nota.isNotBlank() && est > 0L) {
            add(
              PendingAlarmOrder(
                id = id,
                notaNumber = nota,
                customerName = obj.optString("customer"),
                orderStatus = obj.optString("status", OrderStatus.RECEIVED.name),
                estimatedFinishAtMillis = est
              )
            )
          }
        }
      }
    } catch (_: Throwable) {
      emptyList()
    }
  }

  internal fun consumeDueCachedOrders(
    context: Context,
    cutoffMillis: Long
  ): List<PendingAlarmOrder> {
    val notifiedKeys = getNotifiedKeys(context)
    val cached = loadPendingOrdersCache(context)
    return cached.filter {
      it.estimatedFinishAtMillis <= cutoffMillis &&
        !notifiedKeys.contains(dueKey(it.id, it.estimatedFinishAtMillis))
    }
  }

  fun rescheduleFromCache(
    context: Context,
    nowMillis: Long = System.currentTimeMillis()
  ) {
    val notifiedKeys = getNotifiedKeys(context)
    val playSound = isDueSoundEnabledSync(context, true)
    val upcoming = loadPendingOrdersCache(context)
      .filter {
        it.estimatedFinishAtMillis > nowMillis &&
          !notifiedKeys.contains(dueKey(it.id, it.estimatedFinishAtMillis))
      }
      .sortedBy { it.estimatedFinishAtMillis }
    savePendingOrdersCache(context, upcoming, playSound)
    scheduleAlarmForEntry(context, upcoming.firstOrNull(), playSound)
  }

  fun scheduleNextDueAlarm(
    context: Context,
    orders: List<LaundryOrder>,
    playSound: Boolean,
    nowMillis: Long = System.currentTimeMillis()
  ) {
    val notifiedKeys = getNotifiedKeys(context)
    val upcoming = orders
      .filter {
        !it.orderStatusEnum.isTerminal &&
          it.estimatedFinishAtMillis > nowMillis &&
          !notifiedKeys.contains(dueKey(it.id, it.estimatedFinishAtMillis))
      }
      .sortedBy { it.estimatedFinishAtMillis }
      .map {
        PendingAlarmOrder(
          id = it.id,
          notaNumber = it.notaNumber,
          customerName = it.customerName,
          orderStatus = it.orderStatus,
          estimatedFinishAtMillis = it.estimatedFinishAtMillis
        )
      }
    savePendingOrdersCache(context, upcoming, playSound)
    scheduleAlarmForEntry(context, upcoming.firstOrNull(), playSound)
  }

  @SuppressLint("MissingPermission")
  private fun scheduleAlarmForEntry(
    context: Context,
    nextOrder: PendingAlarmOrder?,
    playSound: Boolean
  ) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val intent = Intent(context, DueLaundryAlarmReceiver::class.java).apply {
        if (nextOrder != null) {
          putExtra(DueLaundryAlarmReceiver.EXTRA_ORDER_ID, nextOrder.id)
          putExtra(DueLaundryAlarmReceiver.EXTRA_NOTA, nextOrder.notaNumber)
          putExtra(DueLaundryAlarmReceiver.EXTRA_CUSTOMER, nextOrder.customerName)
          putExtra(DueLaundryAlarmReceiver.EXTRA_EST_MILLIS, nextOrder.estimatedFinishAtMillis)
          putExtra(DueLaundryAlarmReceiver.EXTRA_PLAY_SOUND, playSound)
        }
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      alarmManager.cancel(pendingIntent)
      if (nextOrder != null) {
        if (canScheduleExactAlarms(context)) {
          // AlarmClock guarantees exact wake-up even when locked & screen is off in Deep Doze
          val showAppIntent = PendingIntent.getActivity(
            context,
            ALARM_REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
          val alarmClockInfo = AlarmManager.AlarmClockInfo(
            nextOrder.estimatedFinishAtMillis,
            showAppIntent
          )
          alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextOrder.estimatedFinishAtMillis,
            pendingIntent
          )
        } else {
          alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            nextOrder.estimatedFinishAtMillis,
            pendingIntent
          )
        }
      }
    } catch (_: Throwable) {
    }
  }
}
