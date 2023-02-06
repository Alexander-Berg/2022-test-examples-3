package ru.yandex.direct.core.testing.info

import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo

class NewKeywordInfo(
    var adGroupInfo: AdGroupInfo<*>,
    var keyword: Keyword = defaultKeyword(),
) {
    val shard: Int get() = adGroupInfo.shard
    val keywordId: Long get() = keyword.id

    val campaignId: Long get() = adGroupInfo.campaignId
    val adGroupId: Long get() = adGroupInfo.adGroupId
}
