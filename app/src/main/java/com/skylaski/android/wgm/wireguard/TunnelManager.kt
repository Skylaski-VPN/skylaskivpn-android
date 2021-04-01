package com.skylaski.android.wgm.wireguard

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.wireguard.android.backend.GoBackend
import com.wireguard.config.Config
import com.wireguard.android.backend.Tunnel


class TunnelManager(context: Context, backend: GoBackend, tunnel: MyTunnel) {
    private var mTAG = "tunnelManager"
    private var mContext = context
    private val mBackend = backend
    private val mTunnel = tunnel

    fun toggleTunnelWithPermissionsResult(){
        Toast.makeText(mContext,"Permissions Granted, Thank You!",Toast.LENGTH_SHORT).show()
    }

    fun connect(sharedPreferences: SharedPreferences) {
        val wgConfigText = sharedPreferences.getString("wireguard_client_config","")
        try {
            val wgConfigParsed = Config.parse(
                wgConfigText!!.byteInputStream(Charsets.UTF_8).bufferedReader(Charsets.UTF_8)
            )
        }catch(ex: Exception){
        }

        val connectThread = Thread(Runnable {
            val textConfig = sharedPreferences.getString("wireguard_client_config","NOCONFIG")

            try {
                val config = Config.parse(
                    textConfig!!.byteInputStream(Charsets.UTF_8).bufferedReader(Charsets.UTF_8)
                )
                mBackend.setState(mTunnel, Tunnel.State.UP, config)
            } catch (ex: Exception) {
            }
        })
        connectThread.start()
        while(connectThread.isAlive){
            run {

            }
        }

        sharedPreferences.edit().putBoolean("is_connected", true).apply()
    }

    fun disconnect(sharedPreferences: SharedPreferences){

        val disconnectThread = Thread(Runnable {
            mBackend.setState(mTunnel, Tunnel.State.DOWN,null)
        })
        disconnectThread.start()
        while(disconnectThread.isAlive){
            run{

            }
        }

        sharedPreferences.edit().putBoolean("is_connected", false).apply()
    }
}

