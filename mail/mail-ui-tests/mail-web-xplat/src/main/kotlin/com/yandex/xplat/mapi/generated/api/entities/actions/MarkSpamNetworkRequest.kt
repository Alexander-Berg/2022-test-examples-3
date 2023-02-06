// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/actions/mark-spam-network-request.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class MarkSpamNetworkRequest(private val mids: YSArray<ID>, private val tids: YSArray<ID>, private val currentFolderFid: ID, private val isSpam: Boolean): MailNetworkRequest() {
    open override fun version(): NetworkAPIVersions {
        return NetworkAPIVersions.v1
    }

    open override fun method(): NetworkMethod {
        return NetworkMethod.post
    }

    open override fun path(): String {
        return if (this.isSpam) "foo" else "antifoo"
    }

    open override fun params(): NetworkParams {
        val params = MapJSONItem()
        if (this.mids.size > 0) {
            params.putString("mids", idsToString(this.mids))
        }
        if (this.tids.size > 0) {
            params.putString("tids", idsToString(this.tids))
        }
        return params.putString("current_folder", idToString(this.currentFolderFid)!!)
    }

    open override fun encoding(): RequestEncoding {
        return JsonRequestEncoding()
    }

}
