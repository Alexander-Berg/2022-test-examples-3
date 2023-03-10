// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/network/auth-network-interceptor.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class AuthNetworkInterceptor(private val account: Account, private val tokenProvider: TokenProvider): NetworkInterceptor {
    open override fun intercept(originalRequest: NetworkRequest): XPromise<NetworkRequest> {
        return this.tokenProvider.obtain(this.account).then( {
            token ->
            SealedNetworkRequest(originalRequest.method(), originalRequest.targetPath(), originalRequest.params(), originalRequest.urlExtra(), this.updateHeadersExtra(originalRequest.headersExtra(), token), originalRequest.encoding())
        })
    }

    private fun updateHeadersExtra(headersExtra: NetworkHeadersExtra, token: Token): NetworkHeadersExtra {
        return headersExtra.putString("Authorization", token.asHeaderValue())
    }

}

