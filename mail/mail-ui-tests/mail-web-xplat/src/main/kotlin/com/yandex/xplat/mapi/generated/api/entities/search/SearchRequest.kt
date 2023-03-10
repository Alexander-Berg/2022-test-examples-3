// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/search/search-request.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class SearchRequest private constructor(private val _path: String, private val first: Int, private val last: Int): MailNetworkRequest() {
    open override fun version(): NetworkAPIVersions {
        return NetworkAPIVersions.v1
    }

    open override fun method(): NetworkMethod {
        return NetworkMethod.post
    }

    open override fun path(): String {
        return this._path
    }

    open override fun encoding(): RequestEncoding {
        return JsonRequestEncoding()
    }

    open override fun params(): NetworkParams {
        return MapJSONItem().putInt32("first", this.first).putInt32("last", this.last)
    }

    open override fun urlExtra(): NetworkUrlExtra {
        val result = super.urlExtra()
        result.putInt32(SearchRequest.REQUEST_DISK_ATTACHES_QUERY_PARAM, 1)
        return result
    }

    companion object {
        @JvmStatic val REQUEST_DISK_ATTACHES_QUERY_PARAM: String = "request_disk_attaches"
        @JvmStatic
        open fun loadOnlyNew(first: Int, last: Int): SearchRequest {
            return SearchRequest("only_new", first, last)
        }

        @JvmStatic
        open fun loadWithAttachments(first: Int, last: Int): SearchRequest {
            return SearchRequest("with_attachments", first, last)
        }

    }
}

