package com.movesense.samples.sensorsample


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.google.gson.Gson
import com.movesense.mds.*
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.scan.ScanSettings
import rx.Subscription
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    // MDS
    private var mMds: Mds? = null

    // UI
    private var mScanResultListView: ListView? = null
    private val mScanResArrayList = ArrayList<MyScanResult>()
    internal var mScanResArrayAdapter: ArrayAdapter<MyScanResult>? = null
    private var mdsSubscription: MdsSubscription? = null
    private var subscribedDeviceSerial: String? = null

    private lateinit var onboardingOne: View
    private lateinit var onboardingTwo: View
    private lateinit var onboardingThree: View
    private lateinit var onboardingFour: View
    private lateinit var nextZero: View
    private lateinit var nextOne: View
    private lateinit var nextTwo: View
    private lateinit var constraintParent: ConstraintLayout

    private// Init RxAndroidBle (Ble helper library) if not yet initialized
    val bleClient: RxBleClient?
        get() {
            if (mBleClient == null) {
                mBleClient = RxBleClient.create(this)
            }

            return mBleClient
        }

    internal var mScanSubscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_layout)

        // Init Scan UI
//        mScanResultListView = findViewById(R.id.listScanResult) as ListView
        mScanResArrayAdapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1, mScanResArrayList)
//        mScanResultListView!!.adapter = mScanResArrayAdapter
//        mScanResultListView!!.onItemLongClickListener = this
//        mScanResultListView!!.onItemClickListener = this

        // Make sure we have all the permissions this app needs
        requestNeededPermissions()

        // Initialize Movesense MDS library
        initMds()

        getViews()
        setWindowWidthToScreens()
        setOnBoardingNavigation()
    }

    private fun getViews() {
        onboardingOne = findViewById(R.id.onboardingOne)
        onboardingTwo = findViewById(R.id.onboardingTwo)
        onboardingThree = findViewById(R.id.onboardingThree)
        onboardingFour = findViewById(R.id.onboardingFour)
        nextZero = findViewById(R.id.nextZero)
        nextOne = findViewById(R.id.nextOne)
        nextTwo = findViewById(R.id.nextTwo)
        constraintParent = findViewById(R.id.constraintParent) as ConstraintLayout
    }


    private fun setWindowWidthToScreens() {
        val metrics = resources.displayMetrics
        val windowWidth = metrics.widthPixels
        val margin = getRightMargin()
        val finalWidth = (windowWidth - margin).toInt()
        onboardingOne.layoutParams.width = finalWidth
        onboardingTwo.layoutParams.width = finalWidth
        onboardingThree.layoutParams.width = finalWidth
        onboardingFour.layoutParams.width = windowWidth
    }

    private fun getRightMargin(): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RIGHT_MARGIN, resources.displayMetrics);
    }

    private fun setOnBoardingNavigation() {
        nextZero.setOnClickListener {
            TransitionManager.beginDelayedTransition(constraintParent)
            val currentTag = constraintParent.tag
            when (currentTag) {
                null -> {
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintParent)
                    constraintSet.clear(R.id.onboardingTwo, ConstraintSet.START)
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
                    constraintSet1.clear(R.id.onboardingThree, ConstraintSet.START)
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
                    constraintSet2.clear(R.id.onboardingFour, ConstraintSet.START)
                    constraintSet2.applyTo(constraintParent)
                    constraintParent.tag = FOURTH_TAG
                }
            }
        }
    }

    private fun initMds() {
        mMds = Mds.builder().build(this)
    }

    internal fun requestNeededPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)

        }
    }

    fun onScanClicked(view: View) {
//        findViewById(R.id.buttonScan).visibility = View.GONE
//        findViewById(R.id.buttonScanStop).visibility = View.VISIBLE

        // Start with empty list
        mScanResArrayList.clear()
        mScanResArrayAdapter?.notifyDataSetChanged()

        mScanSubscription = bleClient!!.scanBleDevices(
                ScanSettings.Builder()
                        // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                        // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                        .build()
                // add filters if needed
        )
                .subscribe(
                        { scanResult ->
                            Log.d(LOG_TAG, "scanResult: $scanResult")

                            // Process scan result here. filter movesense devices.
                            if (scanResult.bleDevice != null &&
                                    scanResult.bleDevice.name != null &&
                                    scanResult.bleDevice.name!!.startsWith("Movesense")) {

                                // replace if exists already, add otherwise
                                val msr = MyScanResult(scanResult)
                                if (mScanResArrayList.contains(msr))
                                    mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
                                else
                                    mScanResArrayList.add(0, msr)

                                mScanResArrayAdapter?.notifyDataSetChanged()
                            }
                        },
                        { throwable ->
                            Log.e(LOG_TAG, "scan error: $throwable")
                            // Handle an error here.

                            // Re-enable scan buttons, just like with ScanStop
                            onScanStopClicked(null)
                        }
                )
    }

    fun onScanStopClicked(view: View?) {
        if (mScanSubscription != null) {
            mScanSubscription!!.unsubscribe()
            mScanSubscription = null
        }

//        findViewById(R.id.buttonScan).visibility = View.VISIBLE
//        findViewById(R.id.buttonScanStop).visibility = View.GONE
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (position < 0 || position >= mScanResArrayList.size)
            return

        val device = mScanResArrayList[position]
        if (!device.isConnected) {
            // Stop scanning
            onScanStopClicked(null)

            // And connect to the device
            connectBLEDevice(device)
        } else {
            // Device is connected, trigger showing /Info
            subscribeToSensor(device.connectedSerial)
        }
    }

    private fun subscribeToSensor(connectedSerial: String) {
        // Clean up existing subscription (if there is one)
        if (mdsSubscription != null) {
            unsubscribe()
        }

        // Build JSON doc that describes what resource and device to subscribe
        // Here we subscribe to 13 hertz accelerometer data
        val sb = StringBuilder()
        val strContract = sb.append("{\"Uri\": \"").append(connectedSerial).append(URI_MEAS_ACC_13).append("\"}").toString()
        Log.d(LOG_TAG, strContract)
//        val sensorUI = findViewById(R.id.sensorUI)

        subscribedDeviceSerial = connectedSerial

        mdsSubscription = mMds?.subscribe(URI_EVENTLISTENER,
                strContract, object : MdsNotificationListener {
            override fun onNotification(data: String) {
                Log.d(LOG_TAG, "onNotification(): $data")

                // If UI not enabled, do it now
//                if (sensorUI.visibility == View.GONE)
//                    sensorUI.visibility = View.VISIBLE

                val accResponse = Gson().fromJson(data, AccDataResponse::class.java)
                if (accResponse != null && accResponse.body.array.size > 0) {

                    val accStr = String.format("%.02f, %.02f, %.02f", accResponse.body.array[0].x, accResponse.body.array[0].y, accResponse.body.array[0].z)

//                    (findViewById(R.id.sensorMsg) as TextView).text = accStr
                }
            }

            override fun onError(error: MdsException) {
                Log.e(LOG_TAG, "subscription onError(): ", error)
                unsubscribe()
            }
        })

    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        if (position < 0 || position >= mScanResArrayList.size)
            return false

        val device = mScanResArrayList[position]

        // unsubscribe if there
        Log.d(LOG_TAG, "onItemLongClick, " + device.connectedSerial + " vs " + subscribedDeviceSerial)
        if (device.connectedSerial == subscribedDeviceSerial)
            unsubscribe()

        Log.i(LOG_TAG, "Disconnecting from BLE device: " + device.macAddress)
        mMds!!.disconnect(device.macAddress)

        return true
    }

    private fun connectBLEDevice(device: MyScanResult) {
        val bleDevice = bleClient!!.getBleDevice(device.macAddress)

        Log.i(LOG_TAG, "Connecting to BLE device: " + bleDevice.macAddress)
        mMds!!.connect(bleDevice.macAddress, object : MdsConnectionListener {

            override fun onConnect(s: String) {
                Log.d(LOG_TAG, "onConnect:$s")
            }

            override fun onConnectionComplete(macAddress: String, serial: String) {
                for (sr in mScanResArrayList) {
                    if (sr.macAddress.equals(macAddress, ignoreCase = true)) {
                        sr.markConnected(serial)
                        break
                    }
                }
                mScanResArrayAdapter?.notifyDataSetChanged()
            }

            override fun onError(e: MdsException) {
                Log.e(LOG_TAG, "onError:$e")

                showConnectionError(e)
            }

            override fun onDisconnect(bleAddress: String) {

                Log.d(LOG_TAG, "onDisconnect: $bleAddress")
                for (sr in mScanResArrayList) {
                    if (bleAddress == sr.macAddress) {
                        // unsubscribe if was subscribed
                        if (sr.connectedSerial != null && sr.connectedSerial == subscribedDeviceSerial)
                            unsubscribe()

                        sr.markDisconnected()
                    }
                }
                mScanResArrayAdapter?.notifyDataSetChanged()
            }
        })
    }

    private fun showConnectionError(e: MdsException) {
        val builder = AlertDialog.Builder(this)
                .setTitle("Connection Error:")
                .setMessage(e.message)

        builder.create().show()
    }

    private fun unsubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription!!.unsubscribe()
            mdsSubscription = null
        }

        subscribedDeviceSerial = null

        // If UI not invisible, do it now
//        val sensorUI = findViewById(R.id.sensorUI)
//        if (sensorUI.visibility != View.GONE)
//            sensorUI.visibility = View.GONE

    }

    fun onUnsubscribeClicked(view: View) {
        unsubscribe()
    }

    companion object {
        private val LOG_TAG = MainActivity::class.java.simpleName
        private val MY_PERMISSIONS_REQUEST_LOCATION = 1
        val URI_CONNECTEDDEVICES = "suunto://MDS/ConnectedDevices"
        val URI_EVENTLISTENER = "suunto://MDS/EventListener"
        val SCHEME_PREFIX = "suunto://"

        // BleClient singleton
        private var mBleClient: RxBleClient? = null

        // Sensor subscription
        private val URI_MEAS_ACC_13 = "/Meas/Acc/13"

        private const val SECOND_TAG: String = "SECOND"
        private const val THIRD_TAG: String = "THIRD"
        private const val FOURTH_TAG: String = "FOURTH"
        private const val RIGHT_MARGIN: Float = 11f
    }
}
