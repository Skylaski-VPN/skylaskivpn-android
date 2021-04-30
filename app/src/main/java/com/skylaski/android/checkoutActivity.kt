package com.skylaski.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.billingclient.api.*
import com.skylaski.R
import com.skylaski.android.wgm.HashUtils
import com.skylaski.android.wgm.WGMApi
import kotlinx.coroutines.*


class checkoutActivity : AppCompatActivity() {
    private val mTag = "NoAccountActivity"
    private lateinit var mContext: Context
    private lateinit var coroutine: CoroutineScope
    private lateinit var billingClient: BillingClient
    private lateinit var token: String

    private fun createProductCard(
        context: Context,
        relativeLayout: LinearLayout,
        skuDetails: SkuDetails?
    ) {
        val cardView = CardView(context)
        val linearLayout = LinearLayout(context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayout.gravity = Gravity.CENTER_HORIZONTAL
        linearLayout.layoutParams = LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        linearLayout.orientation = LinearLayout.VERTICAL


        cardView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardView.radius = 30F
        cardView.useCompatPadding = true
        cardView.setPadding(25, 100, 25, 100)
        cardView.setCardBackgroundColor(Color.WHITE)
        cardView.maxCardElevation = 30F
        cardView.maxCardElevation = 6F

        val productNameTextView = TextView(context)
        productNameTextView.layoutParams = layoutParams
        productNameTextView.text = skuDetails!!.title
        productNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30F)
        productNameTextView.setTextColor(Color.WHITE)
        productNameTextView.setBackgroundColor(getColor(R.color.skylaski_red))
        productNameTextView.setPadding(25, 25, 25, 25)
        productNameTextView.gravity = Gravity.CENTER

        val productDescriptionTextView = TextView(context)
        productDescriptionTextView.layoutParams = layoutParams
        productDescriptionTextView.text = skuDetails.description
        productDescriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18F)
        productDescriptionTextView.setTextColor(Color.BLACK)
        productDescriptionTextView.setPadding(25, 25, 25, 25)
        productDescriptionTextView.gravity = Gravity.CENTER

        //Log.i(mTag,"Subscription Period: "+skuDetails.subscriptionPeriod)
        var monthlyPrice = 0
        var monthlyPriceDouble: Double = 0.0
        if(skuDetails.subscriptionPeriod == "P1Y"){
            monthlyPrice = skuDetails.priceAmountMicros.toInt() / 12
            monthlyPriceDouble = monthlyPrice.toDouble() / 1000000
        }

        val monthlyPriceTextView = TextView(context)
        monthlyPriceTextView.layoutParams = layoutParams
        monthlyPriceTextView.text = "$"+"%.2f".format(monthlyPriceDouble)+" "+getString(R.string.per_month)
        monthlyPriceTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24F)
        monthlyPriceTextView.setTextColor(Color.BLACK)
        monthlyPriceTextView.setPadding(25, 25, 25, 25)
        monthlyPriceTextView.gravity = Gravity.CENTER

        val billingNoticeTextView = TextView(context)
        billingNoticeTextView.layoutParams = layoutParams
        billingNoticeTextView.text = "*"+skuDetails.price +" "+getString(R.string.billed_annually)
        billingNoticeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14F)
        billingNoticeTextView.setTextColor(Color.BLACK)
        billingNoticeTextView.setPadding(25, 25, 25, 25)
        billingNoticeTextView.gravity = Gravity.CENTER

        val signUpButton = Button(context)
        signUpButton.layoutParams = LinearLayout.LayoutParams(512, LinearLayout.LayoutParams.WRAP_CONTENT)
        signUpButton.text = getString(R.string.sign_up)
        signUpButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 23F)
        signUpButton.setTextColor(Color.BLACK)
        signUpButton.setPadding(25,25,25,25)
        signUpButton.setOnClickListener {


        // An activity reference from which the billing flow will be launched.
        val activity: Activity = this
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        val hashedToken = HashUtils.sha1(token)
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .setObfuscatedAccountId(hashedToken)
            .build()
            billingClient.launchBillingFlow(activity, flowParams).responseCode
        }

        linearLayout.addView(productNameTextView)
        linearLayout.addView(productDescriptionTextView)
        linearLayout.addView(monthlyPriceTextView)
        linearLayout.addView(billingNoticeTextView)
        linearLayout.addView(signUpButton)


        cardView.addView(linearLayout)

        relativeLayout.addView(cardView)
    }

    fun querySkuDetails(
        billingClient: BillingClient,
        coroutineScope: CoroutineScope,
        context: Context
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val skuList = ArrayList<String>()
            skuList.add("1")
            skuList.add("3")
            skuList.add("4")

            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

            billingClient.querySkuDetailsAsync(params.build()) { _, skuDetailsList ->
                skuDetailsList!!.forEach {
                    //Log.i(mTag, "Title: " + it.title.toString())
                    //Log.i(mTag, "SKU: " + it.sku.toString())
                    createProductCard(context,findViewById<LinearLayout>(R.id.skusLinearLayout), it)

                }

            }



        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        coroutine = CoroutineScope(Job() + Dispatchers.Main.immediate)
        mContext = applicationContext

        // This activity is only ever called with a proper Intent
        val deepLinkIntent: Intent = intent
        val pattern = kotlin.text.Regex("^skylaski:\\/\\/www0\\.skylaski\\.com\\/NoAccount\\?token=(.*)$")
        val matchResult = pattern.find(deepLinkIntent.dataString.toString())
        token = matchResult!!.groupValues[1]

        //Log.i(mTag,"User Token: $token")

        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.

                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK){
                    //Log.i(mTag,"Found a Purchase: "+billingResult.responseCode.toString())
                    //Log.i(mTag,"Purchase Token: "+purchases!![0].purchaseToken.toString())
                    //Log.i(mTag,"Product ID: "+ purchases[0].sku.toString())

                    //WGMApi.createCheckout(token, purchases!![0].purchaseToken, purchases!![0].sku)
                    Toast.makeText(applicationContext,getString(R.string.thank_you),Toast.LENGTH_LONG)

                    val shortDelay = 3500; // 3.5 seconds
                    Thread.sleep(shortDelay.toLong())

                    // Server should be caught up by now. Nothing happens until we can find our purchase on Skylaski's servers.
                    // Let's try to create our checkout.
                    if(WGMApi.googleCheckout(token, purchases!![0].purchaseToken, purchases[0].sku)){
                        // confirm purchase with google
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchases[0].purchaseToken)
                            coroutine.launch(Dispatchers.IO) {
                                billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                            }

                        // checkout was successful, vpn plan setup and clients are available
                        // This is where we turn on the VPN
                        // Setup encrypted preferences
                        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
                            "secret_shared_prefs",
                            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                            applicationContext,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                        // Store the user token
                        sharedPreferences.edit().putString("user_token",token).apply()

                        // Start the client
                        val clientIntent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(CLIENT_DEEP_LINK_PREFIX))
                        clientIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        clientIntent.putExtra("TOKEN", sharedPreferences.getString("user_token",""))

                        startActivity(clientIntent)
                        finish()

                    }
                    else{
                        Toast.makeText(applicationContext,getString(R.string.checkout_failed),Toast.LENGTH_LONG).show()
                    }

                }
                else{
                    Toast.makeText(applicationContext,getString(R.string.no_purchase_made),Toast.LENGTH_SHORT).show()
                }


            }

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    //Log.i(mTag, "BillingClient is Ready")

                    // Before offering an item for sale, check that the user does not already own the item.
                    // If the user has a consumable that is still in their item library,
                    // they must consume the item before they can buy it again.


                    // Get products
                    querySkuDetails(billingClient, coroutine, applicationContext)


                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Toast.makeText(
                    applicationContext,
                    getString(R.string.billing_service_disconnected),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

    }
}