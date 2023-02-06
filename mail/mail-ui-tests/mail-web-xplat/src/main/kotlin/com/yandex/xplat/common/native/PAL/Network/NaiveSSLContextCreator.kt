package com.yandex.xplat.common

import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NaiveSSLContextCreator : SSLContextCreator {
    private val trustAllCerts: Array<TrustManager> = arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    override fun createSSLConfiguredClient(httpBuilder: OkHttpClient.Builder): OkHttpClient.Builder {
        val sslContext = SSLContext.getInstance("SSL")
        if (sslContext != null) {
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            httpBuilder
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        }
        return httpBuilder
    }
}
