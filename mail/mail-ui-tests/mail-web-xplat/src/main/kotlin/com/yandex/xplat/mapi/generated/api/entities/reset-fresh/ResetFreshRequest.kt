// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/reset-fresh/reset-fresh-request.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class ResetFreshRequest: MailNetworkRequest() {
    open override fun version(): NetworkAPIVersions {
        return NetworkAPIVersions.v1
    }

    open override fun method(): NetworkMethod {
        return NetworkMethod.post
    }

    open override fun path(): String {
        return "reset_fresh"
    }

    open override fun encoding(): RequestEncoding {
        return JsonRequestEncoding()
    }

    open override fun params(): NetworkParams {
        return MapJSONItem()
    }

}

