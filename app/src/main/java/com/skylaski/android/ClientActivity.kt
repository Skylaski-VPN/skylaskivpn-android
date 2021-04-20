package com.skylaski.android

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.skylaski.R
import com.skylaski.R.drawable.my_button
import com.skylaski.android.wgm.DEFAULT_DNS_BLOCKING
import com.skylaski.android.wgm.DEFAULT_DNS_NO_BLOCKING
import com.skylaski.android.wgm.WGMApi
import com.skylaski.android.wgm.wireguard.MyTunnel
import com.skylaski.android.wgm.wireguard.TunnelManager
import com.wireguard.android.backend.GoBackend
import org.json.JSONArray
import org.json.JSONObject

private const val TUNNEL_NAME = "wg0"

class ClientActivity : AppCompatActivity() {
    private val mTAG = "ClientActivity"
    private lateinit var wgBackend: GoBackend
    private lateinit var tunnelManager: TunnelManager
    private lateinit var mSharedPreferences: SharedPreferences
    private var tunnel = MyTunnel(TUNNEL_NAME)

    private fun createCard(
        context: Context,
        relativeLayout: LinearLayout,
        type: String,
        sharedPreferences: SharedPreferences
    ) {

        if(type == "connection"){
            // Define CardView
            val cardView = CardView(context)
            val linearLayout = LinearLayout(context)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Define Layout
            linearLayout.gravity = Gravity.CENTER_HORIZONTAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            linearLayout.orientation = LinearLayout.VERTICAL

            // Set Card Params
            cardView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardView.radius = 30F
            cardView.useCompatPadding = true
            cardView.setPadding(25, 100, 25, 100)
            cardView.setCardBackgroundColor(Color.WHITE)
            cardView.maxCardElevation = 30F
            cardView.maxCardElevation = 6F

            // Card Title
            val chooseLocationTextView = TextView(context)
            chooseLocationTextView.layoutParams = layoutParams
            chooseLocationTextView.text = getString(R.string.choose_location)
            chooseLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30F)
            chooseLocationTextView.setTextColor(Color.WHITE)
            chooseLocationTextView.setBackgroundColor(getColor(R.color.skylaski_red))
            chooseLocationTextView.setPadding(25, 25, 25, 25)
            // Location Spinner
            val locationSpinner = Spinner(context)
            locationSpinner.layoutParams = layoutParams
            locationSpinner.setPadding(25, 25, 25, 25)

            // Connection Switch
            val connectionSwitch = Switch(context)
            connectionSwitch.layoutParams = layoutParams
            connectionSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30F)
            connectionSwitch.setTextColor(Color.BLACK)
            connectionSwitch.highlightColor = getColor(R.color.skylaski_blue)
            connectionSwitch.setPadding(25, 25, 25, 25)
            connectionSwitch.setOnClickListener(View.OnClickListener {
                onConnectSwitch(
                    connectionSwitch
                )
            })

            // DNS Switch
            val dnsSwitch = Switch(context)
            dnsSwitch.layoutParams = layoutParams
            dnsSwitch.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30F)
            dnsSwitch.setTextColor(Color.BLACK)
            dnsSwitch.highlightColor = getColor(R.color.skylaski_blue)
            dnsSwitch.setPadding(25, 25, 25, 25)
            dnsSwitch.setOnClickListener(View.OnClickListener { onDNSSwitch(dnsSwitch) })

            // Bottom Text
            val alertTextView = TextView(context)
            alertTextView.layoutParams = layoutParams
            alertTextView.text = getString(R.string.choose_location)
            alertTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18F)
            alertTextView.setTextColor(Color.BLACK)
            alertTextView.setBackgroundColor(Color.WHITE)
            alertTextView.setPadding(25, 25, 25, 25)
            chooseLocationTextView.gravity = Gravity.CENTER

            // Set Connection and DNS Switch
            // Check if user already has a configuration
            Toast.makeText(applicationContext, getString(R.string.loading), Toast.LENGTH_SHORT).show()
            if (WGMApi.checkClientConfig(sharedPreferences)) {
                alertTextView.text = ""

                // Set the connection toggle to disconnected by default
                connectionSwitch.isChecked = false
                connectionSwitch.text = getString(R.string.connect)
                if (sharedPreferences.getBoolean("is_connected", false)) {
                    connectionSwitch.isChecked = true
                    connectionSwitch.text = getString(R.string.connected)
                }

                // Set the DNS toggle
                dnsSwitch.isChecked = true
                dnsSwitch.text = getString(R.string.blocking_trackers)
                if (sharedPreferences.getString("device_dns", DEFAULT_DNS_BLOCKING) != DEFAULT_DNS_BLOCKING) {
                    dnsSwitch.isChecked = false
                    dnsSwitch.text = getString(R.string.block_trackers)
                }
                // Fill the Location Spinner
                getLocations(sharedPreferences, locationSpinner)

            } else {
                // Client should be created via checkClientConfig if it didn't exist
                // disable everything but the alert text
                chooseLocationTextView.isEnabled = false
                locationSpinner.isEnabled = false
                connectionSwitch.text = getString(R.string.connect)
                connectionSwitch.isEnabled = false
                dnsSwitch.text = getString(R.string.block_trackers)
                dnsSwitch.isEnabled = false
                alertTextView.text = getString(R.string.max_device_alert)
            }
            //alertTextView.text = getString(R.string.device_id)+": "+sharedPreferences.getString("local_uid","Unknown").toString()

            // Add All to Layout
            linearLayout.addView(chooseLocationTextView)
            linearLayout.addView(locationSpinner)
            linearLayout.addView(connectionSwitch)
            linearLayout.addView(dnsSwitch)
            linearLayout.addView(alertTextView)
            // Add Layout to Card
            cardView.addView(linearLayout)
            // Add Card to Parent
            relativeLayout.addView(cardView)
        }
        else if(type == "account"){

            // CardView
            val cardView = CardView(context)
            val linearLayout = LinearLayout(context)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Linear Layout
            linearLayout.gravity = Gravity.CENTER_HORIZONTAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            linearLayout.orientation = LinearLayout.VERTICAL

            // Cardview Settings
            cardView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardView.radius = 30F
            cardView.useCompatPadding = true
            cardView.setPadding(25, 100, 25, 100)
            cardView.setCardBackgroundColor(Color.WHITE)
            cardView.maxCardElevation = 30F
            cardView.maxCardElevation = 6F

            // Card Title Text
            val accountTitleTextView = TextView(context)
            accountTitleTextView.gravity = Gravity.CENTER_HORIZONTAL
            accountTitleTextView.layoutParams = layoutParams
            accountTitleTextView.text = getString(R.string.account)
            accountTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30F)
            accountTitleTextView.setTextColor(Color.WHITE)
            accountTitleTextView.setBackgroundColor(getColor(R.color.skylaski_red))
            accountTitleTextView.setPadding(25, 25, 25, 25)

            // Account Details Text HTML Formatted
            val accountDetailsTextView = TextView(context)
            accountDetailsTextView.layoutParams = layoutParams
            accountDetailsTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18F)
            accountDetailsTextView.setTextColor(Color.BLACK)
            accountDetailsTextView.setBackgroundColor(Color.WHITE)
            accountDetailsTextView.setPadding(25, 25, 25, 25)

            // Get VPN Plan Details from API
            val plan = WGMApi.getPlan(sharedPreferences)
            //Log.i(mTAG, "PLAN RESULTS: " + plan.toString())
            // Fill Values
            val planDetailString = "<h3>"+plan.getJSONObject("result").getJSONObject("product").getString(
                "name"
            )+"</h3>\n"+
                    "<p><b>Expires: </b>"+plan.getJSONObject("result").getJSONObject("plan").getString(
                "expiration"
            )+"</p>\n"+
                    //"<p><b>Total Devices: </b>"+plan.getJSONObject("result").getJSONObject("product").getString("total_clients_per_user")+"</p>\n"+
                    //"<p><b>Total Users: </b>"+plan.getJSONObject("result").getJSONObject("product").getString("total_users")+"</p>\n"+
                    "<p><b>Device ID: </b>"+sharedPreferences.getString("local_uid", "")+"</p>\n"
            // Convert HTML
            accountDetailsTextView.text = HtmlCompat.fromHtml(
                planDetailString,
                FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH
            )

            // Add All to Layout
            linearLayout.addView(accountTitleTextView)
            linearLayout.addView(accountDetailsTextView)

            // Add layout to CardView
            cardView.addView(linearLayout)
            // Add Card to parent
            relativeLayout.addView(cardView)
        }
        else if(type == "account_button"){
            // CardView
            val cardView = CardView(context)
            val linearLayout = LinearLayout(context)

            // Linear Layout
            linearLayout.gravity = Gravity.CENTER_HORIZONTAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            linearLayout.orientation = LinearLayout.VERTICAL

            // Cardview Settings
            cardView.layoutParams = LinearLayout.LayoutParams(
                1024,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardView.radius = 30F
            cardView.useCompatPadding = true
            cardView.setPadding(25, 100, 25, 100)
            cardView.setCardBackgroundColor(Color.WHITE)
            cardView.maxCardElevation = 30F
            cardView.maxCardElevation = 6F
            cardView.foregroundGravity = Gravity.CENTER_HORIZONTAL

            // Account Button
            val accountButton = Button(context)
            accountButton.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            accountButton.text = getString(R.string.profile)
            accountButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)
            accountButton.setTextColor(Color.WHITE)
            accountButton.background = getDrawable(my_button)
            accountButton.setPadding(0, 25, 0, 25)
            accountButton.setOnClickListener(View.OnClickListener {
                account()
            })

            // Add All to Layout
            linearLayout.addView(accountButton)

            // Add layout to CardView
            cardView.addView(linearLayout)
            // Add Card to parent
            relativeLayout.addView(cardView)
        }
        else if(type == "logout_button"){
            // CardView
            val cardView = CardView(context)
            val linearLayout = LinearLayout(context)

            // Linear Layout
            linearLayout.gravity = Gravity.CENTER_HORIZONTAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            linearLayout.orientation = LinearLayout.VERTICAL

            // Cardview Settings
            cardView.layoutParams = LinearLayout.LayoutParams(
                1024,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardView.radius = 30F
            cardView.useCompatPadding = true
            cardView.setPadding(25, 100, 25, 100)
            cardView.setCardBackgroundColor(Color.WHITE)
            cardView.maxCardElevation = 30F
            cardView.maxCardElevation = 6F
            cardView.foregroundGravity = Gravity.CENTER_HORIZONTAL

            // Logout Button
            var logOutButton = Button(context)
            logOutButton.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            logOutButton.text = getString(R.string.log_out)
            logOutButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)
            logOutButton.setTextColor(Color.WHITE)
            logOutButton.setPadding(0, 25, 0, 25)
            logOutButton.background = getDrawable(my_button)
            logOutButton.setOnClickListener(View.OnClickListener {
                onLogout()
            })

            // Add All to Layout
            linearLayout.addView(logOutButton)


            // Add layout to CardView
            cardView.addView(linearLayout)
            // Add Card to parent
            relativeLayout.addView(cardView)
        }
        else if(type == "test_button"){
            // CardView
            val cardView = CardView(context)
            val linearLayout = LinearLayout(context)

            // Linear Layout
            linearLayout.gravity = Gravity.CENTER_HORIZONTAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            linearLayout.orientation = LinearLayout.VERTICAL

            // Cardview Settings
            cardView.layoutParams = LinearLayout.LayoutParams(
                1024,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardView.radius = 30F
            cardView.useCompatPadding = true
            cardView.setPadding(25, 100, 25, 100)
            cardView.setCardBackgroundColor(Color.WHITE)
            cardView.maxCardElevation = 30F
            cardView.maxCardElevation = 6F
            cardView.foregroundGravity = Gravity.CENTER_HORIZONTAL

            // Logout Button
            var logOutButton = Button(context)
            logOutButton.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            logOutButton.text = getString(R.string.test)
            logOutButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20F)
            logOutButton.setTextColor(Color.WHITE)
            logOutButton.setPadding(0, 25, 0, 25)
            logOutButton.background = getDrawable(my_button)
            logOutButton.setOnClickListener(View.OnClickListener {
                onTest()
            })

            // Add All to Layout
            linearLayout.addView(logOutButton)


            // Add layout to CardView
            cardView.addView(linearLayout)
            // Add Card to parent
            relativeLayout.addView(cardView)
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        // Decrypt the local sharedPreferences
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        mSharedPreferences = sharedPreferences

        // Setup the WireGuard GoBackend
        val backendThread = Thread(Runnable {
            wgBackend = GoBackend(applicationContext)
            GoBackend.setAlwaysOnCallback {
                tunnelManager.connect(sharedPreferences)
            }
            tunnelManager = TunnelManager(applicationContext, wgBackend, tunnel)
        })
        backendThread.start()
        while(backendThread.isAlive){
            run{
                Toast.makeText(applicationContext, getString(R.string.starting), Toast.LENGTH_SHORT).show()
            }
        }


        // Check for proper permissions to setup a secure tunnel
        val permissionActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { tunnelManager.toggleTunnelWithPermissionsResult() }
        val permissionIntent = GoBackend.VpnService.prepare(applicationContext)
        if(permissionIntent != null){
            permissionActivityResultLauncher.launch(permissionIntent)
        }


        // This activity is only ever called with a proper Intent
        val deepLinkIntent: Intent = intent
        var token = ""
        if(deepLinkIntent.hasExtra("TOKEN")) {
            token = deepLinkIntent.getStringExtra("TOKEN").toString()
        }
        else {
            val pattern = kotlin.text.Regex("^skylaski:\\/\\/www0\\.skylaski\\.com\\/skylaskivpnapp\\?token=(.*)$")
            val matchResult = pattern.find(deepLinkIntent.dataString.toString())
            token = matchResult!!.groupValues[1]
        }

        // Found Token, apply
        sharedPreferences.edit().putString("user_token", token).apply()

        // Draw Connection Dialog
        createCard(
            applicationContext,
            findViewById(R.id.cardsLinearLayout),
            "connection",
            sharedPreferences
        )

        // Draw Account Dialog
        createCard(
            applicationContext,
            findViewById(R.id.cardsLinearLayout),
            "account",
            sharedPreferences
        )

        // Draw Account Button
        createCard(
            applicationContext,
            findViewById(R.id.cardsLinearLayout),
            "account_button",
            sharedPreferences
        )
        // Draw Test Button
        createCard(applicationContext,findViewById(R.id.cardsLinearLayout),"test_button",sharedPreferences)

        // Draw Log Out Button
        createCard(
            applicationContext,
            findViewById(R.id.cardsLinearLayout),
            "logout_button",
            sharedPreferences
        )

    }

    private fun onTest(){
        val newIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www0.skylaski.com/checkip"))
        startActivity(newIntent)
    }

    private fun getLocations(sharedPreferences: SharedPreferences, spinner: Spinner){
        // getAvailable Locations and store them

        if(sharedPreferences.getString("locationJSON", null) == null) {
            //Toast.makeText(applicationContext,"Getting Locations",Toast.LENGTH_SHORT).show()
            WGMApi.getLocations(sharedPreferences)
        }

        val locationList: ArrayList<String> = ArrayList()
        val locationSavedArray = sharedPreferences.getString("locationJSON", null)
        val locationJSONArray = JSONArray(locationSavedArray)
        for (i in 0 until locationJSONArray.length()){
            val tempJSONObject = JSONObject(locationJSONArray[i].toString())
            locationList.add(tempJSONObject.getString("name"))
        }

        //val spinner = findViewById<Spinner>(R.id.locationSpinner)
        val arrAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            locationList
        )
        spinner.adapter = arrAdapter
        // Set the spinner to client's configured state
        for(i in 0 until locationJSONArray.length()){
            val tempJSON = locationJSONArray.getJSONObject(i)
            if(tempJSON.getString("loc_uid") == sharedPreferences.getString("loc_uid", null)){
                spinner.setSelection(i)
            }
        }

        //spinner.setSelection()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                // get Location's unique_id and attach client
                val locationName = locationList[position]
                for (i in 0 until locationJSONArray.length()) {
                    val tempJSONObject = JSONObject(locationJSONArray[i].toString())
                    var locationUID = ""
                    if (locationName == tempJSONObject.getString("name")) {
                        // Found the location UID
                        locationUID = tempJSONObject.getString("loc_uid")
                        val currentLocation = sharedPreferences.getString("loc_uid", null)
                        // Is the user changing their location?
                        if (currentLocation != locationUID) {
                            // First detach client from current location
                            detachLocation(sharedPreferences)
                            // Now attach client to new location
                            attachLocation(locationUID, sharedPreferences)
                            sharedPreferences.edit().putString("loc_uid", locationUID).apply()
                        }
                    }
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun detachLocation(sharedPreferences: SharedPreferences) : Boolean{
        Toast.makeText(applicationContext, "Disconnecting", Toast.LENGTH_SHORT).show()
        WGMApi.detachClient(sharedPreferences)

        // update sharedPreferences
        sharedPreferences.edit().putBoolean("isAttached", false).apply()
        tunnelManager.disconnect(sharedPreferences)
        sharedPreferences.edit().putBoolean("is_connected", false).apply()
        sharedPreferences.edit().putString("wireguard_client_config", null).apply()

        return true
    }

    fun attachLocation(locationUID: String, sharedPreferences: SharedPreferences) : Boolean{
        // attempt to attach the client to a GW server at the identified location

        Toast.makeText(applicationContext, getString(R.string.finding_server), Toast.LENGTH_SHORT).show()
        WGMApi.attachClient(sharedPreferences, locationUID)

        // We just attached a new location, clear config and recheck client config
        sharedPreferences.edit().putBoolean("isAttached", true).apply()
        // connect by default

        sharedPreferences.edit().putString("wireguard_client_config", null).apply()
        WGMApi.checkClientConfig(sharedPreferences)

        tunnelManager.connect(sharedPreferences)
        sharedPreferences.edit().putBoolean("is_connected", true).apply()

        return true

    }
    private fun onConnectSwitch(connectionSwitch: Switch){

        if(connectionSwitch.isChecked){
            // Connect to the VPN
            connectionSwitch.text = getString(R.string.connected)
            Toast.makeText(applicationContext, getString(R.string.connecting), Toast.LENGTH_SHORT).show()

            tunnelManager.connect(mSharedPreferences)

        }
        else {
            // Disconnect from VPN
            connectionSwitch.text = getString(R.string.connect)
            Toast.makeText(
                applicationContext,
                getString(R.string.disconnecting),
                Toast.LENGTH_SHORT
            ).show()

            tunnelManager.disconnect(mSharedPreferences)
        }

    }

    private fun onDNSSwitch(dnsSwitch: Switch) {

        if(dnsSwitch.isChecked){
            mSharedPreferences.edit().putString("device_dns", DEFAULT_DNS_BLOCKING).apply()
            dnsSwitch.text= getString(R.string.blocking_trackers)

            // Now get the latest DNS server information
            val dnsResponse = WGMApi.getDNS(mSharedPreferences, DEFAULT_DNS_BLOCKING)
            //Log.i(mTAG,"DNSRESPONSE "+dnsResponse.toString())

            // Let's update the config
            val wgTextConfig = mSharedPreferences.getString("wireguard_client_config", "")
            val newTextConfig = wgTextConfig!!.replace(
                "(DNS = \\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3})".toRegex(),
                "DNS = $dnsResponse"
            )
            mSharedPreferences.edit().putString("wireguard_client_config", newTextConfig).apply()

            // Now disconnect and reconnect the tunnel
            tunnelManager.disconnect(mSharedPreferences)
            tunnelManager.connect(mSharedPreferences)
        }
        else{
            mSharedPreferences.edit().putString("device_dns", DEFAULT_DNS_NO_BLOCKING).apply()
            dnsSwitch.text= getString(R.string.block_trackers)

            // Now get the latest DNS server information
            val dnsResponse = WGMApi.getDNS(mSharedPreferences, DEFAULT_DNS_NO_BLOCKING)
            //Log.i(mTAG,"DNSRESPONSE "+dnsResponse.toString())

            // Let's update the config
            val wgTextConfig = mSharedPreferences.getString("wireguard_client_config", "")
            val newTextConfig = wgTextConfig!!.replace(
                "(DNS = \\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3})".toRegex(),
                "DNS = $dnsResponse"
            )
            mSharedPreferences.edit().putString("wireguard_client_config", newTextConfig).apply()

            // Now disconnect and reconnect the tunnel
            tunnelManager.disconnect(mSharedPreferences)
            tunnelManager.connect(mSharedPreferences)
        }
    }

    private fun onLogout(){

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.log_out))
        alertDialogBuilder.setMessage(getString(R.string.logout_message))
        alertDialogBuilder.setNegativeButton("Cancel") {_, _ -> }
        alertDialogBuilder.setPositiveButton(getString(R.string.log_out)){ _, _ ->
            // We should kill all running tunnels before clearing the preferences
            tunnelManager.disconnect(mSharedPreferences)

            // We should also delete the client off the backend.
            // first detach

            // This is now handled by the deleteClient() command
            //Toast.makeText(applicationContext,"Detaching Client", Toast.LENGTH_SHORT).show()
            //WGMApi.detachClient(mSharedPreferences)

            Toast.makeText(applicationContext, getString(R.string.cleaning_up), Toast.LENGTH_SHORT).show()
            WGMApi.deleteClient(mSharedPreferences)

            mSharedPreferences.edit().clear().apply()
            finishAffinity()

        }
        alertDialogBuilder.show()






    }

    private fun account(){
        // load the default browser to take the user to their account page
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ACCOUNT_URI))
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}