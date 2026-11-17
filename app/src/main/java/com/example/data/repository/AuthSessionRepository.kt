package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.ThemePreferences
import com.example.data.model.ActiveShopSession
import com.example.data.model.ServiceCategory
import com.example.data.model.ShopProfile
import com.example.data.model.StaffInvite
import com.example.data.model.StaffPermissions
import com.example.data.model.UserRole
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Locale

sealed interface SessionResolution {
  data class Ready(val session: ActiveShopSession) : SessionResolution
  data class NeedsShopSetup(
    val userUid: String,
    val userEmail: String,
    val userName: String,
    val inactiveInviteMessage: String? = null
  ) : SessionResolution
  data class ChooseRole(
    val ownerSession: ActiveShopSession,
    val cashierSession: ActiveShopSession
  ) : SessionResolution
}

class AuthSessionRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth,
  private val appContext: Context? = null
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance(),
    context.applicationContext
  )

  private val isEn: Boolean
    get() = appContext?.let { ThemePreferences.getSavedLanguageSync(it) == AppLanguage.EN } ?: false

  private fun requireUser() = auth.currentUser
    ?: throw IllegalStateException(
      if (isEn) "User must sign in with Google first."
      else "Pengguna harus login dengan Google terlebih dahulu."
    )

  suspend fun resolveCurrentSession(): Result<SessionResolution> {
    return try {
      val user = requireUser()
      val uid = user.uid
      val emailLower = (user.email ?: "").trim().lowercase(Locale.ROOT)
      val displayName = user.displayName?.takeIf { it.isNotBlank() }
        ?: emailLower.substringBefore("@").ifBlank { if (isEn) "User" else "Pengguna" }

      // 1. Check if user owns a shop (/shops/{uid})
      val ownerShopSnap = db.collection("shops").document(uid).get().await()
      val ownerSession = if (ownerShopSnap.exists()) {
        val profile = ownerShopSnap.toObject(
          ShopProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        profile?.let {
          ActiveShopSession(
            shopId = uid,
            userUid = uid,
            userName = displayName,
            userEmail = emailLower,
            role = UserRole.OWNER,
            permissions = StaffPermissions.ownerAll(),
            shopProfile = it
          )
        }
      } else {
        null
      }

      // 2. Check if user is invited as a Kasir via /staffDirectory
      var cashierSession: ActiveShopSession? = null
      var inactiveMsg: String? = null

      if (emailLower.isNotBlank()) {
        val invitesSnap = db.collection("staffDirectory")
          .whereEqualTo("email", emailLower)
          .get()
          .await()

        val invites = invitesSnap.documents.mapNotNull { doc ->
          doc.toObject(StaffInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

        val activeInvite = invites.firstOrNull { it.active }
        if (activeInvite != null) {
          // Bind UID to /shops/{shopId}/staffMembers/{uid} and /staffDirectory/{inviteId}
          val staffRef = db.collection("shops")
            .document(activeInvite.shopId)
            .collection("staffMembers")
            .document(uid)

          val invitedRole = if (activeInvite.role == "OWNER") "OWNER" else "KASIR"
          val effectivePerms = if (invitedRole == "OWNER") {
            StaffPermissions.ownerAll().toMap()
          } else {
            activeInvite.permissions
          }
          val existingStaff = staffRef.get().await()
          val memberPayload = buildMap<String, Any> {
            put("shopId", activeInvite.shopId)
            put("inviteId", activeInvite.id)
            put("name", activeInvite.name)
            put("email", emailLower)
            put("role", invitedRole)
            put("active", true)
            put("permissions", effectivePerms)
            put(
              "createdAt",
              existingStaff.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            )
            put("updatedAt", FieldValue.serverTimestamp())
          }
          staffRef.set(memberPayload).await()

          if (activeInvite.staffUid != uid) {
            db.collection("staffDirectory").document(activeInvite.id)
              .update(
                mapOf(
                  "staffUid" to uid,
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
              .await()
          }

          val shopSnap = db.collection("shops").document(activeInvite.shopId).get().await()
          val shopProfile = shopSnap.toObject(
            ShopProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ) ?: ShopProfile(id = activeInvite.shopId, businessName = "ADA LAUNDRY")

          cashierSession = ActiveShopSession(
            shopId = activeInvite.shopId,
            userUid = uid,
            userName = activeInvite.name.ifBlank { displayName },
            userEmail = emailLower,
            role = if (invitedRole == "OWNER") UserRole.OWNER else UserRole.KASIR,
            permissions = if (invitedRole == "OWNER") StaffPermissions.ownerAll() else activeInvite.parsedPermissions,
            shopProfile = shopProfile
          )
        } else if (invites.any { !it.active }) {
          inactiveMsg = if (isEn) {
            "Your cashier account is currently disabled by the Owner. Please contact the Owner to reactivate it."
          } else {
            "Akun kasir Anda saat ini dinonaktifkan oleh Pemilik. Hubungi Pemilik untuk mengaktifkan kembali."
          }
        }
      }

      val resolution = when {
        ownerSession != null && cashierSession != null ->
          SessionResolution.ChooseRole(ownerSession, cashierSession)
        ownerSession != null ->
          SessionResolution.Ready(ownerSession)
        cashierSession != null ->
          SessionResolution.Ready(cashierSession)
        else ->
          SessionResolution.NeedsShopSetup(
            userUid = uid,
            userEmail = emailLower,
            userName = displayName,
            inactiveInviteMessage = inactiveMsg
          )
      }
      Result.success(resolution)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "session_resolution")
      Result.failure(e)
    }
  }

  suspend fun createOwnerShopWithCatalog(
    businessName: String,
    address: String,
    phone: String,
    operatingHours: String,
    notaPrefix: String
  ): Result<ActiveShopSession> {
    return try {
      val user = requireUser()
      val uid = user.uid
      val emailLower = (user.email ?: "").trim().lowercase(Locale.ROOT)
      val displayName = user.displayName?.takeIf { it.isNotBlank() }
        ?: emailLower.substringBefore("@").ifBlank { "Pemilik" }

      val shopRef = db.collection("shops").document(uid)
      val cleanPrefix = notaPrefix.trim().uppercase(Locale.ROOT).ifBlank { "ADA" }.take(12)
      val shopPayload = mapOf(
        "ownerUid" to uid,
        "ownerEmail" to emailLower,
        "businessName" to businessName.trim().ifBlank { "ADA LAUNDRY" },
        "address" to address.trim(),
        "phone" to phone.trim(),
        "operatingHours" to operatingHours.trim().ifBlank { "07:00 - 21:00" },
        "notaPrefix" to cleanPrefix,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )

      val batch = db.batch()
      batch.set(shopRef, shopPayload)

      // Initial service catalog templates (prices editable by owner; 0 fake transactions)
      val initialServices = listOf(
        Triple("Cuci Komplit Reguler", ServiceCategory.KILOAN, 7000L) to ("kg" to false),
        Triple("Cuci Komplit Express", ServiceCategory.KILOAN, 12000L) to ("kg" to false),
        Triple("Setrika Saja", ServiceCategory.KILOAN, 5000L) to ("kg" to false),
        Triple("Kemeja", ServiceCategory.SATUAN, 8000L) to ("pcs" to false),
        Triple("Celana", ServiceCategory.SATUAN, 8000L) to ("pcs" to false),
        Triple("Jaket", ServiceCategory.SATUAN, 15000L) to ("pcs" to false),
        Triple("Selimut", ServiceCategory.SATUAN, 20000L) to ("pcs" to false),
        Triple("Bed Cover", ServiceCategory.SATUAN, 30000L) to ("pcs" to false),
        Triple("Karpet Standar", ServiceCategory.KARPET, 25000L) to ("lembar" to false),
        Triple("Karpet Tebal / Besar", ServiceCategory.KARPET, 40000L) to ("lembar" to false),
        Triple("Layanan Express", ServiceCategory.KILOAN, 10000L) to ("kg" to true)
      )

      initialServices.forEachIndexed { idx, (triple, unitAndExtra) ->
        val (name, category, price) = triple
        val (unitLabel, isExtra) = unitAndExtra
        val svcRef = shopRef.collection("services").document("svc_default_${idx + 1}")
        batch.set(
          svcRef,
          mapOf(
            "shopId" to uid,
            "name" to name,
            "category" to category.name,
            "unitLabel" to unitLabel,
            "price" to price,
            "isExtraFee" to isExtra,
            "active" to true,
            "description" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }

      // Initial expense categories
      val defaultCategories = listOf(
        "Deterjen",
        "Pewangi",
        "Plastik laundry",
        "Listrik",
        "Air",
        "Gaji karyawan",
        "Sewa tempat",
        "Perawatan mesin",
        "Pengeluaran lainnya"
      )
      defaultCategories.forEachIndexed { idx, catName ->
        val catRef = shopRef.collection("expenseCategories").document("cat_default_${idx + 1}")
        batch.set(
          catRef,
          mapOf(
            "shopId" to uid,
            "name" to catName,
            "isDefault" to true,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }

      batch.commit().await()

      val createdProfile = ShopProfile(
        id = uid,
        ownerUid = uid,
        ownerEmail = emailLower,
        businessName = businessName.trim().ifBlank { "ADA LAUNDRY" },
        address = address.trim(),
        phone = phone.trim(),
        operatingHours = operatingHours.trim().ifBlank { "07:00 - 21:00" },
        notaPrefix = cleanPrefix
      )

      Result.success(
        ActiveShopSession(
          shopId = uid,
          userUid = uid,
          userName = displayName,
          userEmail = emailLower,
          role = UserRole.OWNER,
          permissions = StaffPermissions.ownerAll(),
          shopProfile = createdProfile
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "shops")
      Result.failure(e)
    }
  }
}
