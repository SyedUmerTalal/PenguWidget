package com.example.penguwidget.retrofit

import com.example.penguwidget.models.Pengu
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path


interface PenguService {
    @GET("v1/pengu/{id}")
    suspend fun getPengu(
        @Path("id") penguId: String,
        @Header("Authorization") token: String,
    ): Response<Pengu>
}