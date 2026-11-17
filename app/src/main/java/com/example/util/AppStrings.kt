package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.local.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ID }

@Composable
@ReadOnlyComposable
fun isEnglish(): Boolean = LocalAppLanguage.current == AppLanguage.EN

@Composable
@ReadOnlyComposable
fun tr(id: String, en: String): String =
  if (LocalAppLanguage.current == AppLanguage.EN) en else id

fun str(lang: AppLanguage, id: String, en: String): String =
  if (lang == AppLanguage.EN) en else id

fun localizedUnit(unitLabel: String, isEn: Boolean): String {
  if (!isEn) return unitLabel
  return when (unitLabel.trim().lowercase()) {
    "lembar" -> "sheet"
    else -> unitLabel
  }
}

fun localizedUnit(unitLabel: String, lang: AppLanguage): String =
  localizedUnit(unitLabel, lang == AppLanguage.EN)
