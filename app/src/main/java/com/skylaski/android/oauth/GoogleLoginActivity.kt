package com.skylaski.android.oauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.skylaski.R
import com.skylaski.android.WEB_DOMAIN

private const val GOOGLE_LOGIN_URL = "https://$WEB_DOMAIN/sign-in/google-mobile.php"

class GoogleLoginActivity : AppCompatActivity() {
    private val mTag = "GoogleLoginActivity"

    /*
        Google doesn't let us authenticate view WebViewClient so we have to redirect the user to their default browser.
        WGM assumes everyone reaching out to google-mobile.php is logging in via an android client and sets session value android=true
        The profile page checks for this value and redirect back to skylaski://
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_login)

        val intentAction = intent.action
        if(intentAction == Intent.ACTION_VIEW) {
            val newIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_LOGIN_URL))
            startActivity(newIntent)
        }
    }
}
