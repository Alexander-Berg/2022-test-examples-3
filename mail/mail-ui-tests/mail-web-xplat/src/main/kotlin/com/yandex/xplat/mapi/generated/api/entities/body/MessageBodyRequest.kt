// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/body/message-body-request.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class MessageBodyRequest(private val mids: YSArray<ID>): MailNetworkRequest() {
    open override fun version(): NetworkAPIVersions {
        return NetworkAPIVersions.v1
    }

    open override fun method(): NetworkMethod {
        return NetworkMethod.post
    }

    open override fun path(): String {
        return "message_body"
    }

    open override fun encoding(): RequestEncoding {
        return JsonRequestEncoding()
    }

    open override fun params(): NetworkParams {
        return MapJSONItem().putBoolean("novdirect", true).putString("mids", idsToString(this.mids))
    }

}
