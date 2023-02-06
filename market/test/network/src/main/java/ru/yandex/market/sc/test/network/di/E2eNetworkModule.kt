package ru.yandex.market.sc.test.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import ru.yandex.market.sc.core.network.util.InformativeHttpExceptionsCallAdapter
import ru.yandex.market.sc.test.network.api.AndroidTestEnvironment
import ru.yandex.market.sc.test.network.api.SortingCenterInternalService
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import ru.yandex.market.sc.test.network.api.SortingCenterPartnerService

@Module
@InstallIn(SingletonComponent::class)
class E2eNetworkModule {
    @Provides
    fun provideSortingCenterManualService(
        okHttpClient: OkHttpClient,
        converterFactory: Converter.Factory,
    ): SortingCenterManualService {
        return Retrofit.Builder()
            .baseUrl(AndroidTestEnvironment.manual)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(InformativeHttpExceptionsCallAdapter.Factory)
            .build()
            .create(SortingCenterManualService::class.java)
    }

    @Provides
    fun provideSortingCenterInternalService(
        okHttpClient: OkHttpClient,
        converterFactory: Converter.Factory,
    ): SortingCenterInternalService {
        return Retrofit.Builder()
            .baseUrl(AndroidTestEnvironment.internal)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(InformativeHttpExceptionsCallAdapter.Factory)
            .build()
            .create(SortingCenterInternalService::class.java)
    }

    @Provides
    fun provideSortingCenterPartnerServiceService(
        okHttpClient: OkHttpClient,
        converterFactory: Converter.Factory,
    ): SortingCenterPartnerService {
        return Retrofit.Builder()
            .baseUrl(AndroidTestEnvironment.partner)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(InformativeHttpExceptionsCallAdapter.Factory)
            .build()
            .create(SortingCenterPartnerService::class.java)
    }
}
