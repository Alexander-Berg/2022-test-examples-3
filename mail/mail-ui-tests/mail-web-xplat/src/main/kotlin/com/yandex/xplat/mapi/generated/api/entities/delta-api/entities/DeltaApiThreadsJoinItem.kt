// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/delta-api/entities/delta-api-threads-join-item.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.*

public open class DeltaApiThreadsJoinItem private constructor(val mid: ID, val tid: ID, val lids: YSArray<LabelID>): DeltaApiItem {
    override val kind: DeltaApiItemKind = DeltaApiItemKind.threadsJoin
    companion object {
        @JvmStatic
        open fun fromJSONItem(jsonItem: JSONItem): DeltaApiThreadsJoinItem? {
            if (jsonItem.kind != JSONItemKind.map) {
                return null
            }
            val map = jsonItem as MapJSONItem
            val mid = map.getInt64("mid")
            if (mid == null) {
                return null
            }
            val tid = map.getInt64("tid")
            if (tid == null) {
                return null
            }
            val lids = map.getArrayOrDefault("labels", mutableListOf()).map( {
                item ->
                (item as StringJSONItem).value
            })
            return DeltaApiThreadsJoinItem(mid!!, tid!!, lids)
        }

    }
}

