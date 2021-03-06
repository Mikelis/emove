package com.movesense.samples.sensorsample


import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
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
import android.widget.TextView
import com.google.gson.Gson
import com.movesense.mds.*
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.scan.ScanSettings
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemLongClickListener {

    // MDS
    private var mMds: Mds? = null

    // UI
    private var mScanResultListView: ListView? = null
    private val mScanResArrayList = ArrayList<MyScanResult>()
    internal var mScanResArrayAdapter: ArrayAdapter<MyScanResult>? = null


    private var accSubscription: MdsSubscription? = null
    private var tempSubscription: MdsSubscription? = null
    private var heartRateSubscription: MdsSubscription? = null

    private var tempList: MutableList<TemperatureSubscribeModel> = mutableListOf()
    private var hrList: MutableList<HeartRate> = mutableListOf()

    private var mdsSubscription: MdsSubscription? = null
    private var subscribedDeviceSerial: String? = null

    private lateinit var onboardingOne: View
    private lateinit var onboardingTwo: View
    private lateinit var onboardingThree: View
    private lateinit var onboardingFour: View
    private lateinit var nextZero: View
    private lateinit var nextOne: View
    private lateinit var nextTwo: View
    private lateinit var sleepView: View
    private lateinit var constraintParent: ConstraintLayout

    private lateinit var bpm: TextView
    private lateinit var temp: TextView
    private lateinit var variability: TextView
    private lateinit var tiredStatus: TextView
    private var playing = false


    private// Init RxAndroidBle (Ble helper library) if not yet initialized
    val bleClient: RxBleClient?
        get() {
            if (mBleClient == null) {
                mBleClient = RxBleClient.create(this)
            }

            return mBleClient
        }

    internal var mScanSubscription: Subscription? = null

    private var data: Data = Data()
        set(value) {
            field = value
            dataSubject.onNext(value)
        }
    private val dataSubject: BehaviorSubject<Data> = BehaviorSubject.create(data)

    fun getData(): Observable<Data> = dataSubject.onBackpressureLatest()

    fun getHrv(): Observable<Double> = getData()
            .map { d -> d.heartRate?.body?.rrData?.firstOrNull() ?: 0 }
//                .doOnNext { Log.d("HRVR", it.toString()) }
            .window(150, 15) // 10 sec, 1 sec
            .flatMap { it.toList() }
//                .doOnNext { Log.d("HRV Buffered", it.toString()) }
            .map { list ->
                (0 until (list.size - 1)).map { i ->
                    Math.pow((list[i] - list[i + 1]).toDouble(), 2.toDouble())
                }
            }
//                .doOnNext { Log.d("HRV Squares", it.toString()) }
            .map { squares -> Math.sqrt(squares.average()) } // average error
//                .doOnNext { Log.d("HRV avg err", it.toString()) }
            .window(10, 1).flatMap { it.toList() } // 1 min
            .map { list: MutableList<Double> ->
                val min = list.min() ?: 0.toDouble()
                val max = list.max() ?: 0.toDouble()
                val diff = max - min
                diff
            }
            .filter { it < 10 } // filter out window start artifacts

    private var maxHrv: Double = 0.toDouble()

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
        getData().subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe { data ->
                    updateCurrentData(data)
                }
        getHrv().subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe { data ->
                    updateCurrentHrvData(data)
                }

//        Observable.combineLatest(getHrv(), getMaxHrv()) { hrv: Double, max: Double ->
//            "$hrv ($max)"
//        }.subscribe { Log.d("HRV", it) }
    }

    private fun updateCurrentData(data: Data) {
        if(data.heartRate==null){
            bpm.text = "..."
        }
        else {
            val hr = data.heartRate?.body?.average
            bpm.text = "%.0f".format(hr)
        }
        if(data.temperature == null){
            temp.text = "..."
        }
        else {
            temp.text = "%.2f".format((data.temperature?.body?.measurement?.minus(273.15)))
        }
//        variability.text = "%.2f".format(data.heartRate?.body?.)
//        tiredStatus(data)
    }

    private fun updateCurrentHrvData(hrv: Double) {
        if (hrv > maxHrv) maxHrv = hrv  // TODO USE THIS
        variability.text = "%.2f".format(hrv)
        tiredStatus(hrv)
    }

    private fun tiredStatus(data: Double) {
        when {
            data < 2 -> { // alert - good
                hideLight()
                tiredStatus.text = getString(R.string.alert)
                tiredStatus.setTextColor(resources.getColor(R.color.alert_color))
            }
            data < (0.9 * maxHrv) -> { // normal
                hideLight()
                tiredStatus.text = getText(R.string.neutral)
                tiredStatus.setTextColor(resources.getColor(R.color.green_color))
            }
            else -> {
                tiredStatus.text = getText(R.string.sleepy)
                tiredStatus.setTextColor(resources.getColor(R.color.status_color))
                playAlarm()
                showLight()
            }
        }
//        when (data) {
//            MainActivity.Data.State.ALERT -> {
//                hideLight()
//                tiredStatus.text = getText(R.string.tired)
//
//            }
//            MainActivity.Data.State.DROWSY -> {
//                hideLight()
//            }
//            MainActivity.Data.State.SLEEP -> {
//                playAlarm()
//                showLight()
//            }
//        }
    }

    private fun playAlarm() {
        if (playing) {
            return
        }
        val mp = MediaPlayer.create(this, R.raw.alarm)
        mp.setOnCompletionListener { end ->
            mp.reset()
            mp.release()
            playing = false
        }
        //TODO do correctly
        Handler().postDelayed({
            mp?.stop()
        },1000)

        mp.start()
    }

    private fun showLight() {
//        sleepView.tag = 1
//        val animation1 = AlphaAnimation(0f, 1.0f)
//        animation1.duration = 1000
//        animation1.fillAfter = true
//        sleepView.startAnimation(animation1)

        //TODO do correctly
        Handler().postDelayed({
            hideLight()
        },1000)
        sleepView.visibility = View.VISIBLE
    }

    private fun hideLight() {
//        if (sleepView.tag == 1) {
//            sleepView.tag = 0
//            val animation1 = AlphaAnimation(0f, 1.0f)
//            animation1.duration = 1000
//            animation1.fillAfter = true
//            sleepView.startAnimation(animation1)
//        }

        sleepView.visibility = View.GONE
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
        sleepView = findViewById(R.id.sleep_alert_white)

        bpm = findViewById(R.id.bpm) as TextView
        temp = findViewById(R.id.temperature) as TextView
        variability = findViewById(R.id.heartrate) as TextView
        tiredStatus = findViewById(R.id.tired_status) as TextView
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

        } else {
            onScanClicked()
        }
    }

    fun onScanClicked() {
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
                                if (mScanResArrayList.contains(msr)) {
//                                    mScanResArrayList[mScanResArrayList.indexOf(msr)] = msr
                                    Log.d("mScanResArrayList", "OnItemClick")
//                                    onItemClick(mScanResArrayList.indexOf(msr))
                                } else {
                                    mScanResArrayList.add(0, msr)
                                    Log.d("mScanResArrayList", "OnItemClick")
                                    onItemClick(0)
                                }

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

    private fun onItemClick(position: Int) {
        if (position < 0 || position >= mScanResArrayList.size)
            return

        val device = mScanResArrayList[position]
        Log.d("onItemClick", "$device")
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
        if (accSubscription != null) {
            unsubscribe()
        }

        // Build JSON doc that describes what resource and device to subscribe
        // Here we subscribe to 13 hertz accelerometer data
        val sb = StringBuilder()
        val strContract = sb.append("{\"Uri\": \"").append(connectedSerial).append(URI_MEAS_ACC_13).append("\"}").toString()
        Log.d(LOG_TAG, strContract)
//        val sensorUI = findViewById(R.id.sensorUI)

        subscribedDeviceSerial = connectedSerial

        accSubscription = mMds?.subscribe(URI_EVENTLISTENER,
                strContract, object : MdsNotificationListener {
            override fun onNotification(notification: String) {

                // If UI not enabled, do it now
//                if (sensorUI.visibility == View.GONE)
//                    sensorUI.visibility = View.VISIBLE
                val accResponse = Gson().fromJson(notification, AccDataResponse::class.java)
                data = data.copy(acceleration = accResponse)

//                Log.d("MainActivity", "Subscription Accelometer - ${accResponse.body.array}")
            }

            override fun onError(error: MdsException) {
                Log.e(LOG_TAG, "subscription onError(): ", error)
                unsubscribe()
            }
        })

//                    (findViewById(R.id.sensorMsg) as TextView).text = accStr

        tempSubscription = mMds?.subscribe("suunto://MDS/EventListener",
                formatContractToJson(connectedSerial, "Meas/Temp"), object : MdsNotificationListener {
            override fun onNotification(notification: String) {

                val temperature = Gson().fromJson<TemperatureSubscribeModel>(notification, TemperatureSubscribeModel::class.java)
                data = data.copy(temperature = temperature)

                Log.d("MainActivity", "Subscription Temperature - ${temperature.body.measurement - 273.15}")

            }

            override fun onError(error: MdsException) {
                Log.e(LOG_TAG, "subscription onError(): ", error)
                unsubscribe()
            }
        })

        heartRateSubscription = mMds?.subscribe("suunto://MDS/EventListener",
                formatContractToJson(connectedSerial, "Meas/Hr"), object : MdsNotificationListener {
            override fun onNotification(notification: String) {

                val heartRate = Gson().fromJson<HeartRate>(notification, HeartRate::class.java)
                data = data.copy(heartRate = heartRate)

                Log.d("MainActivity", "Subscription R-R - ${heartRate.body.rrData.asList()}")
            }

            override fun onError(error: MdsException) {
                Log.e(LOG_TAG, "subscription onError(): ", error)
                unsubscribe()
            }
        })

    }

    fun formatContractToJson(serial: String, uri: String): String {
        val sb = StringBuilder()
        return sb.append("{\"Uri\": \"").append(serial).append("/").append(uri).append("\"}").toString()
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
                        subscribeToSensor(serial)
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

//        builder.create().show()
    }

    private fun unsubscribe() {
        if (accSubscription != null) {
            accSubscription!!.unsubscribe()
            accSubscription = null
        }

        if (tempSubscription != null) {
            tempSubscription!!.unsubscribe()
            tempSubscription = null
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


    data class Data(
            val acceleration: AccDataResponse? = null,
            val heartRate: HeartRate? = null,
            val temperature: TemperatureSubscribeModel? = null
    ) {

        fun getState(): State = State.ALERT

        enum class State {
            ALERT, DROWSY, SLEEP


        }
    }
}