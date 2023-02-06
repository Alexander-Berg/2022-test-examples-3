package ru.yandex.direct.core.testing.info.banner

import ru.yandex.direct.core.entity.banner.model.Banner
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.dbutil.model.ClientId

abstract class BannerInfo<T : Banner> {
    lateinit var adGroupInfo: AdGroupInfo<*>
    lateinit var banner: T

    val shard: Int get() = adGroupInfo.shard
    val uid: Long get() = adGroupInfo.uid

    val clientInfo: ClientInfo get() = adGroupInfo.clientInfo
    val clientId: ClientId? get() = adGroupInfo.clientId

    val campaignId: Long get() = adGroupInfo.campaignId
    val adGroupId: Long get() = adGroupInfo.adGroupId
    val bannerId: Long get() = banner.id

    fun isAdGroupInfoInitialized() = ::adGroupInfo.isInitialized
    fun isBannerInitialized() = ::banner.isInitialized

    open fun withAdGroupInfo(adGroupInfo: AdGroupInfo<*>) = apply {
        this.adGroupInfo = adGroupInfo
    }

    open fun withBanner(banner: T) = apply {
        this.banner = banner
    }

}
