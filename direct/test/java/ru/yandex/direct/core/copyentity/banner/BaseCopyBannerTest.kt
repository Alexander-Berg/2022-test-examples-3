package ru.yandex.direct.core.copyentity.banner

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyConfigBuilder
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.copyentity.CopyOperationFactory
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.banner.model.Banner
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.banner.service.BannerService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewBannerInfo
import ru.yandex.direct.core.testing.steps.Steps

abstract class BaseCopyBannerTest : AbstractSpringTest() {

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var copyOperationFactory: CopyOperationFactory

    @Autowired
    protected lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    protected lateinit var bannerService: BannerService

    protected lateinit var client: ClientInfo

    protected fun sameAdGroupBannerCopyOperation(
        banner: NewBannerInfo,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<BannerWithAdGroupId, Long> {
        val config = CopyConfigBuilder(
            banner.clientId,
            banner.clientId,
            banner.uid,
            BannerWithAdGroupId::class.java,
            listOf(banner.bannerId),
        )
            .withFlags(flags)
            .withParentIdMapping(AdGroup::class.java, banner.adGroupId, banner.adGroupId)
            .build()
        return copyOperationFactory.build(config)
    }

    protected fun copyValidBanner(
        copyOperation: CopyOperation<BannerWithAdGroupId, Long>,
        allowWarnings: Boolean = false,
    ) = CopyEntityTestUtils.copyValidEntity(BannerWithAdGroupId::class.java, copyOperation, allowWarnings)

    protected fun <T : Banner> copyValidBanner(
        banner: NewBannerInfo,
        allowWarnings: Boolean = false,
    ): T {
        val copyOperation = sameAdGroupBannerCopyOperation(banner)
        val bannerId = copyValidBanner(copyOperation, allowWarnings).first()
        return getBanner(bannerId)
    }

    protected fun <T : Banner> getBanner(bannerId: Long): T {
        @Suppress("UNCHECKED_CAST")
        return bannerService.getBannersByIds(listOf(bannerId)).first() as T
    }

}
