package com.example.penguwidget.dagger

import android.util.Log
import com.example.penguwidget.repository.PenguRepository
import com.example.penguwidget.retrofit.PenguService
import com.example.penguwidget.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun providePenguService(): PenguService {
        return Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(PenguService::class.java)
    }

    @Singleton
    @Provides
    fun providePenguRepository(
        penguService: PenguService,
    ): PenguRepository {
        return PenguRepository(penguService)
    }

    @Singleton
    @Provides
    fun provideSocket(
    ): Socket {
        return IO.socket(
            "https://api.penguapp.co",
            IO.Options
                .builder()
                .setTransports(arrayOf("websocket"))
                .setQuery("token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI4MGFjZTAzMC05YjExLTRhYTgtODZkMS1iMjY4MzJmNGIzYjIiLCJmaXJlYmFzZVVpZCI6IkV1cnhLYXphbWNRYTZKRVBDR0RaS0tGY3J2ODIiLCJwaG9uZU51bWJlciI6IisxMzEwMjM3ODY2MiIsImlhdCI6MTcwODQ2ODI1NH0.6JC162qy-B1kBbwVXju7473M8SIoeg2jFP1SKbLUP60")
                .build()
        )
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(WebSockets)
            install(ResponseObserver) {
                onResponse { response ->
                    Log.d("HTTP status:", "${response.status.value}")
                }
            }
            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }

}