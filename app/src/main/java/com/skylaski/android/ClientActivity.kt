package com.skylaski.android

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.switchmaterial.SwitchMaterial
import com.skylaski.R
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
            tunnelManager = TunnelManager(applicationContext,wgBackend,tunnel)
        })
        backendThread.start()
        while(backendThread.isAlive){
            run{
                Toast.makeText(applicationContext,"Starting Backend",Toast.LENGTH_SHORT).show()
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

        // Check if user already has a configuration
        Toast.makeText(applicationContext, "Checking Config", Toast.LENGTH_SHORT).show()
        if (WGMApi.checkClientConfig(sharedPreferences)) {
            findViewById<TextView>(R.id.alertText).text = ""

            // Set the connection toggle to disconnected by default
            val connectionSwitch = findViewById<SwitchMaterial>(R.id.connectionSwitch)
            connectionSwitch.isChecked = false
            connectionSwitch.text = "Connect"
            if (sharedPreferences.getBoolean("is_connected", false)) {
                connectionSwitch.isChecked = true
                connectionSwitch.text = "Connected"
            }

            // Set the DNS toggle
            val dnsSwitch = findViewById<SwitchMaterial>(R.id.dnsSwitch)
            dnsSwitch.isChecked = true
            dnsSwitch.text = "Blocking Trackers"
            if (sharedPreferences.getString("device_dns", DEFAULT_DNS_BLOCKING) != DEFAULT_DNS_BLOCKING) {
                dnsSwitch.isChecked = false
                dnsSwitch.text = "Block Trackers"
            }

            getLocations(sharedPreferences)

        } else {
            // Client should be created via checkClientConfig if it didn't exist
            // disable everything but the alert text
            findViewById<TextView>(R.id.titleText).isEnabled = false
            findViewById<Spinner>(R.id.locationSpinner).isEnabled = false
            findViewById<SwitchMaterial>(R.id.connectionSwitch).isEnabled = false
            findViewById<SwitchMaterial>(R.id.dnsSwitch).isEnabled = false
            findViewById<TextView>(R.id.alertText)
                    .setText(R.string.max_device_alert)
        }
        findViewById<TextView>(R.id.alertText)
            .setText("Unique ID: "+sharedPreferences.getString("local_uid","Unknown").toString())
    }

    private fun getLocations(sharedPreferences: SharedPreferences){
        // getAvailable Locations and store them

        if(sharedPreferences.getString("locationJSON",null) == null) {
            Toast.makeText(applicationContext,"Getting Locations",Toast.LENGTH_SHORT).show()
            WGMApi.getLocations(sharedPreferences)
        }

        val locationList: ArrayList<String> = ArrayList()
        val locationSavedArray = sharedPreferences.getString("locationJSON",null)
        val locationJSONArray = JSONArray(locationSavedArray)
        for (i in 0 until locationJSONArray.length()){
            val tempJSONObject = JSONObject(locationJSONArray[i].toString())
            locationList.add(tempJSONObject.getString("name"))
        }

        val spinner = findViewById<Spinner>(R.id.locationSpinner)
        val arrAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, locationList)
        spinner.adapter = arrAdapter
        // Set the spinner to client's configured state
        for(i in 0 until locationJSONArray.length()){
            val tempJSON = locationJSONArray.getJSONObject(i)
            if(tempJSON.getString("loc_uid") == sharedPreferences.getString("loc_uid",null)){
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
        Toast.makeText(applicationContext,"Detaching",Toast.LENGTH_SHORT).show()
        WGMApi.detachClient(sharedPreferences)

        // update sharedPreferences
        sharedPreferences.edit().putBoolean("isAttached",false).apply()
        tunnelManager.disconnect(sharedPreferences)
        sharedPreferences.edit().putBoolean("is_connected", false).apply()
        sharedPreferences.edit().putString("wireguard_client_config",null).apply()

        return true
    }

    fun attachLocation(locationUID: String, sharedPreferences: SharedPreferences) : Boolean{
        // attempt to attach the client to a GW server at the identified location

        Toast.makeText(applicationContext,"Attaching",Toast.LENGTH_SHORT).show()
        WGMApi.attachClient(sharedPreferences,locationUID)

        // We just attached a new location, clear config and recheck client config
        sharedPreferences.edit().putBoolean("isAttached",true).apply()
        // connect by default

        sharedPreferences.edit().putString("wireguard_client_config",null).apply()
        Toast.makeText(applicationContext,"Checking Configuration",Toast.LENGTH_SHORT).show()
        WGMApi.checkClientConfig(sharedPreferences)

        tunnelManager.connect(sharedPreferences)
        sharedPreferences.edit().putBoolean("is_connected",true).apply()

        return true

    }
    fun onConnectSwitch(view: View){
        val connectionSwitch = view.findViewById<SwitchMaterial>(R.id.connectionSwitch)

        if(connectionSwitch.isChecked){
            // Connect to the VPN
            connectionSwitch.text = "Connected"
            Toast.makeText(applicationContext,"Connecting",Toast.LENGTH_SHORT).show()

            tunnelManager.connect(mSharedPreferences)

        }
        else {
            // Disconnect from VPN
            connectionSwitch.text = "Connect"
            Toast.makeText(applicationContext,"Disconnecting",Toast.LENGTH_SHORT).show()

            tunnelManager.disconnect(mSharedPreferences)
        }

    }

    fun onDNSSwitch(view: View) {
        val dnsSwitch = view.findViewById<SwitchMaterial>(R.id.dnsSwitch)

        if(dnsSwitch.isChecked){
            mSharedPreferences.edit().putString("device_dns", DEFAULT_DNS_BLOCKING).apply()
            dnsSwitch.text="Blocking Trackers"

            // Now get the latest DNS server information
            val dnsResponse = WGMApi.getDNS(mSharedPreferences, DEFAULT_DNS_BLOCKING)

            // Let's update the config
            val wgTextConfig = mSharedPreferences.getString("wireguard_client_config","")
            val newTextConfig = wgTextConfig!!.replace("(DNS = \\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3})".toRegex(), "DNS = $dnsResponse")
            mSharedPreferences.edit().putString("wireguard_client_config",newTextConfig).apply()

            // Now disconnect and reconnect the tunnel
            tunnelManager.disconnect(mSharedPreferences)
            tunnelManager.connect(mSharedPreferences)
        }
        else{
            mSharedPreferences.edit().putString("device_dns", DEFAULT_DNS_NO_BLOCKING).apply()
            dnsSwitch.text="Block Trackers"

            // Now get the latest DNS server information
            val dnsResponse = WGMApi.getDNS(mSharedPreferences, DEFAULT_DNS_NO_BLOCKING)

            // Let's update the config
            val wgTextConfig = mSharedPreferences.getString("wireguard_client_config","")
            val newTextConfig = wgTextConfig!!.replace("(DNS = \\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3})".toRegex(), "DNS = $dnsResponse")
            mSharedPreferences.edit().putString("wireguard_client_config",newTextConfig).apply()

            // Now disconnect and reconnect the tunnel
            tunnelManager.disconnect(mSharedPreferences)
            tunnelManager.connect(mSharedPreferences)
        }
    }

    fun onLogout(view: View){

        // We should kill all running tunnels before clearing the preferences
        tunnelManager.disconnect(mSharedPreferences)

        // We should also delete the client off the backend.
        // first detach

        // This is now handled by the deleteClient() command
        //Toast.makeText(applicationContext,"Detaching Client", Toast.LENGTH_SHORT).show()
        //WGMApi.detachClient(mSharedPreferences)

        Toast.makeText(applicationContext,"Deleting Client", Toast.LENGTH_SHORT).show()
        WGMApi.deleteClient(mSharedPreferences)

        mSharedPreferences.edit().clear().apply()
        finish()
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