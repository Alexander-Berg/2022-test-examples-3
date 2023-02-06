package ru.yandex.market.sc.test.network.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Singleton
class YandexSSLFactoryManager @Inject constructor() {
    val socketFactory: SSLSocketFactory
    val trustManager: X509TrustManager

    init {
        val certificate = runBlocking {
            withContext(Dispatchers.IO) {
                URL(CERTIFICATE_URL)
                    .openStream()
                    .use(CertificateFactory.getInstance("X.509")::generateCertificate)
            }
        }

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }

        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        socketFactory = sslContext.socketFactory
        trustManager = trustManagerFactory.trustManagers[0] as X509TrustManager
    }

    private companion object {
        private const val CERTIFICATE_URL = "https://crls.yandex.net/YandexInternalRootCA.crt"
    }
}
