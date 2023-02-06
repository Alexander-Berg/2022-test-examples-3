package ru.yandex.direct.core.testing.steps

import org.jooq.DSLContext
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.bidmodifier.BidModifier
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository
import ru.yandex.direct.core.testing.data.TestBidModifiers
import ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics
import ru.yandex.direct.core.testing.info.NewAdGroupBidModifierInfo
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.core.testing.steps.adgroup.AdGroupStepsFactory
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import java.time.LocalDateTime

@Component
class NewBidModifierSteps(
    val dslContextProvider: DslContextProvider,
    val adGroupStepsFactory: AdGroupStepsFactory,
    val bidModifierRepository: BidModifierRepository,
) {

    fun createDefaultAdGroupBidModifierDemographics(adGroupInfo: AdGroupInfo<*>): NewAdGroupBidModifierInfo {
        return createAdGroupBidModifier(createDefaultBidModifierDemographics(null), adGroupInfo)
    }

    fun createAdGroupBidModifier(bidModifier: BidModifier, adGroupInfo: AdGroupInfo<*>): NewAdGroupBidModifierInfo {
        val bidModifierInfo = NewAdGroupBidModifierInfo(adGroupInfo = adGroupInfo, bidModifiers = listOf(bidModifier))
        return createAdGroupBidModifier(bidModifierInfo)
    }

    fun createAdGroupBidModifier(bidModifierInfo: NewAdGroupBidModifierInfo): NewAdGroupBidModifierInfo {
        if (bidModifierInfo.bidModifiers == null) {
            bidModifierInfo.bidModifier = TestBidModifiers.createDefaultBidModifierMobile(null)
        }

        if (bidModifierInfo.adGroupInfo.adGroup.id == null) {
            adGroupStepsFactory.createAdGroup(bidModifierInfo.adGroupInfo)
        }

        if (bidModifierInfo.bidModifierId == null) {
            bidModifierInfo.bidModifier!!
                .withCampaignId(bidModifierInfo.campaignId)
                .withAdGroupId(bidModifierInfo.adGroupId)
                .withLastChange(LocalDateTime.now())
            val dslContext: DSLContext = dslContextProvider.ppc(bidModifierInfo.shard)
            bidModifierRepository.addModifiers(dslContext, listOf(bidModifierInfo.bidModifier),
                emptyMap(), bidModifierInfo.clientId!!, bidModifierInfo.uid)
        }
        return bidModifierInfo
    }
}
