package ru.yandex.direct.core.testing.steps.banner

import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.core.testing.info.banner.BannerInfo
import ru.yandex.direct.core.testing.steps.adgroup.AdGroupStepsFactory
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import kotlin.reflect.KClass

abstract class BannerSteps<B : BannerWithSystemFields, T : BannerInfo<B>>(
    private val adGroupStepsFactory: AdGroupStepsFactory,
    private val dslContextProvider: DslContextProvider,
    private val bannerModifyRepository: BannerModifyRepository,
    private val allowedAdGroupClasses: Set<KClass<out AdGroup>>,
) {
    fun createDefaultBanner(): T {
        return createBanner(getBannerInfo())
    }

    @Suppress("UNCHECKED_CAST")
    fun createDefaultBanner(adGroupInfo: AdGroupInfo<*>): T {
        return createBanner(getBannerInfo().withAdGroupInfo(adGroupInfo) as T)
    }

    @Suppress("UNCHECKED_CAST")
    fun createBanner(adGroupInfo: AdGroupInfo<*>, banner: B): T {
        return createBanner(getBannerInfo().withAdGroupInfo(adGroupInfo).withBanner(banner) as T)
    }

    fun createBanner(bannerInfo: T): T {
        checkBannerInfoConsistency(bannerInfo)

        initializeBannerInfo(bannerInfo)

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.adGroupInfo.adGroup.id == null) {
            adGroupStepsFactory.createAdGroup(bannerInfo.adGroupInfo)
        }

        createRelations(bannerInfo)

        val banner = bannerInfo.banner
        // дозаполняем поля баннера перед сохранением
        banner.adGroupId = bannerInfo.adGroupId
        banner.campaignId = bannerInfo.campaignId

        val container = BannerRepositoryContainer(bannerInfo.shard)

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.shard), container, listOf(banner))

        return bannerInfo
    }

    protected abstract fun initializeBannerInfo(bannerInfo: T)

    protected abstract fun getBannerInfo(): T

    abstract fun getBannerInfoClass(): KClass<T>

    protected open fun createRelations(bannerInfo: T) {
    }

    protected open fun checkBannerInfoConsistency(bannerInfo: T) {
        if (bannerInfo.adGroupInfo.isAdGroupInitialized()) {
            val adGroup: AdGroup = bannerInfo.adGroupInfo.adGroup
            check(allowedAdGroupClasses.contains(adGroup::class)) {
                "adGroup class must be in whiteList, but current adGroup has class " + adGroup::class
            }
        }
    }
}


