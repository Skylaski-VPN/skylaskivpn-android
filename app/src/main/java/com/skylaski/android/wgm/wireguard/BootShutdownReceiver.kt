package com.skylaski.android.wgm.wireguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS
import com.wireguard.android.backend.GoBackend
import kotlinx.coroutines.*


class BootShutdownReceiver: BroadcastReceiver() {
    private val mTAG = "BootShutdownReceiver"
    private lateinit var wgBackend: GoBackend
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var tunnelManager: TunnelManager
    private var tunnel = MyTunnel("wg0")

    override fun onReceive(context: Context, intent: Intent) {
        coroutineScope = CoroutineScope(Job() + Dispatchers.Main.immediate)
        // Decrypt the local sharedPreferences
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            DEFAULT_MASTER_KEY_ALIAS,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Setup the WireGuard GoBackend
        coroutineScope.launch(Dispatchers.IO){
            wgBackend = GoBackend(context)
            GoBackend.setAlwaysOnCallback {
                tunnelManager.connect(sharedPreferences)
            }
            tunnelManager = TunnelManager(context,wgBackend,tunnel)
        }

        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action) {
            Log.i(mTAG, "Broadcast receiver restoring state (boot)")

            tunnelManager.connect(sharedPreferences)

        } else if (Intent.ACTION_SHUTDOWN == action) {
            // Not sure what to do on shutdown yet, state is saved in encrypted sharedPreferences
            Log.i(mTAG, "Broadcast receiver saving state (shutdown)")
            //tunnelManager.saveState()
        }


    }

}