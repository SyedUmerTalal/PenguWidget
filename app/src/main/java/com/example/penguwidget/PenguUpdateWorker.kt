package com.example.penguwidget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.penguwidget.utils.Constants.HUNGER_SATURATION_KEY
import com.example.penguwidget.utils.Constants.LOVE_SATURATION_KEY
import com.example.penguwidget.utils.Constants.PENGU_UPDATE_WORKER
import com.example.penguwidget.utils.Constants.WALLPAPER_IMAGE_URL_KEY
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@HiltWorker
class PenguUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val socket: Socket
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                socket.run {
                    on(PENGU_WALLPAPER_CHANGED_EVENT) { args ->
                        val data = args[0] as JSONObject
                        val wallpaper = data.getJSONObject("wallpaper")
                        val wallpaperImageUrl = wallpaper.getString("url")

                        broadcastActionPenguWallpaperChanged(context, wallpaperImageUrl)
                    }

                    on(PENGU_FED_EVENT) { args ->
                        val data = args[0] as JSONObject
                        val saturation = data.getInt("saturation")

                        broadcastActionPenguFed(context, saturation)
                    }

                    on(PENGU_LOVED_EVENT) { args ->
                        val data = args[0] as JSONObject
                        val love = data.getInt("love")

                        broadcastActionPenguLoved(context, love)
                    }

                    connect()
                }

                Result.success()
            } catch (throwable: Throwable) {
                Log.e(PENGU_UPDATE_WORKER, "Failed to fetch Pengu", throwable)
                Result.failure()
            }
        }
    }

    companion object {
        const val PENGU_WALLPAPER_CHANGED_EVENT = "pengu:wallpaper_changed"
        const val PENGU_FED_EVENT = "pengu:fed"
        const val PENGU_LOVED_EVENT = "pengu:loved"

        fun broadcastActionPenguWallpaperChanged(context: Context, wallpaperImageUrl: String) {
            val intent = Intent(context, AppWidget::class.java).apply {
                action = AppWidget.ACTION_PENGU_WALLPAPER_CHANGED
                putExtra(WALLPAPER_IMAGE_URL_KEY, wallpaperImageUrl)
            }
            context.sendBroadcast(intent)
        }

        fun broadcastActionPenguFed(context: Context, hungerSaturation: Int) {
            val intent = Intent(context, AppWidget::class.java).apply {
                action = AppWidget.ACTION_PENGU_FED
                putExtra(HUNGER_SATURATION_KEY, hungerSaturation)
            }
            context.sendBroadcast(intent)
        }

        fun broadcastActionPenguLoved(context: Context, loveSaturation: Int) {
            val intent = Intent(context, AppWidget::class.java).apply {
                action = AppWidget.ACTION_PENGU_LOVED
                putExtra(LOVE_SATURATION_KEY, loveSaturation)
            }
            context.sendBroadcast(intent)
        }
    }

}