// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM api/entities/thread/thread-meta.ts >>>

package com.yandex.xplat.mapi

import com.yandex.xplat.common.JSONItem
import com.yandex.xplat.common.JSONItemKind
import com.yandex.xplat.common.MapJSONItem
import com.yandex.xplat.common.nullthrows

public open class ThreadMeta(val scn: Long, val tid: ID, val fid: ID, val topMid: ID, val threadCount: Int = 1) {
}

public fun threadMetaFromJSONItem(item: JSONItem): ThreadMeta? {
    if (item.kind != JSONItemKind.map) {
        return null
    }
    val map = item as MapJSONItem
    val scn = nullthrows(map.getInt64("scn"))
    val tid = idFromString(map.getString("tid"))!!
    val fid = idFromString(map.getString("fid"))!!
    val topMid = idFromString(map.getString("mid"))!!
    val counter = map.getInt32OrDefault("threadCount", 1)
    return ThreadMeta(scn, tid, fid, topMid, counter)
}

