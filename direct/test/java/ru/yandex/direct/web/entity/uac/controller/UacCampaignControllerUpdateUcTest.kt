package ru.yandex.direct.web.entity.uac.controller

import java.math.BigDecimal
import java.util.function.Consumer
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefectIds
import ru.yandex.direct.core.entity.organization.model.PermalinkAssignType
import ru.yandex.direct.core.entity.organizations.validation.OrganizationDefectIds
import ru.yandex.direct.core.entity.region.validation.RegionIdDefectIds
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.validation.ContentDefectIds
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.feature.FeatureName.RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC
import ru.yandex.direct.regions.Region.UZBEKISTAN_REGION_ID
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.tracing.util.TraceUtil
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerUpdateUcTest : UacCampaignControllerUpdateUcTestBase() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    private lateinit var uacCampaignInfo: UacCampaignSteps.UacCampaignInfo

    @Before
    fun ydbBefore() {
        uacCampaignInfo = uacCampaignSteps.createTextCampaign(clientInfo)
        uacCampaignId = uacCampaignInfo.uacCampaign.id

        createContents()
    }

    override fun saveContents(contents: List<UacYdbContent>) {
        uacYdbContentRepository.saveContents(contents)
    }

    override fun getEcomCampaign(): UacYdbCampaign {
        return uacYdbCampaignRepository.getCampaign(uacEcomCampaignId)!!
    }

    /**
     * Проверяем обновление UC кампании при запросе с одним image и video
     */
    @Test
    fun `update uc campaign with contents`() {
        val request = updateCampaignRequest(
            contentIds = listOf(imageContentFirst.id, videoContentFirst.id)
        )

        doSuccessRequest(
            request,
            expectImageContentsCount = 1,
            expectVideoContentsCount = 1,
        )
    }

    @Test
    fun `update campaign permalink become unavailable no USE_BANNERS_ADD_UAC_TGO_UPDATE_PREVALIDATION property`() {
        //создаём кампанию с пермалинком
        val permalinkId = TraceUtil.randomId()
        val request = updateCampaignRequest(permalinkId = permalinkId)
        (organizationsClientStub as OrganizationsClientStub).addUidsByPermalinkId(permalinkId, listOf(clientInfo.uid))
        doSuccessRequest(request)

        //дописываем к кампании группу и баннер в mysql и привязываем к нему пермалинк
        val bannerId = steps.bannerSteps().createActiveTextBanner(
            steps.adGroupSteps().createActiveTextAdGroup(uacCampaignInfo.campaign)
        ).bannerId
        testOrganizationRepository.addAutoPermalink(uacCampaignInfo.campaign.shard, bannerId, permalinkId)
        testOrganizationRepository.changePermalinkAssignType(uacCampaignInfo.campaign.shard, bannerId,
            permalinkId, PermalinkAssignType.MANUAL)

        //удаляем оргу и делаем запрос на обновление
        (organizationsClientStub as OrganizationsClientStub).removePermalinks(permalinkId)
        doSuccessRequest(request)
    }

    @Test
    fun `update campaign permalink become unavailable with USE_BANNERS_ADD_UAC_TGO_UPDATE_PREVALIDATION property`() {
        //создаём кампанию с пермалинком
        val permalinkId = TraceUtil.randomId()
        val request = updateCampaignRequest(permalinkId = permalinkId)
        (organizationsClientStub as OrganizationsClientStub).addUidsByPermalinkId(permalinkId, listOf(clientInfo.uid))
        doSuccessRequest(request)


        //дописываем к кампании группу и баннер в mysql и привязываем к нему пермалинк
        val bannerId = steps.bannerSteps().createActiveTextBanner(
            steps.adGroupSteps().createActiveTextAdGroup(uacCampaignInfo.campaign)
        ).bannerId
        testOrganizationRepository.addAutoPermalink(uacCampaignInfo.campaign.shard, bannerId, permalinkId)
        testOrganizationRepository.changePermalinkAssignType(uacCampaignInfo.campaign.shard, bannerId,
            permalinkId, PermalinkAssignType.MANUAL)

        //удаляем оргу и делаем запрос на обновление
        ppcPropertiesSupport.set(PpcPropertyNames.USE_BANNERS_ADD_UAC_TGO_UPDATE_PREVALIDATION, "1")
        (organizationsClientStub as OrganizationsClientStub).removePermalinks(permalinkId)
        checkBadRequest(request, path=path(field(PatchCampaignRequest::permalinkId.name)),
            defectId = OrganizationDefectIds.Gen.ORGANIZATION_NOT_FOUND)
        ppcPropertiesSupport.remove(PpcPropertyNames.USE_BANNERS_ADD_UAC_TGO_UPDATE_PREVALIDATION)
    }

    /**
     * Проверка что при добавлении организации баннеры кампании отправляются на перемодерацию
     */
    @Test
    fun `send banners to moderation when add organization`() {
        val permalinkId = TraceUtil.randomId()
        (organizationsClientStub as OrganizationsClientStub).addUidsByPermalinkId(permalinkId, listOf(clientInfo.uid))

        val bannerId = steps.bannerSteps().createActiveTextBanner(
            steps.adGroupSteps().createActiveTextAdGroup(uacCampaignInfo.campaign)
        ).bannerId

        val firstRequestToUpdate = updateCampaignRequest(permalinkId = null)
        doSuccessRequest(
            request = firstRequestToUpdate,
            uacCampaignId = uacCampaignId
        )

        bannerModerationRepository.updateStatusModerate(uacCampaignInfo.campaign.shard, listOf(bannerId), BannerStatusModerate.YES)

        val secondRequestToUpdate = updateCampaignRequest(permalinkId = permalinkId)
        doSuccessRequest(
            request = secondRequestToUpdate,
            uacCampaignId = uacCampaignId
        )

        val actualBannersStatusModerate = bannerTypedRepository
            .getBannersByCampaignIds(uacCampaignInfo.campaign.shard, listOf(uacCampaignInfo.campaign.campaignId))
            .map { it as TextBanner }
            .map { it.statusModerate }
        SoftAssertions.assertSoftly {
            it.assertThat(actualBannersStatusModerate)
                .`as`("Статус модерации баннеров изменился на ready")
                .hasSize(1)
                .containsOnly(BannerStatusModerate.READY)
        }
    }

    /**
     * Проверка что при отрыве организации от кампании баннеры не отправляются на перемодерацию
     */
    @Test
    fun `do not send banners to moderation when delete organization`() {
        val permalinkId = TraceUtil.randomId()
        (organizationsClientStub as OrganizationsClientStub).addUidsByPermalinkId(permalinkId, listOf(clientInfo.uid))

        val bannerId = steps.bannerSteps().createActiveTextBanner(
            steps.adGroupSteps().createActiveTextAdGroup(uacCampaignInfo.campaign)
        ).bannerId

        val firstRequestToUpdate = updateCampaignRequest(permalinkId = permalinkId)
        doSuccessRequest(
            request = firstRequestToUpdate,
            uacCampaignId = uacCampaignId
        )

        bannerModerationRepository.updateStatusModerate(uacCampaignInfo.campaign.shard, listOf(bannerId), BannerStatusModerate.YES)

        val secondRequestToUpdate = updateCampaignRequest(permalinkId = null)
        doSuccessRequest(
            request = secondRequestToUpdate,
            uacCampaignId = uacCampaignId
        )

        val actualBannersStatusModerate = bannerTypedRepository
            .getBannersByCampaignIds(uacCampaignInfo.campaign.shard, listOf(uacCampaignInfo.campaign.campaignId))
            .map { it as TextBanner }
            .map { it.statusModerate }
        SoftAssertions.assertSoftly {
            it.assertThat(actualBannersStatusModerate)
                .`as`("Статус модерации баннеров не изменился")
                .hasSize(1)
                .containsOnly(BannerStatusModerate.YES)
        }
    }

    /**
     * Проверяем обновление UC кампании при запросе без контента
     */
    @Test
    fun `update uc campaign without contents`() {
        val request = updateCampaignRequest()

        doSuccessRequest(request)
    }

    /**
     * Проверяем что UC кампания не обновляется при запросе с двумя video ассетами
     */
    @Test
    fun `update uc campaign with two video contents`() {
        val request = updateCampaignRequest(
            contentIds = listOf(videoContentFirst.id, videoContentSecond.id),
        )

        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::contentIds.name)),
            defectId = ContentDefectIds.Gen.SIZE_OF_VIDEO_CONTENTS_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    /**
     * Проверяем что UC кампания не обновляется при запросе с двумя image ассетами
     */
    @Test
    fun `update uc campaign with two image contents`() {
        val request = updateCampaignRequest(
            contentIds = listOf(imageContentFirst.id, imageContentSecond.id),
        )

        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::contentIds.name)),
            defectId = ContentDefectIds.Gen.SIZE_OF_IMAGE_CONTENTS_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    /**
     * Проверяем что UC кампания не обновляется при запросе с двумя title ассетами
     */
    @Test
    fun `update uc campaign with two title contents`() {
        val request = updateCampaignRequest(
            titles = listOf("title1", "title2"),
        )

        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::titles.name)),
            defectId = CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    /**
     * Проверяем что UC кампания не обновляется при запросе с двумя text ассетами
     */
    @Test
    fun `update uc campaign with two text contents`() {
        val request = updateCampaignRequest(
            texts = listOf("text1", "text2"),
        )

        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::texts.name)),
            defectId = CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX,
        )
    }

    /**
     * Проверяем что UC кампания не обновляется при запросе с регионами, не содержащими минус регион
     */
    @Test
    fun `update uc campaign with invalid minus region`() {
        val request = updateCampaignRequest(
            minusRegions = listOf(UZBEKISTAN_REGION_ID),
        )

        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::regions.name)),
            defectId = RegionIdDefectIds.Regions.MINUS_REGIONS_WITHOUT_PLUS_REGIONS,
        )
    }

    /**
     * Проверяем что UC кампания обновляется при запросе с несколькими text/title/video/image ассетами
     * и c включенной фичой UC_MULTIPLE_ADS_ENABLED
     */
    @Test
    fun `update uc campaign with multiple contents and with feature`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_MULTIPLE_ADS_ENABLED, true)

        val request = updateCampaignRequest(
            titles = listOf("title1", "title2"),
            texts = listOf("text1", "text2"),
            contentIds = listOf(videoContentFirst.id, videoContentSecond.id, imageContentFirst.id, imageContentSecond.id),
        )

        doSuccessRequest(
            request,
            expectTitleContentsCount = 2,
            expectTextContentsCount = 2,
            expectImageContentsCount = 2,
            expectVideoContentsCount = 2,
        )
    }

    /**
     * Проверяем, что UC кампания обновляется при изменении списка минус слов
     */
    @Test
    fun `update uc campaign with minus keywords`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_ENABLE_MINUS_KEYWORDS_TGO, true)
        val minusKeywords = listOf("minus word 1", "minus word 2")
        val request = updateCampaignRequest(
            minusKeywords = minusKeywords
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign
        assertThat(textCampaign.minusKeywords).containsExactlyInAnyOrderElementsOf(minusKeywords)
    }

    /**
     * Проверяем, что UC кампания не обновляется при изменении списка минус-слов без фичи
     */
    @Test
    fun `update uc campaign with minus keywords without the feature`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_ENABLE_OPTIONAL_KEYWORDS, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_ENABLE_MINUS_KEYWORDS_TGO, false)
        val minusKeywords = listOf("minus word 1", "minus word 2")
        val request = updateCampaignRequest(
            minusKeywords = minusKeywords
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign
        assertThat(textCampaign.minusKeywords).isEmpty()
    }

    @Test
    fun updateUcGoalsWithoutConversionValue() {
        val defaultConversionValue = Currencies.getCurrency(CurrencyCode.RUB).ucDefaultConversionValue
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())

        val request = updateCampaignRequest(
            goals = listOf(goal1, goal2)
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign
        softly {
            assertThat(textCampaign.meaningfulGoals).hasSize(2)
            assertThat(textCampaign.meaningfulGoals).allSatisfy(Consumer {
                assertThat(it.conversionValue).isEqualTo(defaultConversionValue)
            })
        }
    }

    @Test
    fun updateUcGoalsWithConversionValue() {
        val conversionValue = 1234.toBigDecimal()
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = conversionValue)
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = conversionValue)

        val request = updateCampaignRequest(
            goals = listOf(goal1, goal2)
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign
        softly {
            assertThat(textCampaign.meaningfulGoals).hasSize(2)
            assertThat(textCampaign.meaningfulGoals).allSatisfy(Consumer {
                assertThat(it.conversionValue).isEqualTo(conversionValue)
            })
        }
    }

    @Test
    fun updateUcGoalsWithAndWithoutConversionValue() {
        val conversionValue = 1234.toBigDecimal()
        val defaultConversionValue = Currencies.getCurrency(CurrencyCode.RUB).ucDefaultConversionValue
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = conversionValue)

        val request = updateCampaignRequest(
            goals = listOf(goal1, goal2)
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign
        assertThat(textCampaign.meaningfulGoals).satisfiesExactlyInAnyOrder(Consumer {
            softly {
                assertThat(it.goalId).isEqualTo(goal1.goalId)
                assertThat(it.conversionValue).isEqualTo(defaultConversionValue)
            }
        }, {
            softly {
                assertThat(it.goalId).isEqualTo(goal2.goalId)
                assertThat(it.conversionValue).isEqualTo(conversionValue)
            }
        })
    }

    @Test
    fun updateUcGoalsWithAndWithoutCpa() {
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong())
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), cpa = BigDecimal.valueOf(200))

        val request = updateCampaignRequest(
            goals = listOf(goal1, goal2),
            crr = 20,
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign
        assertThat(textCampaign.meaningfulGoals).satisfiesExactlyInAnyOrder(Consumer {
            softly {
                assertThat(it.goalId).isEqualTo(goal1.goalId)
                assertThat(it.isMetrikaSourceOfValue).isTrue()
            }
        }, {
            softly {
                assertThat(it.goalId).isEqualTo(goal2.goalId)
                assertThat(it.conversionValue).isEqualTo(BigDecimal.valueOf(1000))
            }
        })
    }

    @Test
    fun updateUcGoalsWithConversionValueAndWithCpa() {
        val goal1 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), conversionValue = BigDecimal.valueOf(2000))
        val goal2 = UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), cpa = BigDecimal.valueOf(200))

        val request = updateCampaignRequest(
            goals = listOf(goal1, goal2)
        )

        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::goals), index(0), field(UacGoal::cpa)),
            defectId = DefectIds.CANNOT_BE_NULL
        )
    }

    fun casesForAutotargetingCategories(): List<List<Any>> {
        val activeCategories = UacRelevanceMatchCategory.values()
            .map { listOf(true, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        val inActiveCategories = UacRelevanceMatchCategory.values()
            .map { listOf(false, listOf(it)) }
        return activeCategories
            .plus(inActiveCategories)
            .plus(listOf(listOf(true, UacRelevanceMatchCategory.values().sorted())))
            .plus(listOf(listOf(false, UacRelevanceMatchCategory.values().sorted())))
    }

    /**
     * Проверяем, что UC кампания обновляется с автотаргетингом
     */
    @Test
    @TestCaseName("Active {0} categories {1}")
    @Parameters(method = "casesForAutotargetingCategories")
    fun `update uc campaign with autotargeting`(
        active: Boolean,
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = active,
            categories = categories.toSet(),
        )

        val request = updateCampaignRequest(relevanceMatch = relevanceMatch)
        doSuccessRequest(request)
        val ucCampaign = uacYdbCampaignRepository.getCampaign(uacCampaignId)!!

        Assertions.assertThat(ucCampaign.relevanceMatch)
            .`as`("автотаргетинг")
            .isEqualTo(relevanceMatch)
    }

    /**
     * Проверяем, что UC кампания не обновляется с автотаргетингом без фичи
     */
    @Test
    fun `update uc campaign with autotargeting and without feature`() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, false)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = setOf(UacRelevanceMatchCategory.EXACT_MARK),
        )

        val request = updateCampaignRequest(relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.MUST_BE_NULL
        )
    }

    fun casesForAutotargetingCategoriesWithoutExtra(): List<List<Any>> {
        val data = UacRelevanceMatchCategory.values()
            .filter { it != UacRelevanceMatchCategory.EXACT_MARK }
            .map { listOf(listOf(it)) }
        return data
            .plus(listOf(
                listOf(UacRelevanceMatchCategory.values()
                    .filter { it != UacRelevanceMatchCategory.EXACT_MARK }
                    .sorted()
                )
            ))
    }

    /**
     * Проверяем, что UC кампания не обновляется с автотаргетингом и без EXACT_MARK категории
     */
    @Test
    @TestCaseName("Categories {0}")
    @Parameters(method = "casesForAutotargetingCategoriesWithoutExtra")
    fun `update uc campaign with autotargeting and without exact_mark category`(
        categories: List<UacRelevanceMatchCategory>,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, RELEVANCE_MATCH_CATEGORIES_ALLOWED_IN_UC, true)

        val relevanceMatch = UacRelevanceMatch(
            active = true,
            categories = categories.toSet(),
        )

        val request = updateCampaignRequest(relevanceMatch = relevanceMatch)
        checkBadRequest(
            request,
            path = path(field(PatchCampaignRequest::relevanceMatch)),
            defectId = DefectIds.INVALID_VALUE
        )
    }

    override fun doSuccessRequest(
        request: PatchCampaignRequest,
        uacCampaignId: String,
        expectTitleContentsCount: Int,
        expectTextContentsCount: Int,
        expectImageContentsCount: Int,
        expectVideoContentsCount: Int,
    ): Long {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .patch("/uac/campaign/${uacCampaignId}?ulogin=" + clientInfo.login)
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo { System.err.println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()
        val directCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignId)

        val campaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
        val titleContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.TITLE }
            .filter { it.removedAt == null }
        val textContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.TEXT }
            .filter { it.removedAt == null }
        val imageContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE }
            .filter { it.removedAt == null }
        val videoContents = campaignContents
            .filter { it.type == ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO }
            .filter { it.removedAt == null }

        SoftAssertions.assertSoftly {
            it.assertThat(titleContents.size)
                .`as`("Количество title ассетов")
                .isEqualTo(expectTitleContentsCount)
            it.assertThat(textContents.size)
                .`as`("Количество text ассетов")
                .isEqualTo(expectTextContentsCount)
            it.assertThat(imageContents.size)
                .`as`("Количество image ассетов")
                .isEqualTo(expectImageContentsCount)
            it.assertThat(videoContents.size)
                .`as`("Количество video ассетов")
                .isEqualTo(expectVideoContentsCount)
        }
        return directCampaign!!.directCampaignId
    }

    @Test
    fun updateTooLongMinusKeywordTest() {
        val newMinusKeywords : List<String> = List(1) { RandomStringUtils.randomAlphabetic(36) }

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_ENABLE_MINUS_KEYWORDS_TGO, true)

        val request = updateCampaignRequest(
            minusKeywords = newMinusKeywords
        )

        checkBadRequest(
            request,
            path = path(field("minusKeywords[0]")),
            defectId = MinusPhraseDefectIds.IndividualKeywordLength.MAX_LENGTH_MINUS_WORD
        )
    }

    @Test
    fun updateTooManyMinusKeywordsTest() {
        val newMinusKeywords : List<String> = List(2000) { RandomStringUtils.randomAlphabetic(20) }

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_ENABLE_MINUS_KEYWORDS_TGO, true)

        val request = updateCampaignRequest(
            minusKeywords = newMinusKeywords
        )

        checkBadRequest(
            request,
            path = path(field("minusKeywords")),
            defectId = MinusPhraseDefectIds.StringLength.MAX_LENGTH_MINUS_KEYWORDS
        )
    }

    @Test
    fun updateManyMinusKeywordsTest() {
        val newMinusKeywords : List<String> = List(1000) { RandomStringUtils.randomAlphabetic(20) }

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_ENABLE_MINUS_KEYWORDS_TGO, true)

        val request = updateCampaignRequest(
            minusKeywords = newMinusKeywords
        )

        val campaignId = doSuccessRequest(request)
        val textCampaign = campaignTypedRepository.getTyped(uacCampaignInfo.campaign.shard, listOf(campaignId))[0] as TextCampaign

        assertThat(textCampaign.minusKeywords).containsExactlyInAnyOrderElementsOf(newMinusKeywords)
    }
}
