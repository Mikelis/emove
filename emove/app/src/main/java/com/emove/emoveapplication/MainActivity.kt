package com.emove.emoveapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.emove.emoveapplication.MainViewModel.State
import com.polidea.rxandroidble.RxBleClient
import kotlinx.android.synthetic.main.activity_main.*
import rx.subscriptions.CompositeSubscription


class MainActivity : AppCompatActivity() {

    private val subs = CompositeSubscription()
    private lateinit var vm: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vm = ViewModelProviders.of(this, MainViewModel.Factory(lifecycle, RxBleClient.create(this), ))
                .get(MainViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        subs.add(vm.getState().subscribe(::onState))
    }

    override fun onPause() {
        super.onPause()
        subs.clear()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            vm.onPermissionGranted()
        }
    }

    private fun onState(newState: State) = when (newState) {
        State.Initial -> requestNeededPermissions()
        State.Scanning -> showScanning()
        is State.Ready -> {
            showScanning(false)
            showData()
        }
    }

    private fun requestNeededPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        }
    }

    private fun showScanning(scanning: Boolean = true) {
        content.text = "Scanning"
    }

    private fun showData() {
        content.text = "Data"
    }

}

