package com.example.busyface

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.util.Log
import android.util.SparseArray

class ComplicationsHandler(_numComplications: Int) {
    private val tag = "ComplicationsHandler"
    private val numComplications: Int = _numComplications
    private val supportedTypes: MutableMap<Int, IntArray> = mutableMapOf()
    val complicationIds = IntArray(numComplications)
    private val activeComplicationDatas: SparseArray<ComplicationData> = SparseArray(numComplications)
    private val complicationDrawables: SparseArray<ComplicationDrawable> = SparseArray(numComplications)

    init {
        //populate the ids
        for (i in 0..(numComplications - 1)) {
            //TODO: better ids? Probably not necessary
            complicationIds[i] = i
        }
    }

    /**
     * Updates complications to properly render in ambient mode based on the screen's capabilities.
     */
    fun onPropertiesChanged(lowBitAmbient: Boolean, burnInProtection: Boolean) {

        //TODO: Could change this to take a Bundle and match the method signature of watchfaceservice engine

        var drawable: ComplicationDrawable
        for (id in complicationIds) {
            drawable = complicationDrawables.get(id)

            if (drawable != null) {
                drawable.setLowBitAmbient(lowBitAmbient)
                drawable.setBurnInProtection(burnInProtection)
            }
        }
    }

    /**
     * update a complication's data when it changes
     */
    fun onComplicationDataUpdate(complicationId: Int, complicationData: ComplicationData) {
        activeComplicationDatas.put(complicationId, complicationData)

        val drawable = complicationDrawables.get(complicationId)
        drawable.setComplicationData(complicationData)
    }

    /**
     * return the id of the complication that is within these bounds
     * if none, return -1
     */
    fun getTappedComplicationId(x: Int, y: Int) : Int {
        var complicationData: ComplicationData?
        var complicationDrawable: ComplicationDrawable

        val currentTimeMillis = System.currentTimeMillis()

        for (id in complicationIds) {
            complicationData = activeComplicationDatas.get(id)

            if (complicationData != null
                && complicationData.isActive(currentTimeMillis)
                && complicationData.type != ComplicationData.TYPE_NOT_CONFIGURED
                && complicationData.type != ComplicationData.TYPE_EMPTY) {

                complicationDrawable = complicationDrawables.get(id)
                val complicationBoundingRect = complicationDrawable.bounds

                if (complicationBoundingRect.width() > 0) {
                    if (complicationBoundingRect.contains(x, y)) {
                        return id
                    }
                } else {
                    Log.e(tag, "Not a recognized complication id.")
                }
            }
        }
        return -1
    }

    // Fires PendingIntent associated with complication (if it has one).
    fun processTap(x: Int, y: Int, applicationContext: Context) {
        Log.d(tag, "processTap()")

        val complicationId = getTappedComplicationId(x, y)

        if (complicationId !in complicationIds) {
            return
        }

        val complicationData = activeComplicationDatas.get(complicationId)

        if (complicationData != null) {
            if (complicationData.tapAction != null) {
                try {
                    complicationData.tapAction.send()
                } catch (e: PendingIntent.CanceledException) {
                    Log.e(tag, "processTap e: $e")
                }

            } else if (complicationData.type == ComplicationData.TYPE_NO_PERMISSION) {
                //launch permission request
                val componentName = ComponentName(applicationContext, BusyFaceService::class.java)

                val permissionRequestIntent = ComplicationHelperActivity.createPermissionRequestHelperIntent(applicationContext, componentName)

                applicationContext.startActivity(permissionRequestIntent)
            }
        } else {
            Log.d(tag, "no pendingintent for complication $complicationId")
        }
    }

    fun drawComplications(canvas: Canvas, currentTimeMillis: Long) {
        for (id in complicationIds) {
            complicationDrawables.get(id).draw(canvas, currentTimeMillis)
        }
    }

    fun onAmbientModeChanged(inAbmientMode: Boolean) {
        var drawable: ComplicationDrawable
        for (id in complicationIds) {
            drawable = complicationDrawables.get(id)
            drawable.setInAmbientMode(inAbmientMode)
        }
    }

    fun setDrawableBounds(id: Int, bounds: Rect) {
        complicationDrawables.get(id).bounds = bounds
    }

    /**
     * Set the supported types for one complication.
     * complicationSupportedTypes is an array of values from the ComplicationData enum
     */
    fun setComplicationSupportedTypes(complicationId: Int, complicationSupportedTypes: IntArray) {
        supportedTypes[complicationId] = complicationSupportedTypes
    }

    fun getComplicationSupportedTypes(complicationId: Int) : IntArray {
        return supportedTypes[complicationId]!!
    }

    fun putComplicationDrawable(complicationId: Int, drawable: ComplicationDrawable) {
        complicationDrawables.put(complicationId, drawable)
    }

    fun getComplicationDrawable(complicationId: Int) : ComplicationDrawable {
        return complicationDrawables.get(complicationId)
    }

    fun putComplicationData(complicationId: Int, data: ComplicationData) {
        activeComplicationDatas.put(complicationId, data)
    }
}