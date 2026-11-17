package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(private val context: Context) {
  fun observeIsOnline(): Flow<Boolean> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun checkNow(): Boolean {
      val active = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(active) ?: return false
      return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    trySend(checkNow())

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onLost(network: Network) {
        trySend(checkNow())
      }

      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
        trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    cm.registerNetworkCallback(request, callback)

    awaitClose {
      cm.unregisterNetworkCallback(callback)
    }
  }.distinctUntilChanged()
}
