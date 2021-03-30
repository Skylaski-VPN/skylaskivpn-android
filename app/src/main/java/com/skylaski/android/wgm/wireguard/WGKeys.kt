package com.skylaski.android.wgm.wireguard

import android.content.SharedPreferences
import android.util.Log
import com.wireguard.crypto.KeyPair

public class WGKeys {
    private var mTAG = "wgKeys"

    public fun genKeys(sharedPreferences: SharedPreferences){
        // Here we are generating keypairs, yay!
        Log.i(mTAG,"Generating new pair of WireGuard Keys")
        val keyPair = KeyPair()
        val privKey = keyPair.privateKey
        val pubKey = keyPair.publicKey
        sharedPreferences.edit().putString("wg_priv_key",privKey.toBase64().toString()).apply()
        sharedPreferences.edit().putString("wg_pub_key",pubKey.toBase64().toString()).apply()
    }
}