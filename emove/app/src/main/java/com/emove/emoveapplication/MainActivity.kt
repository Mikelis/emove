package com.emove.emoveapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.TransitionManager
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.emove.emoveapplication.MainViewModel.State
import com.movesense.mds.Mds
import com.polidea.rxandroidble.RxBleClient
import kotlinx.android.synthetic.main.onboarding_layout.*
import kotlinx.android.synthetic.main.onboarding_one.*
import kotlinx.android.synthetic.main.onboarding_two.*
import kotlinx.android.synthetic.main.onboarding_zero.*
import rx.subscriptions.CompositeSubscription




class MainActivity : AppCompatActivity() {

    private val subs = CompositeSubscription()
    private lateinit var vm: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_layout)

        vm = ViewModelProviders.of(
                this,
                MainViewModel.Factory(
                        lifecycle,
                        RxBleClient.create(this),
                        Mds.Builder().build(this)
                )
        ).get(MainViewModel::class.java)
        setWindowWidthToScreens()
        setOnBoardingNavigation()
    }

    companion object {
        private const val SECOND_TAG: String = "SECOND"
        private const val THIRD_TAG: String = "THIRD"
        private const val FOURTH_TAG: String = "FOURTH"
        private const val RIGHT_MARGIN: Float = 11f
    }

    private fun setWindowWidthToScreens(){
        val metrics = resources.displayMetrics
        val windowWidth = metrics.widthPixels
        val margin = getRightMargin()
        val finalWidth = (windowWidth - margin).toInt()
        onboardingOne.layoutParams.width  = finalWidth
        onboardingTwo.layoutParams.width  = finalWidth
        onboardingThree.layoutParams.width  = finalWidth
        onboardingFour.layoutParams.width  = windowWidth
    }

    private fun getRightMargin() :Float{
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RIGHT_MARGIN, resources.displayMetrics);
    }

    private fun setOnBoardingNavigation(){
        nextZero.setOnClickListener {
            TransitionManager.beginDelayedTransition(constraintParent)
            val currentTag = constraintParent.tag
            when (currentTag) {
                null -> {
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintParent)
                    constraintSet.clear(R.id.onboardingTwo,ConstraintSet.START)
                    constraintSet.applyTo(constraintParent)
                    constraintParent.tag = SECOND_TAG
                }

            }
        }
        nextOne.setOnClickListener {
            TransitionManager.beginDelayedTransition(constraintParent)
            val currentTag = constraintParent.tag
            when (currentTag) {
                SECOND_TAG -> {
                    val constraintSet1 = ConstraintSet()
                    constraintSet1.clone(constraintParent)
                    constraintSet1.clear(R.id.onboardingThree,ConstraintSet.START)
                    constraintSet1.applyTo(constraintParent)
                    constraintParent.tag = THIRD_TAG
                }

            }
        }
        nextTwo.setOnClickListener {
            TransitionManager.beginDelayedTransition(constraintParent)
            val currentTag = constraintParent.tag
            when (currentTag) {
                THIRD_TAG -> {
                    val constraintSet2 = ConstraintSet()
                    constraintSet2.clone(constraintParent)
                    constraintSet2.clear(R.id.onboardingFour,ConstraintSet.START)
                    constraintSet2.applyTo(constraintParent)
                    constraintParent.tag = FOURTH_TAG
                }
            }
        }
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
        else -> {
        }
    }

    private fun requestNeededPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        } else {
            vm.onPermissionGranted()
        }
    }

    private fun showScanning(scanning: Boolean = true) {
//        content.text = "Scanning"
    }

    private fun showData() {
//        content.text = "Data"
    }

}

