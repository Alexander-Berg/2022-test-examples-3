package ru.yandex.direct.core.copyentity.testing

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import ru.yandex.direct.core.entity.addition.callout.model.Callout
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.bidmodifier.BidModifier
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.core.entity.retargeting.model.Retargeting
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.model.Entity
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyTimeComparator

object CopyAssertStrategies {

    val CAMPAIGN_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "orderId", "productId", "uid", "clientId", "walletId", "lastChange", "dayBudgetLastChange",
            "statusBsSynced", "statusModerate", "statusActive", "statusPostModerate", "name", "fio",
            "attributionModel", "contextLimit", "geo", "contextPriceCoef", "broadMatch", "broadMatchGoalId",
            "sumLast", "sumSpent", "sum", "createTime", "copiedFrom", "promoExtensionId",
            "bidModifiers.id", "bidModifiers.campaignId", "bidModifiers.adGroupId", "bidModifiers.lastChange",
            "strategy.strategyData.lastBidderRestartTime", "strategyId",
        )
        .withIgnoredFieldsMatchingRegexes("^bidModifiers\\..*\\.id$", "^bidModifiers\\..*\\.lastChange$")
        .withComparatorForFields(approximatelyTimeComparator(180), "dayBudgetLastChange")
        .build()

    val ADGROUP_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "campaignId", "statusPostModerate", "statusModerate", "name", "lastChange",
            "geo", "statusBsSynced", "priorityId",
            "usersSegments.adGroupId", "usersSegments.timeCreated", "usersSegments.lastSuccessUpdateTime",
        )
        .build()

    val BANNER_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "adGroupId", "campaignId", "bsBannerId", "domainId", "lastChange", "statusBsSynced",
            "statusPostModerate", "displayHrefStatusModerate", "statusModerate", "statusActive",
            "creativeRelationId", "creativeStatusModerate", "language", "geoFlag",
            "yaContextCategories", "imageDateAdded", "imageStatusModerate", "imageBannerId", "imageId",
            "showTitleAndBody" //бывает false при чтении баннера, в котором null при добавлении
        )
        .build()

    val VCARD_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "addressId", "uid", "orgDetailsId", "campaignId", "manualPoint.id", "precision",
            "autoPoint.x", "autoPoint.y1", "autoPoint.x1", "autoPoint.y", "autoPoint.y2",
            "autoPoint.x2", "autoPoint.id", "lastDissociation", "lastChange", "pointType",
        )
        .build()

    val SITELINK_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "clientId", "linksHash", "sitelinks.id")
        .build()

    val BID_MODIFIERS_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoreAllExpectedNullFields(true)
        .withIgnoredFields("id", "campaignId", "adGroupId", "lastChange")
        .withIgnoredFieldsMatchingRegexes("^.*\\.id$", "^.*\\.lastChange$")
        .build()

    val KEYWORD_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "adGroupId", "campaignId", "lastChange", "modificationTime", "needCheckPlaceModified",
            "showsForecast", "place", "isSuspended",
        )
        .build()

    val RETARGETING_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "adGroupId", "campaignId", "lastChangeTime", "statusBsSynced")
        .build()

    val RELEVANCE_MATCH_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "adGroupId", "campaignId", "lastChangeTime", "isSuspended", "isDeleted",
        )
        .build()

    val DYNAMIC_ADTARGET_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            "id", "dynamicConditionId", "adGroupId", "campaignId", "price", "priceContext",
            "statusBsSynced",
        )
        .build()

    val BANNER_IMAGE_POOL_COMPARE_STRATEGY: RecursiveComparisonConfiguration =
        RecursiveComparisonConfiguration.builder()
            .withIgnoredFields("id", "clientId", "createTime")
            .build()

    val CALLOUT_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "clientId", "statusModerate", "createTime", "lastChange")
        .build()

    val PROMO_EXTENSION_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "promoExtensionId", "clientId", "statusModerate")
        .build()

    val MOBILE_APP_COMPARE_STRATEGY: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "clientId", "mobileContentId", "mobileContent.clientId", "mobileContent.id")
        .build()

    val CAMP_METRIKA_GOAL: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id.campaignId", // цель принадлежит скопированной кампании
        "campaignId", // цель принадлежит скопированной кампании

        // TODO https://a.yandex-team.ru/arc_vcs/direct/core/src/main/java/ru/yandex/direct/core/copyentity/preprocessors/CampMetrikaGoalPreprocessor.kt?rev=r9176280#L14
        // "statDate", // сбрасываем время обновления статистики (дефолтное значение поля)
        // "goalsCount", // сбрасываем количество достижений цели (дефолтное значение поля)
        // "contextGoalsCount", // сбрасываем количество достижений цели (дефолтное значение поля)
    ).build()

    val STRATEGY_BY_ENTITIES: Map<Class<out Entity<*>>, RecursiveComparisonConfiguration> = mapOf(
        BaseCampaign::class.java to CAMPAIGN_COMPARE_STRATEGY,
        AdGroup::class.java to ADGROUP_COMPARE_STRATEGY,
        BannerWithAdGroupId::class.java to BANNER_COMPARE_STRATEGY,
        Vcard::class.java to VCARD_COMPARE_STRATEGY,
        SitelinkSet::class.java to SITELINK_COMPARE_STRATEGY,
        BidModifier::class.java to BID_MODIFIERS_COMPARE_STRATEGY,
        Keyword::class.java to KEYWORD_COMPARE_STRATEGY,
        Retargeting::class.java to RETARGETING_COMPARE_STRATEGY,
        RelevanceMatch::class.java to RELEVANCE_MATCH_COMPARE_STRATEGY,
        DynamicAdTarget::class.java to DYNAMIC_ADTARGET_COMPARE_STRATEGY,
        Callout::class.java to CALLOUT_COMPARE_STRATEGY,
        PromoExtension::class.java to PROMO_EXTENSION_COMPARE_STRATEGY,
        MobileApp::class.java to MOBILE_APP_COMPARE_STRATEGY,
        CampMetrikaGoal::class.java to CAMP_METRIKA_GOAL,
    )

}
