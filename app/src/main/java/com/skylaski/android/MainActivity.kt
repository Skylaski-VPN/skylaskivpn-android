package com.skylaski.android

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS
import com.skylaski.android.wgm.HashUtils
import com.skylaski.android.wgm.WebViewClient

const val DEEP_LINK_PREFIX = "skylaski://wgm0.skylaski.com/skylaskivpnapp"
public const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36 SkylaskiVPN0.1"
public const val SKYLASKI_LOGIN_URL = "https://wgm0.skylaski.com/sign-in/index.php"
public const val DEFAULT_KILLSWITCH = "on"

class MainActivity : AppCompatActivity() {

    private val mTAG = "MainActivity"
    private lateinit var myWebView: WebView

    // Let the user go back through the webpages.
    override fun onBackPressed() {
        if (myWebView != null && myWebView.canGoBack()) myWebView.goBack() // if there is previous page open it
        else super.onBackPressed() //if there is no previous page, close app
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.skylaski.R.layout.activity_main)

        // Setup encrypted preferences
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                "secret_shared_prefs",
                DEFAULT_MASTER_KEY_ALIAS,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var deviceName: String = sharedPreferences.getString("device_name","").toString()
        var deviceId: String = sharedPreferences.getString("device_id","").toString()
        val userToken: String = sharedPreferences.getString("user_token","").toString()
        var localUid: String = sharedPreferences.getString("local_uid","").toString()

        // Check if the client already knows the device's local id
        if(deviceId == ""){
            Log.i(mTAG,"No device id saved, generating a new one")
            deviceId = Settings.Secure.ANDROID_ID
            sharedPreferences.edit().putString("device_id", deviceId).apply()
        }
        else{
            Log.i(mTAG, "Device id exists: $deviceId")
        }
        // check if client already knows the device's name
        if(deviceName == ""){
            Log.i(mTAG,"No device name, generating a new one")
            deviceName = android.os.Build.MODEL
            sharedPreferences.edit().putString("device_name", deviceName).apply()
        }
        // check if client already knows local uid
        if(localUid == ""){
            val combinedString: String = "$deviceName . $deviceId"
            localUid = HashUtils.sha256(combinedString)
            Log.i(mTAG,"No local_uid, generated a new one")
            sharedPreferences.edit().putString("local_uid",localUid).apply()
        }
        // Check if the client already acquired an API token, if so we redirect to ProfileActivity
        if(userToken != ""){

            val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_PREFIX))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("TOKEN", sharedPreferences.getString("user_token",""))

            startActivity(intent)
        }
        else {
            // Otherwise, redirect the user to login
            myWebView = findViewById(com.skylaski.R.id.webview)

            myWebView.webViewClient = WebViewClient(applicationContext)
            myWebView.clearCache(true)
            myWebView.clearHistory()
            myWebView.settings.useWideViewPort = true
            myWebView.settings.javaScriptEnabled = true
            myWebView.settings.userAgentString = USER_AGENT
            myWebView.loadUrl(SKYLASKI_LOGIN_URL)
        }

    }

}