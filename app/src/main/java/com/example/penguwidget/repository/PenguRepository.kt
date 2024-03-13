package com.example.penguwidget.repository

import com.example.penguwidget.models.Pengu
import com.example.penguwidget.retrofit.PenguService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PenguRepository(
    private val penguService: PenguService,
) {
    suspend fun getPengu(penguId: String, authToken: String): Pengu {
        return withContext(Dispatchers.IO) {
            return@withContext penguService.getPengu(penguId, authToken).body()!!
        }
    }
}