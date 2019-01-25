package com.nyp.fypj.smartbackpackapp.mdui

import android.annotation.SuppressLint
import android.os.Bundle
import android.app.Activity
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.nyp.fypj.smartbackpackapp.Constants
import com.nyp.fypj.smartbackpackapp.R
import com.nyp.fypj.smartbackpackapp.bluetooth.BtCommandObject
import com.nyp.fypj.smartbackpackapp.bluetooth.BtWrapper
import com.nyp.fypj.smartbackpackapp.bluetooth.HoldingZoneData

import kotlinx.android.synthetic.main.activity_iot_bt_testing.*
import android.os.AsyncTask.execute
import com.nyp.fypj.smartbackpackapp.ml.IotDataTensorFlowManager
import java.util.concurrent.Executors
import android.databinding.adapters.TextViewBindingAdapter.setText
import android.os.Build
import android.support.annotation.RequiresApi
import com.nyp.fypj.smartbackpackapp.ml.Classifier
import java.nio.FloatBuffer


class IotBtTestingActivity : Activity() {

    var et_input:EditText? = null
    var et_output:EditText? = null
    var btn_send:Button? = null
    var btn_ml:Button? = null
    var wv_reference:WebView? = null

    var btWrapper:BtWrapper? = null

    private var classifier: Classifier? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val INPUT_SIZE = 5
    private val INPUT_NAME = "input_tensor"
    private val OUTPUT_NAME = "output_pred"

    private val MODEL_FILE = "file:///android_asset/sbp_model.tflite"
    private val LABEL_FILE = "file:///android_asset/sbp_labels.txt"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iot_bt_testing)

        et_input = findViewById(R.id.et_input)
        et_output = findViewById(R.id.et_output)
        btn_send = findViewById(R.id.btn_send)
        btn_ml = findViewById(R.id.btn_ml)
        wv_reference = findViewById(R.id.wv_reference)

        //btWrapper = BtWrapper(uiHandlers)
        wv_reference!!.loadUrl("https://github.com/c0j0s/SmartBackpack/blob/master/SmartBackpackIOT/README.md#iot-device-function-codes")

        btn_send!!.setOnClickListener {
            val code = et_input!!.text.toString()

            when(code){
                "00000"->{
                    btWrapper!!.disconnectDevice()
                }
                "10000"->{
                    btWrapper!!.restartDevice()
                }
                "11000"->{
                    et_output!!.setText("Action Not Implemented")
                }
                "11500"->{
                    et_output!!.setText("Action Not Implemented")
                }
                "12000"->{
                    et_output!!.setText("Action Not Implemented")
                }
                "12500"->{
                    et_output!!.setText("Action Not Implemented")
                }
                "30000"->{
                    btWrapper!!.getSensorData()
                }
                "31000"->{
                    btWrapper!!.syncHoldingZone()
                }
                "32000"->{
                    et_output!!.setText("Action Not Implemented")
                }
                "41000"->{
                    btWrapper!!.toggleDebug()
                }
                "42000"->{
                    et_output!!.setText("Action Not Implemented")
                }
                else -> {
                    btWrapper!!.elseTest()
                }
            }
        }

        initTensorFlowAndLoadModel()

        btn_ml!!.setOnClickListener {

            val input = floatArrayOf(
                    0.48.toFloat(),
                    0.67.toFloat(),
                    2.52.toFloat(),
                    1.84.toFloat(),
                    0.2.toFloat()
            )


            val results = classifier!!.classifyComfortLevel(input)

            if (results.isNotEmpty()) {
                val value = " Number is : " + results[0].title
                et_output!!.setText(value)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initTensorFlowAndLoadModel() {
        try {
            classifier = IotDataTensorFlowManager.create(
                    assets,
                    MODEL_FILE,
                    LABEL_FILE,
                    INPUT_SIZE,
                    INPUT_NAME,
                    OUTPUT_NAME)
            Log.d(TAG, "Load Success")
        } catch (e: Exception) {
            throw RuntimeException("Error initializing TensorFlow!", e)
        }
    }

    private val uiHandlers = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            Log.d(TAG, msg.what.toString())
            when (msg.what) {
                //handle when device connected
                Constants.HANDLER_ACTION.CONNECTED.value->{

                    et_output!!.setText("Device Connected")

                }
                Constants.HANDLER_ACTION.DISCONNECTED.value->{

                    et_output!!.setText("Device Disconnected")

                }
                Constants.HANDLER_ACTION.CONNECT_LOST.value->{

                    et_output!!.setText("Backpack connection lost")

                }
                Constants.HANDLER_ACTION.CONNECT_ERROR.value->{

                    et_output!!.setText("Backpack connection error")

                }
                Constants.HANDLER_ACTION.COMMAND_SEND.value->{

                    et_output!!.setText("Command send")

                }
                Constants.HANDLER_ACTION.RECEIVE_RESPONSE.value->{

                    val mBtCommandObject = msg.obj as BtCommandObject
                    when(mBtCommandObject.function_code){
                        Constants.BT_FUN_CODE.GET_SENSOR_DATA.code->{

                            et_output!!.setText(mBtCommandObject.data.toString())

                        }
                        Constants.BT_FUN_CODE.SYNC_HOLDING_ZONE.code->{

                            for (keyValuePair in mBtCommandObject.data){
                                et_output!!.setText(keyValuePair.toString())
                            }

                            btWrapper!!.flushHoldingZone()
                        }
                        Constants.BT_FUN_CODE.FLUSH_HOLDING_ZONE.code->{

                            et_output!!.setText("Holding zone flushing complete")

                        }
                        Constants.BT_FUN_CODE.CHANGE_DEVICE_SETTINGS.code ->{

                            et_output!!.setText("Device config changed, not sync to hana")

                        }
                        Constants.BT_FUN_CODE.TOGGLE_DEBUG.code->{

                            et_output!!.setText(mBtCommandObject.data.toString())

                        }
                        else -> {
                            //received code not supported
                            et_output!!.setText("received code not supported: " + mBtCommandObject.function_code)
                        }
                    }
                }
                Constants.HANDLER_ACTION.RECEIVE_ERROR.value->{

                    et_output!!.setText("Error occurred")

                }
                else -> {
                    et_output!!.setText("state not supported: " + msg.what)
                }
            }
        }
    }

    companion object {
        private const val TAG = " IotBtTestingActivity"
    }

}
