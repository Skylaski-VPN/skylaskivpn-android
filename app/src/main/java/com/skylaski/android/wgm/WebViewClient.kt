package com.skylaski.android.wgm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import com.skylaski.android.WEB_DOMAIN
import com.skylaski.android.WEB_DOMAIN_REGEX

const val DEEP_LINK_PREFIX = "skylaski://$WEB_DOMAIN/skylaskivpnapp"
const val GOOGLE_LOGIN_DEEP_LINK_PREFIX = "skylaski://$WEB_DOMAIN/google-login"
const val NOACCOUNT_DEEP_LINK_PREFIX = "skylaski://$WEB_DOMAIN/NoAccount"

class WebViewClient(context: Context) : WebViewClient() {
    private var myContext = context
    private var mTAG = "WebViewClient"

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {

        // Check for the website detecting the app and redirecting us back in.
        if (url.toString().startsWith(DEEP_LINK_PREFIX)) {
            // get token from string
            val pattern = Regex("^skylaski:\\/\\/$WEB_DOMAIN_REGEX\\/skylaskivpnapp\\?token=(.*)$")
            val matchResult = pattern.find(url.toString())
            val userToken = matchResult!!.groupValues[1]

            val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("TOKEN", userToken)

            myContext.startActivity(intent)

            return true
        }
        else if(url.toString().startsWith(GOOGLE_LOGIN_DEEP_LINK_PREFIX)){
            // Check to see if we're trying to login with Google
            val intent = Intent(Intent.ACTION_VIEW,Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            myContext.startActivity(intent)
            return true

        }
        else if(url.toString().startsWith(NOACCOUNT_DEEP_LINK_PREFIX)){
            val intent = Intent(Intent.ACTION_VIEW,Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            myContext.startActivity(intent)
            return true
        }

        return super.shouldOverrideUrlLoading(view, url)
    }

}