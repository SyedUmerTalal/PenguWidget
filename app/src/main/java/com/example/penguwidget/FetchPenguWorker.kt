package com.example.penguwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.penguwidget.repository.PenguRepository
import com.example.penguwidget.utils.Constants
import com.example.penguwidget.utils.Constants.AUTH_TOKEN_KEY
import com.example.penguwidget.utils.Constants.FETCH_PENGU_WORKER
import com.example.penguwidget.utils.Constants.PENGU_ID_KEY
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class FetchPenguWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val penguRepository: PenguRepository,
) :
    CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val penguId = workerParameters.inputData.getString(PENGU_ID_KEY)
        val authToken = workerParameters.inputData.getString(AUTH_TOKEN_KEY)

        Log.d(FETCH_PENGU_WORKER, "Fetching Pengu with ID: $penguId, AuthToken: $authToken")

        return withContext(Dispatchers.IO) {
            return@withContext try {
                val pengu = penguRepository.getPengu(penguId!!, authToken!!)

                Log.d(FETCH_PENGU_WORKER, "Pengu fetched successfully")

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds =
                    appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
                appWidgetIds.forEach { appWidgetId ->
                    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget).apply {
                        setTextViewText(
                            R.id.owner_initial_text,
                            pengu.owner.name.first().toString()
                        )
                        setTextViewText(
                            R.id.friend_initial_text,
                            pengu.friend.name.first().toString()
                        )
                    }
                    AppWidget.apply {
                        updateHungerSaturation(pengu.hungerSaturation, remoteViews)
                        updateLoveSaturation(context, pengu.loveSaturation, remoteViews)
                        updateWallpaper(pengu.wallpaper.imageUrl, context, appWidgetId, remoteViews)
                        updateLayers(
                            pengu.previewImages.map { it.imageUrl }.toTypedArray(),
                            context,
                            appWidgetId,
                            remoteViews
                        )
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }

                Result.success()
            } catch (throwable: Throwable) {
                Log.e(FETCH_PENGU_WORKER, "Failed to fetch Pengu", throwable)
                Result.failure()
            }
        }
    }
}