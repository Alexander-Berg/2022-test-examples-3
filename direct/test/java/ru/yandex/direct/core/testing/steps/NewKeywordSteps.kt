package ru.yandex.direct.core.testing.steps

import org.apache.commons.lang3.ObjectUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.testing.info.NewKeywordInfo
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.core.testing.steps.adgroup.AdGroupStepsFactory
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Component
class NewKeywordSteps(
    val dslContextProvider: DslContextProvider,
    val keywordRepository: KeywordRepository,
    val adGroupStepsFactory: AdGroupStepsFactory
) {

    fun createKeyword(adGroupInfo: AdGroupInfo<*>): NewKeywordInfo {
        return createKeyword(NewKeywordInfo(adGroupInfo = adGroupInfo))
    }

    fun createKeyword(adGroupInfo: AdGroupInfo<*>, keyword: Keyword): NewKeywordInfo {
        return createKeyword(NewKeywordInfo(keyword = keyword, adGroupInfo = adGroupInfo))
    }

    fun createKeyword(keywordInfo: NewKeywordInfo): NewKeywordInfo {
        if (keywordInfo.adGroupInfo.adGroup.id == null) {
            adGroupStepsFactory.createAdGroup(keywordInfo.adGroupInfo)
        }
        if (keywordInfo.keyword.id == null) {
            keywordInfo.keyword
                .withAdGroupId(keywordInfo.adGroupInfo.adGroupId)
                .withCampaignId(keywordInfo.adGroupInfo.campaignInfo.campaignId)
            ensureValidPrices(keywordInfo)
            val dslContext: DSLContext = dslContextProvider.ppc(keywordInfo.shard)
            keywordRepository.addKeywords(dslContext.configuration(), listOf(keywordInfo.keyword))
        }
        return keywordInfo
    }

    /**
     * проставить КФ валидные (минимальные) цены, если того требует ручная стратегия
     */
    private fun ensureValidPrices(keywordInfo: NewKeywordInfo) {
        val keyword = keywordInfo.keyword
        val clientCurrencyCode = keywordInfo.adGroupInfo.clientInfo.client!!.workCurrency
        val isCpm = keywordInfo.adGroupInfo.adGroupType.name.toLowerCase().contains("cpm")
        val minPrice = if (isCpm) clientCurrencyCode.currency.minCpmPrice else clientCurrencyCode.currency.minPrice
        // не определяем, какие ставки на самом деле нужны, т.к. в model0 это как-то сложно
        // в принципе, можно выставить сразу две, в базе тоже так бывает.
        val campaign = keywordInfo.adGroupInfo.campaignInfo.typedCampaign
        if (campaign is CampaignWithStrategy && !campaign.strategy.isAutoBudget) {
            if (ObjectUtils.compare(minPrice, keyword.price) > 0) {
                keyword.withPrice(minPrice)
            }
            if (ObjectUtils.compare(minPrice, keyword.priceContext) > 0) {
                keyword.withPriceContext(minPrice)
            }
        } else {
            if (keyword.autobudgetPriority == null) {
                keyword.withAutobudgetPriority(3)
            }
        }
    }
}
