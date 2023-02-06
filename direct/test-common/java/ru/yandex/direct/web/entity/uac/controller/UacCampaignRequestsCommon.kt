package ru.yandex.direct.web.entity.uac.controller

import java.math.BigDecimal
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacFeedFilter
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterCondition
import ru.yandex.direct.core.entity.uac.model.UacFeedFilterOperator
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.UacAdGroupBriefRequest

object UacCampaignRequestsCommon {
    fun doSuccessCreateRequest(request: CreateCampaignRequest, mockMvc: MockMvc, login: String): String {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=$login")
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf8")
        )
            .andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
    }

    fun createCampaignRequest(
        metrikaClient: MetrikaClientStub? = null,
        uid: Long,
        href: String? = null,
        contentIds: List<String>? = null,
        texts: List<String>? = listOf("Some text for banner"),
        titles: List<String>? = listOf("Some title for banner"),
        isEcom: Boolean? = null,
        feedId: Long? = null,
        goals: List<UacGoal>? = null,
        cpa: BigDecimal? = null,
        crr: Long? = null,
        counters: List<Int>? = null,
        weekLimit: BigDecimal = UacCampaignControllerCreateUcTestBase.DEFAULT_WEEK_LIMIT,
        feedFilters: List<UacFeedFilter>? = null,
        trackingParams: String? = null,
        recommendationsManagementEnabled: Boolean? = null,
        priceRecommendationsManagementEnabled: Boolean? = null,
        minusRegions: List<Long>? = null,
        minusKeywords: List<String>? = null,
        relevanceMatch: UacRelevanceMatch? = null,
        showTitleAndBody: Boolean? = null,
    ): CreateCampaignRequest {
        val bannerHref = href ?: ("https://" + TestDomain.randomDomain())
        val sitelink = Sitelink("sitelink title", "https://" + TestDomain.randomDomain(), "sitelink descr")
        val regions = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID)
        val socdem = Socdem(
            listOf(Gender.FEMALE),
            AgePoint.AGE_45,
            AgePoint.AGE_INF,
            Socdem.IncomeGrade.LOW,
            Socdem.IncomeGrade.PREMIUM
        )
        val goal = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null)
        val actualGoals = goals ?: listOf(goal)
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val actualCounters = counters ?: listOf(counterId)
        val actualFeedFilters = feedFilters ?: listOf(
            UacFeedFilter(emptyList()),
            UacFeedFilter(
                listOf(
                    UacFeedFilterCondition("name", UacFeedFilterOperator.CONTAINS, "[\"b\"]", listOf("b")),
                    UacFeedFilterCondition("categoryId", UacFeedFilterOperator.EQUALS, "[\"1\"]", listOf("1")),
                )
            ),
            UacFeedFilter(
                listOf(
                    UacFeedFilterCondition("age", UacFeedFilterOperator.EQUALS, "[\"18\"]", listOf("18")),
                )
            ),
        )
        val filtersThatMakeSense = if (feedId != null) actualFeedFilters else null
        val campaignMinusKeywords = minusKeywords ?: listOf("minusKeyword1", "minusKeyword2")

        if (metrikaClient != null) {
            metrikaClient.addUserCounter(uid, counterId)
            metrikaClient.addCounterGoal(counterId, goal.goalId.toInt())
        }

        return CreateCampaignRequest(
            displayName = "Text campaign",
            href = bannerHref,
            texts = texts,
            titles = titles,
            regions = regions,
            minusRegions = minusRegions,
            contentIds = contentIds,
            weekLimit = weekLimit,
            limitPeriod = LimitPeriodType.MONTH,
            advType = AdvType.TEXT,
            hyperGeoId = null,
            keywords = listOf("keyword1", "keyword2"),
            minusKeywords = campaignMinusKeywords,
            socdem = socdem,
            deviceTypes = setOf(DeviceType.ALL),
            inventoryTypes = null,
            goals = actualGoals,
            goalCreateRequest = null,
            counters = actualCounters,
            permalinkId = null,
            phoneId = null,
            calltrackingPhones = emptyList(),
            sitelinks = listOf(sitelink),
            appId = null,
            trackingUrl = null,
            impressionUrl = null,
            targetId = TargetType.INSTALL,
            skadNetworkEnabled = null,
            adultContentEnabled = null,
            cpa = if (goals != null) cpa else BigDecimal.valueOf(100000000L, 6),
            crr = crr,
            timeTarget = null,
            strategy = null,
            retargetingCondition = null,
            videosAreNonSkippable = null,
            brandSurveyId = null,
            brandSurveyName = null,
            showsFrequencyLimit = null,
            strategyPlatform = null,
            adjustments = null,
            isEcom = isEcom,
            feedId = feedId,
            feedFilters = filtersThatMakeSense,
            trackingParams = trackingParams,
            cloneFromCampaignId = null,
            cpmAssets = null,
            campaignMeasurers = null,
            uacBrandsafety = null,
            uacDisabledPlaces = null,
            widgetPartnerId = null,
            source = null,
            mobileAppId = null,
            isRecommendationsManagementEnabled = recommendationsManagementEnabled,
            isPriceRecommendationsManagementEnabled = priceRecommendationsManagementEnabled,
            relevanceMatch = relevanceMatch,
            showTitleAndBody = showTitleAndBody,
            altAppStores = null,
            bizLandingId = null,
            searchLift = null,
        )
    }

    fun createAdGroupBriefRequest(
        texts: List<String> = listOf("title"),
        titles: List<String> = listOf("text"),
        contentIds: List<String>?,
        sitelinks: List<Sitelink>? = listOf(Sitelink("title", "https://" + TestDomain.randomDomain(), "descr")),
    ) = UacAdGroupBriefRequest(
        texts = texts,
        titles = titles,
        sitelinks = sitelinks,
        contentIds = contentIds,
    )
}
