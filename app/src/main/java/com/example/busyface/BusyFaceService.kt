package com.example.busyface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import java.util.*
import java.util.concurrent.TimeUnit

class BusyFaceService : CanvasWatchFaceService() {

    companion object {
        const val NUM_COMPLICATIONS = 16

        val complications : ComplicationsHandler = ComplicationsHandler(BusyFaceService.NUM_COMPLICATIONS)
    }

    private val TAG = "BusyFaceService"
    private val UPDATE_RATE_MILLIS : Long = TimeUnit.SECONDS.toMillis(1)

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private val MSG_UPDATE_TIME = 0
        private val calendar = Calendar.getInstance()

        private var registeredTimeZoneReceiver = false
        private var centerX = 0.0f
        private var centerY = 0.0f
        private var backgroundPaint = Paint()
        private var ambient = false
        private var lowBitAmbient = false
        private var burnInProtection = false

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        private val updateTimeHandler = object : Handler() {
            override fun handleMessage(msg: Message?) {
                invalidate()
                if (shouldTimerBeRunning()) {
                    val timeMS = System.currentTimeMillis()
                    val delayMS = UPDATE_RATE_MILLIS - (timeMS % UPDATE_RATE_MILLIS)

                    this.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMS)
                }
            }
        }


        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            initializeComplications()

            setWatchFaceStyle(WatchFaceStyle.Builder(this@BusyFaceService).setAcceptsTapEvents(true).build())

            initializeBackground()
        }

        private fun initializeComplications() {
            Log.d(TAG, "initializeComplications()")

            val dataTypes = intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE)

            val drawable: ComplicationDrawable = getDrawable(R.drawable.custom_complication_styles) as ComplicationDrawable
            drawable.setContext(applicationContext)

            for (id in 0..NUM_COMPLICATIONS) {
                complications.setComplicationSupportedTypes(id, dataTypes)
                complications.putComplicationDrawable(id, drawable)
            }

            setActiveComplications(*complications.complicationIds)
        }

        private fun initializeBackground() {
            backgroundPaint.color = Color.BLACK
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            centerX = width / 2f
            centerY = height / 2f

            setComplicationsBounds(width)
        }

        /**
         * Setting up 16 complications in a 4x4 grid
         */
        private fun setComplicationsBounds(width: Int) {
            val complicationSize = width / 6
            var id = 0

            for (i in 1..4) {
                val yOffset = i * complicationSize

                for (j in 1..4) {
                    val xOffset = j * complicationSize

                    val bounds = Rect(xOffset, yOffset, xOffset + complicationSize, yOffset + complicationSize)
                    complications.setDrawableBounds(id, bounds)
                    id++
                }
            }
        }

        override fun onPropertiesChanged(properties: Bundle?) {
            lowBitAmbient = properties!!.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            burnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)

            complications.onPropertiesChanged(lowBitAmbient, burnInProtection)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)

            ambient = isInAmbientMode

            complications.onAmbientModeChanged(inAmbientMode)

            updateTimer()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)

            complications.drawComplications(canvas, now)
        }

        private fun drawBackground(canvas: Canvas) {
            if (ambient && (lowBitAmbient || burnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else {
                canvas.drawPaint(backgroundPaint)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            updateTimer()
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }

            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@BusyFaceService.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }

            registeredTimeZoneReceiver = false
            this@BusyFaceService.unregisterReceiver(timeZoneReceiver)
        }

        private fun shouldTimerBeRunning() : Boolean {
            return (isVisible && !isInAmbientMode)
        }

        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }
    }
}