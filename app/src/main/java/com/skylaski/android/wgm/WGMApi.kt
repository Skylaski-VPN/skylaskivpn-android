package com.skylaski.android.wgm

import android.content.SharedPreferences
import com.skylaski.android.wgm.wireguard.WGKeys
import org.json.JSONObject

public const val USER_API_ENDPOINT = "https://wgm0.skylaski.com/api/0.1/user/index.php"
public const val CLIENT_API_ENDPOINT = "https://wgm0.skylaski.com/api/0.1/client/index.php"
public const val DEFAULT_DNS_BLOCKING = "3"
public const val DEFAULT_DNS_NO_BLOCKING = "2"

public object WGMApi {

    private const val mTAG = "wgmApi"

    fun googleCheckout(token: String, purchaseCode: String, sku: String) : Boolean{
        var checkoutApiResponse = JSONObject()
        val createCheckoutReqBody = JSONObject().put("cmd","google_checkout")
        createCheckoutReqBody.put("purchaseCode",purchaseCode)
        createCheckoutReqBody.put("sku",sku)

        val requestThread = Thread(Runnable {
            try {
                // Get client information
                val response = ApiRequestHandler.requestPOST(
                    USER_API_ENDPOINT,
                    createCheckoutReqBody,
                    token
                )
                checkoutApiResponse = JSONObject("""$response""")
                // Log.i(mTAG,"Checkout Response: "+checkoutApiResponse.toString())
            } catch (ex: Exception) {
            }
        })
        requestThread.start()
        while (requestThread.isAlive) { // Make sure the API Calls finish before we move on
            run {

            }
        }

        return checkoutApiResponse.getBoolean("success")
    }

    fun getDNS(sharedPreferences: SharedPreferences,dnsType: String): String {
        var dnsResponse = ""
        var dnsApiResponse = JSONObject()
        val getDNSReqBody = JSONObject().put("cmd","get_dns")
        getDNSReqBody.put("client_uid",sharedPreferences.getString("client_uid",null))
        getDNSReqBody.put("client_token",sharedPreferences.getString("device_token",null))
        getDNSReqBody.put("dns_type",dnsType)

        val requestThread = Thread(Runnable {
            try {
                // Get client information
                val response = ApiRequestHandler.requestPOST(
                    CLIENT_API_ENDPOINT,
                    getDNSReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                dnsApiResponse = JSONObject("""$response""")

            } catch (ex: Exception) {
            }
        })
        requestThread.start()
        while (requestThread.isAlive) { // Make sure the API Calls finish before we move on
            run {

            }
        }

        dnsResponse = if(dnsApiResponse.getBoolean("success")) {
            val result = dnsApiResponse.getJSONObject("result")
            result.getString("dns_server")
        } else{
            "FAIL"
        }

        return dnsResponse
    }

    fun getClients(sharedPreferences: SharedPreferences) : JSONObject {
        var clientsResponse = JSONObject()
        val getClientsReqBody = JSONObject().put("cmd", "get_clients")

        val requestThread = Thread(Runnable {
            try {
                // Get client information
                val response = ApiRequestHandler.requestPOST(
                    USER_API_ENDPOINT,
                    getClientsReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                clientsResponse = JSONObject("""$response""")

            } catch (ex: Exception) {
            }
        })
        requestThread.start()
        while (requestThread.isAlive) { // Make sure the API Calls finish before we move on
            run {

            }
        }

        return clientsResponse
    }

    fun getUpgrades(sharedPreferences: SharedPreferences) :JSONObject {
        var upgradeResponse = JSONObject()
        val getUpgradeReqBody = JSONObject().put("cmd", "get_upgrades")

        val requestThread = Thread(Runnable {
            try {
                // Get available upgrades
                val response = ApiRequestHandler.requestPOST(
                    USER_API_ENDPOINT,
                    getUpgradeReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                upgradeResponse = JSONObject("""$response""")
            } catch (ex: Exception){
            }
        })
        requestThread.start()
        while(requestThread.isAlive){
            run {

            }
        }

        return upgradeResponse
    }

    fun getPlan(sharedPreferences: SharedPreferences) : JSONObject{
        var planResponse = JSONObject()
        val getPlanReqBody = JSONObject().put("cmd", "get_plan")

        val requestThread = Thread(Runnable {
            try {
                // Get Plan Information
                val response = ApiRequestHandler.requestPOST(
                    USER_API_ENDPOINT,
                    getPlanReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                planResponse = JSONObject("""$response""")
            } catch (ex: Exception){
            }
        })
        requestThread.start()
        while(requestThread.isAlive){
            run{

            }
        }

        return planResponse
    }
    fun getUser(sharedPreferences: SharedPreferences) : JSONObject {
        var userResponse = JSONObject()
        val getUserReqBody = JSONObject().put("cmd", "get_user")

        val requestThread = Thread(Runnable {
            try {
                // Get User Information
                val response = ApiRequestHandler.requestPOST(
                    USER_API_ENDPOINT,
                    getUserReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                userResponse = JSONObject("""$response""")
            } catch (ex: Exception) {
            }
        })
        requestThread.start()
        while(requestThread.isAlive){
            run {

            }
        }

        return userResponse
    }
    fun checkClientConfig(sharedPreferences: SharedPreferences) : Boolean {
        /*
            This is where we do most of the work.
            1. Check if the client already has a configuration; aka already attached to a location
            2. If not, the client could still exist on the backend, so we set the proper values in sharedPreferences
            3. Lastly if the client does not exist on the backend we try to create it. Return false, if we fail. This could typically be because the user has already maxed out their client.
         */
        val success: Boolean
        var clientResponse = JSONObject()
        val getClientReqBody = JSONObject().put("cmd", "get_client_config")
        getClientReqBody.put("local_uid", sharedPreferences.getString("local_uid", null))

        val mRequestThread = Thread(Runnable {
            try {
                // Get Available Locations
                val response = ApiRequestHandler.requestPOST(
                    CLIENT_API_ENDPOINT,
                    getClientReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                clientResponse = JSONObject("""$response""")
            } catch (ex: Exception) {
            }
        })
        mRequestThread.start()
        while (mRequestThread.isAlive) {
            run {

            }
        }

        if (!clientResponse.getBoolean("success")) {

            // Client has no configuration, but it could have a token
            if (clientResponse.getString("message") == "Client has no config") {

                // Get Client Token when it exits
                val resultObj = clientResponse.getJSONObject("result")
                sharedPreferences.edit().putString("device_token", resultObj.getString("client_token")).apply()
                sharedPreferences.edit().putString("client_uid", resultObj.getString("client_uid")).apply()

                success = true

            } else {

                // Client does not exist, we'll need to create it.
                val clientJSON = createClient(sharedPreferences)

                if (!clientJSON.getBoolean("max_clients")) {

                    // we successfully created the client and got a device_token and client_uid in response
                    sharedPreferences.edit().putString("device_token", clientJSON.getString("client_token")).apply()
                    sharedPreferences.edit().putString("client_uid", clientJSON.getString("client_uid")).apply()
                    success = true

                } else {
                    // Client is over account's Max Clients available
                    success = false
                }
            }

        } else {
            // Client has a configuration, let's make sure sharedPreferences are set
            val result = clientResponse.getJSONObject("result")
            sharedPreferences.edit().putString("device_token", result.getString("client_token")).apply()
            sharedPreferences.edit().putString("client_uid", result.getString("client_uid")).apply()

            // The config comes without Private Key set. That stays local to the device. We need to insert it!
            // if the client has re-installed the app they'll get a new keyset, we need to check if sharedPreferences already has a value, otherwise we need to delete and recreate the client.
            if(sharedPreferences.getString("wg_priv_key","") == ""){
                // No private key stored, recreate the client
                deleteClient(sharedPreferences)
                val clientJSON = createClient(sharedPreferences)
                success = if (!clientJSON.getBoolean("max_clients")) {

                    // we successfully created the client and got a device_token and client_uid in response
                    sharedPreferences.edit().putString("device_token", clientJSON.getString("client_token")).apply()
                    sharedPreferences.edit().putString("client_uid", clientJSON.getString("client_uid")).apply()
                    true

                } else {
                    // Client is over account's Max Clients available
                    false
                }
            } else {
                val wgTextConfig = result.getString("config")
                val newConfig = wgTextConfig.replace(
                    "<PRIVATEKEY>",
                    sharedPreferences.getString("wg_priv_key", null)!!
                )
                sharedPreferences.edit().putString("wireguard_client_config", newConfig).apply()
                success = true
            }

        }

        return success
    }

    private fun createClient(sharedPreferences: SharedPreferences) : JSONObject {

        var mClientResponse = JSONObject()
        // Generate WireGuard keys for this client
        val keys = WGKeys()
        keys.genKeys(sharedPreferences)

        val getClientReqBody = JSONObject().put("cmd", "create_client")
        getClientReqBody.put("local_uid", sharedPreferences.getString("local_uid", null))
        getClientReqBody.put("name", sharedPreferences.getString("local_uid", null))
        getClientReqBody.put("pub_key", sharedPreferences.getString("wg_pub_key",null))

        // Set client's default DNS setting
        getClientReqBody.put("dns_type", DEFAULT_DNS_BLOCKING)
        sharedPreferences.edit().putString("device_dns", DEFAULT_DNS_BLOCKING).apply()

        // Send the create_client command to API
        val mRequestThread = Thread(Runnable {
            try{
                val response = ApiRequestHandler.requestPOST(
                    CLIENT_API_ENDPOINT,
                    getClientReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                //Log.i(mTAG,"Create Client Response: "+mClientResponse.toString())
                mClientResponse = JSONObject("""$response""")

            } catch (ex: Exception){
            }
        })
        mRequestThread.start()
        while(mRequestThread.isAlive){
            run{

            }
        }

        return mClientResponse.getJSONObject("result")
    }

    public fun getLocations(sharedPreferences: SharedPreferences) : Boolean {
        val getLocationReqBody = JSONObject().put("cmd", "get_locations")
        getLocationReqBody.put("client_uid",sharedPreferences.getString("client_uid",null))
        getLocationReqBody.put("client_token",sharedPreferences.getString("device_token",null))
        var success = false

        val mRequestThread = Thread(Runnable {
            try {
                // Get Available Locations
                val response = ApiRequestHandler.requestPOST(
                    CLIENT_API_ENDPOINT,
                    getLocationReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                val locationResponse = JSONObject("""$response""")
                val locationJSON = locationResponse.getJSONArray("result")
                sharedPreferences.edit().putString("locationJSON",locationJSON.toString()).apply()
                success = true
            } catch (ex: Exception) {
            }
        })
        mRequestThread.start()
        while (mRequestThread.isAlive) {
            run {

            }
        }

        return success
    }

    public fun attachClient(sharedPreferences: SharedPreferences, locationUID: String) : Boolean{
        var success = false
        val attachReqBody = JSONObject().put("cmd", "attach_location")
        attachReqBody.put("client_uid",sharedPreferences.getString("client_uid",null))
        attachReqBody.put("client_token",sharedPreferences.getString("device_token",null))
        attachReqBody.put("loc_uid",locationUID)

        val mRequestThread = Thread(Runnable {
            try {
                // Get Available Locations
                val response = ApiRequestHandler.requestPOST(
                    CLIENT_API_ENDPOINT,
                    attachReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )

                val attachResponse = JSONObject("""$response""")
            } catch (ex: Exception) {
                success = true
            }
        })
        mRequestThread.start()
        while(mRequestThread.isAlive){
            run{

            }
        }
        checkClientConfig(sharedPreferences)

        return success
    }
    public fun detachClient(sharedPreferences: SharedPreferences) : Boolean{
        val detachReqBody = JSONObject().put("cmd", "detach_location")
        detachReqBody.put("client_uid",sharedPreferences.getString("client_uid",null))
        detachReqBody.put("client_token",sharedPreferences.getString("device_token",null))
        var success = false

        val mRequestThread = Thread(Runnable {
            try {
                // Get Available Locations
                val response = ApiRequestHandler.requestPOST(
                    CLIENT_API_ENDPOINT,
                    detachReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                val detachResponse = JSONObject("""$response""")
                success = true
            } catch (ex: Exception) {
            }
        })
        mRequestThread.start()
        while(mRequestThread.isAlive){
            run {

            }
        }
        return success
    }

    fun deleteClient(sharedPreferences: SharedPreferences) : Boolean{
        // then delete
        val deleteReqBody = JSONObject().put("cmd", "delete_client")
        deleteReqBody.put("local_uid",sharedPreferences.getString("local_uid",null))
        var success = false

        val mRequestThread = Thread(Runnable {
            try {
                // Get Available Locations
                val response = ApiRequestHandler.requestPOST(
                    USER_API_ENDPOINT,
                    deleteReqBody,
                    sharedPreferences.getString("user_token", "")!!
                )
                val deleteResponse = JSONObject("""$response""")
                success = true
            } catch (ex: Exception) {
            }
        })
        mRequestThread.start()
        while(mRequestThread.isAlive){
            run {

            }
        }

        return success
    }

}