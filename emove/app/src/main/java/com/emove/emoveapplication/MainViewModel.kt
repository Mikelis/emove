package com.emove.emoveapplication

import androidx.lifecycle.*
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class MainViewModel(lifecycle: Lifecycle) : ViewModel(), LifecycleObserver {

    private val stateSubject: BehaviorSubject<State> = BehaviorSubject.createDefault(State.default())

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {

    }

    fun getState(): Observable<State> = stateSubject.toFlowable(BackpressureStrategy.LATEST)
            .toObservable()
            .distinctUntilChanged()

    fun onPermissionGranted() {
        stateSubject.onNext(State.Scanning)
    }

    class Factory(private val lifecycle: Lifecycle) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(lifecycle) as T
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