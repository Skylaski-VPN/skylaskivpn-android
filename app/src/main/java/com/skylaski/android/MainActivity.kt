package com.skylaski.android

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS
import com.skylaski.android.wgm.HashUtils
import com.skylaski.android.wgm.WebViewClient

const val WEB_DOMAIN="www.skylaski.com"
const val WEB_DOMAIN_REGEX="www\\.skylaski\\.com"
const val API_DOMAIN="wgm.skylaski.com"
const val CLIENT_DEEP_LINK_PREFIX = "skylaski://$WEB_DOMAIN/skylaskivpnapp"
public const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36 SkylaskiVPN0.1"
public const val SKYLASKI_LOGIN_URL = "https://$WEB_DOMAIN/sign-in/index.php"
public const val DEFAULT_KILLSWITCH = "on"
public const val ACCOUNT_URI = "https://$WEB_DOMAIN/sign-in/"

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
            deviceId = Settings.Secure.ANDROID_ID
            sharedPreferences.edit().putString("device_id", deviceId).apply()
        }

        // check if client already knows the device's name
        if(deviceName == ""){
            deviceName = android.os.Build.MODEL
            sharedPreferences.edit().putString("device_name", deviceName).apply()
        }
        // check if client already knows local uid
        if(localUid == ""){
            val combinedString: String = "$deviceName . $deviceId"
            localUid = HashUtils.sha256(combinedString)
            sharedPreferences.edit().putString("local_uid",localUid).apply()
        }
        // Check if the client already acquired an API token, if so we redirect to ProfileActivity
        if(userToken != ""){

            val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(CLIENT_DEEP_LINK_PREFIX))
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