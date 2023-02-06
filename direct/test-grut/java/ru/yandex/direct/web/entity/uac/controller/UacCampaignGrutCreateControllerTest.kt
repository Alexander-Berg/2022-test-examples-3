package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacShowsFrequencyLimit
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CpmAssetButton
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacBannerMeasurerSystem
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacButtonAction
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacMeasurer
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacSearchLift
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.regions.Region.BY_REGION_ID
import ru.yandex.direct.regions.Region.RUSSIA_REGION_ID
import ru.yandex.direct.regions.Region.TURKEY_REGION_ID
import ru.yandex.direct.test.utils.checkContains
import ru.yandex.direct.test.utils.checkContainsInAnyOrder
import ru.yandex.direct.test.utils.checkContainsKey
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.createUcCampaignRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.validation.model.WebValidationResult
import ru.yandex.grut.objects.proto.AgePoint.EAgePoint
import ru.yandex.grut.objects.proto.Campaign.ECampaignTypeOld
import ru.yandex.grut.objects.proto.Campaign.TBriefStrategy.ECampaignPlatform
import ru.yandex.grut.objects.proto.Campaign.TBriefStrategy.ELimitPeriod
import ru.yandex.grut.objects.proto.Campaign.TBriefStrategy.EMobileAppCampaignTargetType
import ru.yandex.grut.objects.proto.Campaign.TBriefStrategy.EStrategyName
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.ECryptaInterestType
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.EDeviceType
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.TRetargetingConditionRule.ERetargetingConditionRuleType
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.TSocdem.EIncomeGrade
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.TTimeTarget
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.TTimeTarget.THolidaySettings
import ru.yandex.grut.objects.proto.Gender.EGender
import ru.yandex.grut.objects.proto.InventoryType.EInventoryType
import java.math.BigDecimal
import java.time.LocalDate

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignGrutCreateControllerTest : BaseGrutCreateCampaignTest() {

    @Test
    fun createUacCampaignBadRequestTest() {
        val tooLongCampaignName = "Test text UC campaign".repeat(20)
        val request = createCampaignRequest(campaignName = tooLongCampaignName)

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCampaignBadRequestWithCpmStrategyTest() {
        val cpmStrategy = UacStrategy(
            UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
            UacStrategyData(
                BigDecimal.TEN,
                false, BigDecimal.TEN,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.ONE,
                null,
                null
            )
        )

        val request = createCampaignRequest(strategy = cpmStrategy)

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCampaignNoRightsTest() {
        val request = createCampaignRequest()
        val anotherUser = steps.userSteps().createDefaultUser()
        testAuthHelper.setSubjectUser(userInfo.uid)
        testAuthHelper.setOperator(anotherUser.uid)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + anotherUser.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        assertThat(result).isEqualTo("No rights")
    }

    @Test
    fun createUacCampaignTest() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.DISABLE_VIDEO_CREATIVE, true)

        val imageContentId2 = grutSteps.createDefaultImageAsset(clientId)
        val imageContentId = grutSteps.createDefaultImageAsset(clientId)
        val imageContentId3 = grutSteps.createDefaultImageAsset(clientId)
        val request = createUacCampaignRequest(
            listOf(imageContentId, imageContentId2, imageContentId3),
            adjustments = listOf(
                UacAdjustmentRequest(region = 225, age = null, gender = null, percent = 50, retargetingConditionId = null),
                UacAdjustmentRequest(region = null, age = AgePoint.AGE_25, gender = Gender.FEMALE, percent = -33, retargetingConditionId = null),
            ),
            showTitleAndBody = true,
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()

        grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())
        val campaignInGrut = grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())!!.apply {
            meta.campaignType.checkEquals(ECampaignTypeOld.CTO_MOBILE_APP)
            spec.campaignBrief.apply {
                targetHref.apply {
                    href.checkEquals(request.href)
                    trackingUrl.checkEquals("https://redirect.appmetrica.yandex.com/serve/1179849830915165578?c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&position_type={PTYPE}&campaign_id=54494649&google_aid={google_aid}&click_id={logid}")
                }
                regionsList.checkContains(BY_REGION_ID)
                appId.checkEquals(request.appId)
                strategy.apply {
                    cpa.checkEquals(500000000)
                    weekLimit.checkEquals(2300000000)
                    limitPeriod.checkEquals(ELimitPeriod.LP_WEEK)
                    mobileAppCampaignTargetType.checkEquals(EMobileAppCampaignTargetType.MACTT_INSTALL)
                    platform.checkEquals(ECampaignPlatform.CP_NOT_SPECIFIED)
                }
                adjustmentsList.checkSize(2)
                adjustmentsList[0].age.checkEquals(EAgePoint.AP_NOT_SPECIFIED)
                adjustmentsList[0].gender.checkEquals(EGender.G_UNKNOWN)
                adjustmentsList[0].region.checkEquals(225L)
                adjustmentsList[0].percent.checkEquals(50)

                adjustmentsList[1].age.checkEquals(EAgePoint.AP_AGE_25)
                adjustmentsList[1].gender.checkEquals(EGender.G_FEMALE)
                adjustmentsList[1].region.checkEquals(0L)
                adjustmentsList[1].percent.checkEquals(-33)
                showTitleAndBody.checkEquals(true)
            }

            spec.campaignData.name.checkEquals(request.displayName)
            spec.briefAssetLinks.linksCount.checkEquals(9)
            spec.briefAssetLinksStatuses.linkStatusesCount.checkEquals(9)
        }

        val assetIds = campaignInGrut.spec.briefAssetLinks.linksList.map { it.assetId }.toSet()
        val assets = grutApiService.assetGrutApi.getAssets(assetIds)
        assets.checkSize(9)

        val titleAssetsById = assets.filter { it.spec.hasTitle() }.associateBy { it.meta.id }
        val textAssetsById = assets.filter { it.spec.hasText() }.associateBy { it.meta.id }
        val imageAssetsById = assets.filter { it.spec.hasImage() }.associateBy { it.meta.id }

        campaignInGrut.spec.briefAssetLinks.linksList
            .mapNotNull { titleAssetsById[it.assetId] }
            .map { it.spec.title }
            .checkContains("title 1", "title 2", "title 3")
        campaignInGrut.spec.briefAssetLinks.linksList
            .mapNotNull { textAssetsById[it.assetId] }
            .map { it.spec.text }
            .checkContains("text 1", "text 2", "text 3")
        campaignInGrut.spec.briefAssetLinks.linksList
            .mapNotNull { imageAssetsById[it.assetId] }
            .map { it.meta.id.toIdString() }
            .checkContains(imageContentId, imageContentId2, imageContentId3)
    }

    @Test
    fun test_CreateUacCampaign_WithDisabledPlaces() {
        val clientId = clientInfo.clientId!!

        val imageContentId = grutSteps.createDefaultImageAsset(clientId)
        val request = createUacCampaignRequest(
            listOf(imageContentId),
            uacDisabledPlaces = UacDisabledPlaces(listOf("http://profi.ru/"), null, null, null)
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()
        val mysqlCampaign = campaignTypedRepository.getTyped(userInfo.shard, listOf(campaignId.toIdLong()))[0]
        assertThat((mysqlCampaign as MobileContentCampaign).disabledDomains).isEqualTo(listOf("profi.ru"))
    }

    @Test
    fun createUcCampaignTest() {
        val request = createUcCampaignRequest()

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()

        val expectedTimeTarget =
            TTimeTarget.newBuilder().apply {
                idTimeZone = 2
                useWorkingWeekends = true
                enabledHolidaysMode = true
                holidaysSettings = THolidaySettings.newBuilder().apply {
                    startHour = 12
                    endHour = 15
                    show = true
                }.build()
                addAllTimeBoard(
                    (1..5).map {
                        TTimeTarget.TTimeBoardItem.newBuilder()
                            .addAllItems(
                                listOf(
                                    0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 100, 100,
                                    100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0
                                )
                            ).build()
                    } + TTimeTarget.TTimeBoardItem.newBuilder().addAllItems(
                        listOf(
                            0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 100,
                            100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0
                        )
                    ).build()
                        + TTimeTarget.TTimeBoardItem.newBuilder().addAllItems(
                        listOf(
                            0, 0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100,
                            100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0
                        )
                    ).build()

                )
            }.build()
        grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())
        val campaignInGrut = grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())!!.apply {
            meta.campaignType.checkEquals(ECampaignTypeOld.CTO_TEXT)
            spec.campaignBrief.apply {
                targetHref.href.checkEquals("http://fukudo-shiba.ru")
                regionsList.checkContains(RUSSIA_REGION_ID)
                strategy.weekLimit.checkEquals(2300000000)
                strategy.limitPeriod.checkEquals(ELimitPeriod.LP_MONTH)
                keywordsList.checkContains(
                    "сиба ину купить щенка", "сиба ину купить щенка москва"
                )
                minusKeywordsList.checkContains(
                    "кот", "котенок"
                )
                socdem.gendersList.checkEquals(listOf(EGender.G_MALE))
                socdem.ageLower.checkEquals(EAgePoint.AP_AGE_18)
                socdem.ageUpper.checkEquals(EAgePoint.AP_AGE_55)
                deviceTypeList.checkContainsInAnyOrder(EDeviceType.DT_PHONE, EDeviceType.DT_TABLET)
                timeTarget.checkEquals(expectedTimeTarget)
            }

            spec.campaignData.name.checkEquals(request.displayName)
            spec.briefAssetLinks.linksCount.checkEquals(3)
            spec.briefAssetLinksStatuses.linkStatusesCount.checkEquals(3)
        }

        val assetIds = campaignInGrut.spec.briefAssetLinks.linksList.map { it.assetId }.toSet()
        val assets = grutApiService.assetGrutApi.getAssets(assetIds)

        assets.checkSize(3)
        val titleAsset = assets.find { it.spec.hasTitle() }
        val textAsset = assets.find { it.spec.hasText() }
        val sitelinkAsset = assets.find { it.spec.hasSitelink() }

        titleAsset!!.apply {
            spec.title.checkEquals("купить щенка сиба-ину")
        }
        textAsset!!.apply {
            spec.text.checkEquals("купить щенка")
        }
        sitelinkAsset!!.apply {
            spec.sitelink.apply {
                title.checkEquals("sitelink title")
                description.checkEquals("sitelink description")
                href.checkEquals("https://sitelink-href.ru")
            }
        }
    }

    @Test
    fun createUacCpmCampaignInvalidNonSkippableTest() {
        //обычное видео и флаг непропуска валидация сработает
        val clientId = clientInfo.clientId!!
        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            LocalDate.of(2021, 4, 15),
            LocalDate.of(2021, 10, 15),
            UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.ALL,
                        interestType = CryptaInterestType.all,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(id = 1, time = 2),
                        )
                    ),
                ),
                name = "condition name",
            ),
            UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15),
            cpmAssets,
            nonSkippable = true,
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun createUacCpmCampaignTest() {
        val clientId = clientInfo.clientId!!

        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val strategyStartDate = LocalDate.of(2021, 4, 15)
        val strategyStartDateTimestamp = 1618444800
        val strategyFinishDate = LocalDate.of(2021, 10, 15)
        val strategyFinishDateTimestamp = 1634256000
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    interestType = CryptaInterestType.all,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(id = 1, time = 2),
                    )
                ),
            ),
            name = "condition name",
        )
        val showsFrequencyLimit = UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15)
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            strategyStartDate,
            strategyFinishDate,
            retargetingCondition,
            showsFrequencyLimit,
            cpmAssets,
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { result -> System.out.println(result.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()

        val campaignInGrut = grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())!!.apply {
            meta.campaignType.checkEquals(ECampaignTypeOld.CTO_CPM_BANNER)
            spec.apply {
                campaignData.name.checkEquals(request.displayName)
                campaignBrief.apply {
                    targetHref.href.checkEquals(request.href)
                    regionsList.checkContains(TURKEY_REGION_ID)
                    videosAreNonSkippable.checkEquals(false)
                    socdem.apply {
                        gendersList.checkContains(EGender.G_MALE)
                        ageLower.checkEquals(EAgePoint.AP_AGE_25)
                        ageUpper.checkEquals(EAgePoint.AP_AGE_55)
                        incomeLower.checkEquals(EIncomeGrade.IG_LOW)
                        incomeUpper.checkEquals(EIncomeGrade.IG_MIDDLE)
                    }
                    strategy.apply {
                        limitPeriod.checkEquals(ELimitPeriod.LP_MONTH)
                        strategyName.checkEquals(EStrategyName.SN_AUTOBUDGET_MAX_IMPRESSIONS)
                        strategyData.apply {
                            budget.checkEquals(0)
                            autoProlongation.checkEquals(true)
                            avgCpm.checkEquals(300000000)
                            startTime.checkEquals(strategyStartDateTimestamp)
                            finishTime.checkEquals(strategyFinishDateTimestamp)
                            sum.checkEquals(2100000000)
                        }
                    }
                    cpmData.apply {
                        this.retargetingCondition.apply {
                            conditionRulesList.checkSize(1)
                            conditionRulesList[0].apply {
                                type.checkEquals(ERetargetingConditionRuleType.RCRT_ALL)
                                interestType.checkEquals(ECryptaInterestType.CIT_ALL)
                                goalsList.checkSize(1)
                                goalsList[0].id.checkEquals(1)
                                goalsList[0].time.checkEquals(2)
                            }
                            name.checkEquals(retargetingCondition.name)
                        }
                        this.showsFrequencyLimit.apply {
                            impressionRateCount.checkEquals(showsFrequencyLimit.impressionRateCount)
                            impressionRateIntervalDays.checkEquals(showsFrequencyLimit.impressionRateIntervalDays)
                        }
                        this.cpmAssetsMap.apply {
                            checkContainsKey(videoContentId)
                            get(videoContentId)!!.apply {
                                val asset = cpmAssets[videoContentId]!!
                                title.checkEquals(asset.title)
                                titleExtension.checkEquals(asset.titleExtension)
                                body.checkEquals(asset.body)
                                hasLogoImageHash().checkEquals(false)
                                // logoImageHash.checkEquals(asset.logoImageHash)
                                pixelsList.checkEquals(asset.pixels)
                                button.apply {
                                    action.checkEquals(asset.button!!.action.name)
                                    customText.checkEquals(asset.button!!.customText)
                                    href.checkEquals(asset.button!!.href)
                                }
                            }
                        }
                    }
                    inventoryTypeList.apply {
                        assertThat(this).isEqualTo(listOf(EInventoryType.IT_INSTREAM))
                    }
                    deviceTypeList.apply {
                        assertThat(this).size().isEqualTo(2)
                        assertThat(this).containsAll(listOf(EDeviceType.DT_DESKTOP, EDeviceType.DT_PHONE_IOS))
                    }
                }
                briefAssetLinks.linksList.checkSize(1)
                briefAssetLinksStatuses.linkStatusesList.checkSize(1)
            }
        }

        campaignInGrut.spec.briefAssetLinks.linksList.map { it.assetId.toIdString() }.checkContains(videoContentId)
    }


    @Test
    fun createUacCpmCampaignSearchLiftWithoutFeature() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.SEARCH_LIFT, false)

        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            LocalDate.of(2021, 4, 15),
            LocalDate.of(2021, 10, 15),
            UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.ALL,
                        interestType = CryptaInterestType.all,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(id = 1, time = 2),
                        )
                    ),
                ),
                name = "condition name",
            ),
            UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15),
            cpmAssets,
            searchLift = UacSearchLift(listOf("brand1", "brand2"), listOf("Object1", "Object2"))
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        assertThat(validationResult.errors).isNotEmpty
        assertThat(validationResult.errors[0].path)
            .isEqualTo(PathHelper.path(PathHelper.field(CreateCampaignRequest::searchLift)).toString())
        assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.MUST_BE_NULL.code)
    }

    @Test
    fun createUacCpmCampaignSearchLiftOverflownLists() {

        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.SEARCH_LIFT, true)
        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            LocalDate.of(2021, 4, 15),
            LocalDate.of(2021, 10, 15),
            UacRetargetingCondition(
                conditionRules = listOf(
                    UacRetargetingConditionRule(
                        type = UacRetargetingConditionRule.RuleType.ALL,
                        interestType = CryptaInterestType.all,
                        goals = listOf(
                            UacRetargetingConditionRuleGoal(id = 1, time = 2),
                        )
                    ),
                ),
                name = "condition name",
            ),
            UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15),
            cpmAssets,
            searchLift = UacSearchLift(
                listOf("brand1", "brand2","brand1", "brand2"),
                listOf("Object1", "Object2", "Object1", "Object2", "Object1", "Object2"))
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        assertThat(validationResult.errors).isNotEmpty
        assertThat(validationResult.errors.map { it.path })
            .containsExactlyElementsOf(
                listOf(
                    PathHelper.path(PathHelper.field(CreateCampaignRequest::searchLift), PathHelper.field(UacSearchLift::brands)).toString(),
                    PathHelper.path(PathHelper.field(CreateCampaignRequest::searchLift), PathHelper.field(UacSearchLift::searchObjects)).toString()))
        assertThat(validationResult.errors.map { it.code }).containsOnly(CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX.code)
    }

    @Test
    fun createUacCpmCampaignSearchLift() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.SEARCH_LIFT, true)

        val brands = listOf("brand1", "brand2")
        val searchObjects = listOf("Object1", "Object2")
        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val strategyStartDate = LocalDate.of(2021, 4, 15)
        val strategyFinishDate = LocalDate.of(2021, 10, 15)
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    interestType = CryptaInterestType.all,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(id = 1, time = 2),
                    )
                ),
            ),
            name = "condition name",
        )
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            strategyStartDate,
            strategyFinishDate,
            retargetingCondition,
            UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15),
            cpmAssets,
            searchLift = UacSearchLift(brands, searchObjects),
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()

        val campaignInGrut = grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())!!
        assertThat(campaignInGrut.spec.campaignBrief.cpmData.searchLift.brandsList).containsExactlyElementsOf(listOf("brand1", "brand2"))
        assertThat(campaignInGrut.spec.campaignBrief.cpmData.searchLift.searchObjectsList).containsExactlyElementsOf(listOf("Object1", "Object2"))
    }

    @Test
    fun createUacCpmCampaignSearchLiftOneNull() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.SEARCH_LIFT, true)

        val brands = listOf("brand1", "brand2")
        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val strategyStartDate = LocalDate.of(2021, 4, 15)
        val strategyFinishDate = LocalDate.of(2021, 10, 15)
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    interestType = CryptaInterestType.all,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(id = 1, time = 2),
                    )
                ),
            ),
            name = "condition name",
        )
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            strategyStartDate,
            strategyFinishDate,
            retargetingCondition,
            UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15),
            cpmAssets,
            searchLift = UacSearchLift(brands),
        )

        val resultRaw = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val campaignId = JsonUtils.fromJson(resultRaw)["result"]["id"].asText()

        val campaignInGrut = grutApiSerive.briefGrutApi.getBrief(campaignId.toIdLong())!!
        assertThat(campaignInGrut.spec.campaignBrief.cpmData.searchLift.searchObjectsList).isEmpty()
        assertThat(campaignInGrut.spec.campaignBrief.cpmData.searchLift.brandsList).containsExactlyElementsOf(listOf("brand1", "brand2"))
    }

    @Test
    fun createTwoUcCampaignsTest() {
        val request = createUcCampaignRequest()

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isCreated)
    }


    @Test
    fun test_CreateUacCampaign_WithShowTitleAndBody_WithoutFlag() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.DISABLE_VIDEO_CREATIVE, false)

        val request = createUacCampaignRequest(
            listOf(grutSteps.createDefaultImageAsset(clientId)),
            showTitleAndBody = true,
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }


    @Test
    fun createUacCpmCampaignWithDisabledFeature() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(clientId, FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true)

        val videoContentId = grutSteps.createDefaultVideoAsset(clientId)
        val strategyStartDate = LocalDate.of(2021, 4, 15)
        val strategyFinishDate = LocalDate.of(2021, 10, 15)
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    interestType = CryptaInterestType.all,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(id = 1, time = 2),
                    )
                ),
            ),
            name = "condition name",
        )
        val cpmAssets = mapOf(
            videoContentId to UacCpmAsset(
                title = "title",
                titleExtension = "title extension",
                body = "body",
                button = CpmAssetButton(UacButtonAction.BUY, "", "http://yandex.ru"),
                logoImageHash = null,
                measurers = listOf(UacMeasurer(UacBannerMeasurerSystem.WEBORAMA, "{\"account\":1,\"tte\":1,\"aap\":1}")),
                pixels = listOf("https://mc.yandex.ru/pixel/7429537502459335683?rnd=%25aw_random%25"),
                bannerHref = "http://yandex.ru/href"
            ),
        )

        val request = createCpmBannerCampaignRequest(
            videoContentId,
            strategyStartDate,
            strategyFinishDate,
            retargetingCondition,
            UacShowsFrequencyLimit(impressionRateCount = 30, impressionRateIntervalDays = 15),
            cpmAssets,
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/campaigns?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
