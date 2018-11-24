package com.emove.emoveapplication

import androidx.lifecycle.*
import com.emove.emoveapplication.models.MyScanResult
import com.movesense.mds.Mds
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.scan.ScanResult
import com.polidea.rxandroidble.scan.ScanSettings
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

class MainViewModel(
        lifecycle: Lifecycle,
        private val rxBleClient: RxBleClient,
        private val mds: Mds
) : ViewModel(), LifecycleObserver, MdsListener.Callback {

    private val subs = CompositeSubscription()
    private val stateSubject: BehaviorSubject<State> = BehaviorSubject.create(State.default())
    private var myScanResult: MyScanResult? = null

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        subs.clear()
    }

    fun getState(): Observable<State> = stateSubject.asObservable().distinctUntilChanged()

    fun onPermissionGranted() {
        stateSubject.onNext(State.Scanning)

        subs.add(rxBleClient.scanBleDevices(ScanSettings.Builder().build()).subscribe(::onScanResult))
    }

    private fun onScanResult(scanResult: ScanResult) {
        if (scanResult.bleDevice?.name?.startsWith("Movesense") == true) {
            myScanResult = MyScanResult(scanResult)
            val bleDevice = rxBleClient.getBleDevice(myScanResult!!.macAddress)
            mds.connect(bleDevice.macAddress, MdsListener(this))
        }
    }

    override fun onConnected(macAddress: String, serial: String) {
         myScanResult!!.markConnected(serial)
        stateSubject.onNext(State.Connected)
        subToSensor()
    }

    private fun subToSensor() {
        TODO()
    }

    class Factory(private val lifecycle: Lifecycle,
                  private val rxBleClient: RxBleClient,
                  private val mds: Mds) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(lifecycle, rxBleClient, mds) as T
        }
    }

    sealed class State {

        object Initial : State()
        object Scanning : State()
        object Connected : State()

        data class Ready(val sensor: RxBleDevice) : State()

        companion object {
            fun default() = Initial
        }
    }

}