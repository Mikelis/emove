package com.emove.emoveapplication

import androidx.lifecycle.*
import com.emove.emoveapplication.models.MyScanResult
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.scan.ScanResult
import com.polidea.rxandroidble.scan.ScanSettings
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

class MainViewModel(lifecycle: Lifecycle,
                    private val rxBleClient: RxBleClient) : ViewModel(), LifecycleObserver {

    private val subs = CompositeSubscription()
    private val stateSubject: BehaviorSubject<State> = BehaviorSubject.create(State.default())

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
            val msr = MyScanResult(scanResult)
        }
    }

    class Factory(private val lifecycle: Lifecycle,
                  private val rxBleClient: RxBleClient) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(lifecycle, rxBleClient) as T
        }
    }

    sealed class State {

        object Initial : State()
        object Scanning : State()

        data class Ready(val sensor: RxBleDevice) : State()

        companion object {
            fun default() = Initial
        }
    }

}