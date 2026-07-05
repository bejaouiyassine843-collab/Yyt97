package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("type") type: String = "video",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("maxResults") maxResults: Int = 24
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getPopularVideos(
        @Query("part") part: String = "snippet,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("key") apiKey: String,
        @Query("regionCode") regionCode: String = "US",
        @Query("maxResults") maxResults: Int = 24
    ): YouTubeVideoResponse

    companion object {
        private const val BASE_URL = "https://youtube.googleapis.com/youtube/v3/"

        fun create(): YouTubeApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YouTubeApiService::class.java)
        }
    }
}
