package com.skylaski.android.wgm.wireguard

import com.wireguard.android.backend.Tunnel

class MyTunnel(name: String): Tunnel {
    private var mName = name

    override fun getName(): String {
        return mName
    }

    override fun onStateChange(newState: Tunnel.State) {
        State
    }


    enum class State {
        DOWN, TOGGLE, UP;

        companion object {
            /**
             * Get the state of a [Tunnel]
             *
             * @param running boolean indicating if the tunnel is running.
             * @return State of the tunnel based on whether or not it is running.
             */
            fun of(running: Boolean): State {
                return if (running) UP else DOWN
            }
        }
    }
}