// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/actions/save-mail-network-request.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.JsonRequestEncoding
import com.yandex.xplat.common.NetworkMethod
import com.yandex.xplat.common.NetworkParams
import com.yandex.xplat.common.RequestEncoding

public open class SaveMailNetworkRequest(val task: MailSendRequest): MailNetworkRequest() {
    open override fun version(): NetworkAPIVersions {
        return NetworkAPIVersions.v1
    }

    open override fun method(): NetworkMethod {
        return NetworkMethod.post
    }

    open override fun path(): String {
        return "store"
    }

    open override fun encoding(): RequestEncoding {
        return JsonRequestEncoding()
    }

    open override fun params(): NetworkParams {
        return this.task.asMapJSONItem()
    }

}
