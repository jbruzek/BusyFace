package com.example.busyface

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderChooserIntent
import android.support.wearable.complications.ProviderInfoRetriever
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.activity_config.*
import java.util.concurrent.Executors

class BusyFaceActivity : Activity(), View.OnClickListener {

    companion object {
        const val TAG = "BusyFaceActivity"
        const val CONFIG_REQUEST_CODE = 10258
    }

    private val complications = BusyFaceService.complications

    private var selectedId = -1
    private lateinit var watchComponentName : ComponentName
    private lateinit var providerInfoRetriever : ProviderInfoRetriever
    private lateinit var defaultAddDrawable : Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        watchComponentName = ComponentName(applicationContext, BusyFaceService::class.java)
        defaultAddDrawable = getDrawable(R.drawable.add_complication)

        //setup complication views
        for (id in 0 until BusyFaceService.NUM_COMPLICATIONS) {
            complications_view_wrapper.getChildAt(id).visibility = View.INVISIBLE
            complications_button_wrapper.getChildAt(id).setOnClickListener(this)
            (complications_button_wrapper.getChildAt(id) as ImageButton).setImageDrawable(defaultAddDrawable)
        }

        providerInfoRetriever = ProviderInfoRetriever(applicationContext, Executors.newCachedThreadPool())
        providerInfoRetriever.init()

        retrieveInitialComplicationsData()
    }

    override fun onDestroy() {
        super.onDestroy()

        providerInfoRetriever.release()
    }

    private fun retrieveInitialComplicationsData() {
        val complicationIds = complications.complicationIds

        providerInfoRetriever.retrieveProviderInfo(
            object : ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                override fun onProviderInfoReceived(watchFaceComplicationId: Int, info: ComplicationProviderInfo?) {
                    Log.d(TAG, "providerinfo received: $info")

                    updateComplicationViews(watchFaceComplicationId, info)
                }
            },
            watchComponentName, *complicationIds)
    }

    private fun updateComplicationViews(id: Int, provInfo: ComplicationProviderInfo?) {
        Log.d(TAG, "Updating complication view: $id")

        if (provInfo != null) {
            complications_view_wrapper.getChildAt(id).visibility = View.VISIBLE
            (complications_button_wrapper.getChildAt(id) as ImageButton).setImageIcon(provInfo.providerIcon)
        } else {
            complications_view_wrapper.getChildAt(id).visibility = View.INVISIBLE
            (complications_button_wrapper.getChildAt(id) as ImageButton).setImageDrawable(defaultAddDrawable)
        }
    }

    override fun onClick(v: View?) {
        launchComplicationHelperActivity(Integer.parseInt(v?.tag as String))
    }

    private fun launchComplicationHelperActivity(id: Int) {
        selectedId = id

        if (id >= 0) {
            startActivityForResult(
                ComplicationHelperActivity.createProviderChooserHelperIntent(applicationContext, watchComponentName, selectedId, *complications.getComplicationSupportedTypes(id)),
                CONFIG_REQUEST_CODE)
        } else {
            Log.d(TAG, "Complication not supported by face")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {
            val provInfo = data?.getParcelableExtra<ComplicationProviderInfo>(ProviderChooserIntent.EXTRA_PROVIDER_INFO)

            if (selectedId >= 0) {
                updateComplicationViews(selectedId, provInfo)
            }
        }
    }
}