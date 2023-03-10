// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/actions/push-subscription-network-request.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class PushSubscriptionNetworkRequest(private val device: String, private val pushToken: String, private val appName: String, private val osName: String, private val isSubscription: Boolean): MailNetworkRequest() {
    open override fun version(): NetworkAPIVersions {
        return NetworkAPIVersions.v1
    }

    open override fun method(): NetworkMethod {
        return NetworkMethod.`get`
    }

    open override fun path(): String {
        return "push"
    }

    open override fun params(): NetworkParams {
        return MapJSONItem().putString("device", this.device).putString("push_token", this.pushToken).putString("app_name", this.appName).putString("os", this.osName)
    }

    open override fun encoding(): RequestEncoding {
        return JsonRequestEncoding()
    }

    open override fun urlExtra(): NetworkUrlExtra {
        val baseExtra = super.urlExtra()
        if (!this.isSubscription) {
            baseExtra.putString("unsubscribe", "yes")
        }
        return baseExtra
    }

}

