package com.emove.emoveapplication

import com.movesense.mds.MdsConnectionListener
import com.movesense.mds.MdsException

class MdsListener(private val callback: Callback) : MdsConnectionListener {

    override fun onConnect(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionComplete(macAddress: String, serial: String) {
        callback.onConnected(macAddress, serial)
    }

    override fun onDisconnect(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(p0: MdsException?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    interface Callback {

        fun onConnected(macAddress: String, serial: String)

    }
}