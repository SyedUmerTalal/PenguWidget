package com.example.penguwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import  androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.penguwidget.utils.Constants.AUTH_TOKEN_KEY
import com.example.penguwidget.utils.Constants.FETCH_PENGU_WORKER
import com.example.penguwidget.utils.Constants.HUNGER_SATURATION_KEY
import com.example.penguwidget.utils.Constants.LOVE_SATURATION_KEY
import com.example.penguwidget.utils.Constants.PENGU_ID_KEY
import com.example.penguwidget.utils.Constants.PENGU_UPDATE_WORKER
import com.example.penguwidget.utils.Constants.WALLPAPER_IMAGE_URL_KEY
import java.util.concurrent.TimeUnit

private const val TAG = "PenguAppWidget"

class AppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?
    ) {
        val inputData = workDataOf(
            PENGU_ID_KEY to "9f7a035f-67e1-450c-bf2e-a9a3352cf350",
            AUTH_TOKEN_KEY to "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI4MGFjZTAzMC05YjExLTRhYTgtODZkMS1iMjY4MzJmNGIzYjIiLCJmaXJlYmFzZVVpZCI6IkV1cnhLYXphbWNRYTZKRVBDR0RaS0tGY3J2ODIiLCJwaG9uZU51bWJlciI6IisxMzEwMjM3ODY2MiIsImlhdCI6MTcwODQ2ODI1NH0.6JC162qy-B1kBbwVXju7473M8SIoeg2jFP1SKbLUP60",
        )
        val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
        val request = PeriodicWorkRequestBuilder<FetchPenguWorker>(
            repeatInterval = 15, repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).setInputData(inputData).setConstraints(constraints).build()

        context?.let {
            WorkManager.getInstance(it).enqueueUniquePeriodicWork(
                FETCH_PENGU_WORKER, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FETCH_PENGU_WORKER)
        WorkManager.getInstance(context).cancelUniqueWork(PENGU_UPDATE_WORKER)
    }

    override fun onEnabled(context: Context?) {
        context?.let {
            val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
            val request =
                OneTimeWorkRequestBuilder<PenguUpdateWorker>().setConstraints(constraints)
                    .build()
            WorkManager.getInstance(it).enqueueUniqueWork(
                PENGU_UPDATE_WORKER,
                ExistingWorkPolicy.KEEP, request
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")

        super.onReceive(context, intent)

        intent?.let {
            when (it.action) {
                ACTION_PENGU_FED -> handleActionPenguFed(context, it)
                ACTION_PENGU_LOVED -> handleActionPenguLoved(context, it)
                ACTION_PENGU_WALLPAPER_CHANGED -> handleActionPenguWallpaperUpdate(context, it)
            }
        }
    }

    private fun handleActionPenguFed(context: Context?, intent: Intent) {
        context?.let {
            val hungerSaturation = intent.getIntExtra(HUNGER_SATURATION_KEY, 0)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds =
                appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
            appWidgetIds?.let {
                it.forEach { appWidgetId ->
                    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget)
                    updateHungerSaturation(hungerSaturation, remoteViews)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun handleActionPenguLoved(context: Context?, intent: Intent) {
        context?.let {
            val loveSaturation = intent.getIntExtra(LOVE_SATURATION_KEY, 0)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds =
                appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
            appWidgetIds?.let {
                it.forEach { appWidgetId ->
                    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget)
                    updateLoveSaturation(context, loveSaturation, remoteViews)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun handleActionPenguWallpaperUpdate(context: Context?, intent: Intent) {
        context?.let {
            val wallpaperImageUrl = intent.getStringExtra(WALLPAPER_IMAGE_URL_KEY)
            wallpaperImageUrl?.let {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds =
                    appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
                appWidgetIds?.let {
                    it.forEach { appWidgetId ->
                        val remoteViews = RemoteViews(context.packageName, R.layout.app_widget)
                        updateWallpaper(wallpaperImageUrl, context, appWidgetId, remoteViews)
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_PENGU_FED = "com.example.penguwidget.action.APPWIDGET_" + "HUNGER_UPDATE"
        const val ACTION_PENGU_WALLPAPER_CHANGED =
            "com.example.penguwidget.action.APPWIDGET_" + "WALLPAPER_UPDATE"
        const val ACTION_PENGU_LOVED = "com.example.penguwidget.action.APPWIDGET_" + "LOVE_UPDATE"

        fun updateHungerSaturation(hungerSaturation: Int, remoteViews: RemoteViews) {
            val hungerLevel = hungerSaturation * 100
            remoteViews.setInt(R.id.hunger_progress_image, "setImageLevel", hungerLevel)
        }

        fun updateLoveSaturation(context: Context, loveSaturation: Int, remoteViews: RemoteViews) {
            val parentDrawable = context.getDrawable(R.drawable.heart_saturation) as LayerDrawable
            val maxLevel = loveSaturation.coerceIn(0, 100) / 25f * 10000f

            for (index in 0..3) {
                val layerDrawable = parentDrawable.getDrawable(3 - index) as LayerDrawable
                layerDrawable.findDrawableByLayerId(R.id.indicator).apply {
                    level = (maxLevel - index * 10000f).toInt().coerceAtLeast(0)
                }
            }

            remoteViews.setImageViewBitmap(R.id.love_saturation_image, parentDrawable.toBitmap())
        }

        fun updateLayers(
            layers: Array<String>,
            context: Context,
            appWidgetId: Int,
            remoteViews: RemoteViews
        ) {
            val layerList = arrayListOf<Drawable>()
            layers.forEach {
                Glide.with(context).load(it).override(600, 600).centerInside()
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?,
                        ) {
                            layerList.add(resource)

                            if (layerList.size == layers.size) {
                                val layerDrawable = LayerDrawable(layerList.toTypedArray())
                                val bitmap = layerDrawable.toBitmap()
                                remoteViews.setImageViewBitmap(R.id.appwidget_pengu_image, bitmap)
                                AppWidgetManager.getInstance(context)
                                    .partiallyUpdateAppWidget(appWidgetId, remoteViews)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }

        fun updateWallpaper(
            wallpaperImageUrl: String,
            context: Context,
            appWidgetId: Int,
            remoteViews: RemoteViews
        ) {
            val wallpaperWidgetTarget = AppWidgetTarget(
                context,
                R.id.appwidget_background,
                remoteViews, appWidgetId
            )

            Glide.with(context.applicationContext).asBitmap().load(wallpaperImageUrl)
                .into(wallpaperWidgetTarget)
        }
    }
}