package ru.yandex.market.sc.test.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import ru.yandex.market.sc.core.network.api.*
import ru.yandex.market.sc.core.network.di.OkHttpClientModule
import ru.yandex.market.sc.test.network.api.AllureInterceptor
import java.util.concurrent.TimeUnit

@Module
@TestInstallIn(
    replaces = [OkHttpClientModule::class],
    components = [SingletonComponent::class],
)
class E2eOkHttpClientModule {
    @Provides
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        headerInterceptor: HeaderInterceptor,
        loggingInterceptor: LoggingInterceptor,
        errorInterceptor: ErrorInterceptor,
        allureInterceptor: AllureInterceptor,
        sslFactoryManager: YandexSSLFactoryManager,
        pathInterceptor: PathInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(errorInterceptor)
            .addInterceptor(allureInterceptor)
            .addInterceptor(pathInterceptor)
            .callTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(0L, TimeUnit.SECONDS)
            .readTimeout(0L, TimeUnit.SECONDS)
            .writeTimeout(0L, TimeUnit.SECONDS)
            .sslSocketFactory(sslFactoryManager.socketFactory, sslFactoryManager.trustManager)
            .build()
    }

    companion object {
        private const val REQUEST_TIMEOUT_SECONDS = 30L
    }
}
