package ru.yandex.direct.core.testing.steps

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository
import ru.yandex.direct.core.testing.data.defaultOfferRetargeting
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Component
class OfferRetargetingSteps @Autowired constructor(
    val dslContextProvider: DslContextProvider,
    private val offerRetargetingRepository: OfferRetargetingRepository
) {
    fun addOfferRetargetingsToAdGroup(
        offerRetargetings: List<OfferRetargeting>,
        adGroupInfo: AdGroupInfo
    ): List<Long> {
        return offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(adGroupInfo.shard).configuration(),
            adGroupInfo.clientId,
            offerRetargetings,
            setOf(adGroupInfo.adGroupId)
        ).map { it.id }
    }

    fun addOfferRetargetingToAdGroup(
        offerRetargeting: OfferRetargeting,
        adGroupInfo: AdGroupInfo
    ): OfferRetargeting {
        val ids = addOfferRetargetingsToAdGroup(listOf(offerRetargeting), adGroupInfo)
        return offerRetargetingRepository.getOfferRetargetingsByIds(
            adGroupInfo.shard,
            adGroupInfo.clientId,
            ids
        )[ids.single()]!!
    }

    fun defaultOfferRetargetingForGroup(adGroupInfo: AdGroupInfo): OfferRetargeting =
       defaultOfferRetargeting
            .withAdGroupId(adGroupInfo.adGroupId)
            .withCampaignId(adGroupInfo.campaignId)
}
