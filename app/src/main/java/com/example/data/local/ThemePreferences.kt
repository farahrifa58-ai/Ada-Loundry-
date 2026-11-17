package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.adaLaundryDataStore by preferencesDataStore(name = "ada_laundry_prefs")

enum class ThemeMode(val label: String) {
  SYSTEM("Ikuti Sistem"),
  LIGHT("Tema Terang"),
  DARK("Tema Gelap");

  fun localizedLabel(isEn: Boolean): String = when (this) {
    SYSTEM -> if (isEn) "Follow System" else label
    LIGHT -> if (isEn) "Light Theme" else label
    DARK -> if (isEn) "Dark Theme" else label
  }
}

enum class AppLanguage(val label: String, val code: String) {
  ID("Bahasa Indonesia", "id"),
  EN("English", "en")
}

enum class ReceiptPaperSize(val label: String, val widthPx: Int) {
  MM_58("58mm Thermal", 320),
  MM_80("80mm Thermal", 420),
  A4("A4 / Standar", 595);

  fun localizedLabel(isEn: Boolean): String = when (this) {
    MM_58 -> "58mm Thermal"
    MM_80 -> "80mm Thermal"
    A4 -> if (isEn) "A4 / Standard" else label
  }

  companion object {
    fun fromString(value: String): ReceiptPaperSize =
      entries.firstOrNull { it.name == value } ?: MM_58
  }
}

class ThemePreferences(private val context: Context) {
  private val themeKey = stringPreferencesKey("theme_mode")
  private val languageKey = stringPreferencesKey("app_language")
  private val savedPrinterKey = stringPreferencesKey("saved_printer_name")
  private val paperSizeKey = stringPreferencesKey("receipt_paper_size")
  private val autoPrintReceiptKey = booleanPreferencesKey("auto_print_receipt")
  private val dueSoundEnabledKey = booleanPreferencesKey("due_sound_enabled")

  val themeModeFlow: Flow<ThemeMode> = context.adaLaundryDataStore.data.map { prefs ->
    val raw = prefs[themeKey] ?: ThemeMode.LIGHT.name
    ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.LIGHT
  }

  val languageFlow: Flow<AppLanguage> = context.adaLaundryDataStore.data.map { prefs ->
    val raw = prefs[languageKey] ?: AppLanguage.ID.name
    val resolved = AppLanguage.entries.firstOrNull { it.name == raw } ?: AppLanguage.ID
    saveLanguageSync(context, resolved)
    resolved
  }

  val savedPrinterFlow: Flow<String> = context.adaLaundryDataStore.data.map { prefs ->
    prefs[savedPrinterKey] ?: ""
  }

  val paperSizeFlow: Flow<ReceiptPaperSize> = context.adaLaundryDataStore.data.map { prefs ->
    ReceiptPaperSize.fromString(prefs[paperSizeKey] ?: ReceiptPaperSize.MM_58.name)
  }

  val dueSoundEnabledFlow: Flow<Boolean> = context.adaLaundryDataStore.data.map { prefs ->
    prefs[dueSoundEnabledKey] ?: true
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.adaLaundryDataStore.edit { prefs ->
      prefs[themeKey] = mode.name
    }
  }

  suspend fun setLanguage(language: AppLanguage) {
    saveLanguageSync(context, language)
    context.adaLaundryDataStore.edit { prefs ->
      prefs[languageKey] = language.name
    }
  }

  suspend fun setSavedPrinter(name: String) {
    context.adaLaundryDataStore.edit { prefs ->
      prefs[savedPrinterKey] = name
    }
  }

  suspend fun setPaperSize(paperSize: ReceiptPaperSize) {
    context.adaLaundryDataStore.edit { prefs ->
      prefs[paperSizeKey] = paperSize.name
    }
  }

  suspend fun setDueSoundEnabled(enabled: Boolean) {
    context.adaLaundryDataStore.edit { prefs ->
      prefs[dueSoundEnabledKey] = enabled
    }
  }

  companion object {
    private const val SYNC_PREFS_NAME = "ada_laundry_due_reminders"
    private const val KEY_APP_LANGUAGE = "sync_app_language"

    fun saveLanguageSync(context: Context, language: AppLanguage) {
      try {
        context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putString(KEY_APP_LANGUAGE, language.name)
          .apply()
      } catch (_: Throwable) {
      }
    }

    fun getSavedLanguageSync(context: Context): AppLanguage {
      return try {
        val raw = context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
          .getString(KEY_APP_LANGUAGE, AppLanguage.ID.name)
        AppLanguage.entries.firstOrNull { it.name == raw } ?: AppLanguage.ID
      } catch (_: Throwable) {
        AppLanguage.ID
      }
    }
  }
}
