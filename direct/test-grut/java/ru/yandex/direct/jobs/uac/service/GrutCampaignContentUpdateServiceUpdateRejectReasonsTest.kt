package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType
import ru.yandex.direct.core.entity.moderationdiag.repository.ModerationDiagRepository
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.IMAGE
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.VIDEO_ADDITION
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.core.entity.uac.service.GrutCampaignContentUpdateService
import ru.yandex.direct.dbschema.ppc.Tables.MOD_REASONS
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsStatuspostmoderate
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.tracing.util.TraceUtil
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_IMAGE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TITLE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_VIDEO
import ru.yandex.grut.objects.proto.RejectReasons.TRejectReason
import ru.yandex.grut.objects.proto.RejectReasons.TRejectReasons

/**
 * Проверяем функцию updateRejectReasons в [CampaignContentUpdateService]
 */
@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class GrutCampaignContentUpdateServiceUpdateRejectReasonsTest : BaseGrutCampaignContentUpdateServiceTest() {

    @Autowired
    private lateinit var campaignContentUpdateService: GrutCampaignContentUpdateService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var moderationDiagRepository: ModerationDiagRepository

    @Autowired
    private lateinit var moderationDiagService: ModerationDiagService

    open var adGroupBriefEnabled = false

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        createGrutClient(clientInfo)
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        moderationDiagService.invalidateAll()
    }

    @AfterEach
    fun after() {
        moderationDiagService.invalidateAll()
    }

    @Suppress("unused")
    fun parametersForRejectReasons() =
        listOf(
            Arguments.of("Тип BANNER для title/text контента -> получаем список причин",
                BANNER, false, false, false),
            Arguments.of("Тип IMAGE для image контента -> получаем список причин",
                IMAGE, true, false, false),
            Arguments.of("Тип VIDEO_ADDITION для video контента -> получаем список причин",
                VIDEO_ADDITION, false, true, false),
            Arguments.of("Тип IMAGE для title/text контента -> не получаем список причин",
                IMAGE, false, false, false),
            Arguments.of("Тип VIDEO_ADDITION для title/text контента -> не получаем список причин",
                VIDEO_ADDITION, false, false, false),
            Arguments.of("Тип IMAGE для video контента -> не получаем список причин",
                IMAGE, false, true, false),
            Arguments.of("Тип VIDEO_ADDITION для image контента -> не получаем список причин",
                VIDEO_ADDITION, true, false, false),

            Arguments.of("Тип BANNER для title/text контента -> получаем список причин",
                BANNER, false, false, true),
            Arguments.of("Тип IMAGE для image контента -> получаем список причин",
                IMAGE, true, false, true),
            Arguments.of("Тип VIDEO_ADDITION для video контента -> получаем список причин",
                VIDEO_ADDITION, false, true, true),
            Arguments.of("Тип IMAGE для title/text контента -> не получаем список причин",
                IMAGE, false, false, true),
            Arguments.of("Тип VIDEO_ADDITION для title/text контента -> не получаем список причин",
                VIDEO_ADDITION, false, false, true),
            Arguments.of("Тип IMAGE для video контента -> не получаем список причин",
                IMAGE, false, true, true),
            Arguments.of("Тип VIDEO_ADDITION для image контента -> не получаем список причин",
                VIDEO_ADDITION, true, false, true),
        )

    /**
     * Проверяем обновление причин отклонения при разных параметрах
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForRejectReasons")
    fun updateRejectReasons(
        description: String,
        moderationReasonObjectType: ModerationReasonObjectType,
        hasImageContent: Boolean,
        hasVideoContent: Boolean,
        newAssetLinkIdGeneration: Boolean,
    ) {
        // title и text всегда есть у баннера
        val assetTypes = mutableSetOf(MT_TITLE, MT_TEXT)
        if (hasImageContent) {
            assetTypes.add(MT_IMAGE)
        }
        if (hasVideoContent) {
            assetTypes.add(MT_VIDEO)
        }

        val testData = createTestData(
            assetTypes,
            newAssetLinkIdGeneration = newAssetLinkIdGeneration,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        createGrutBanner(testData, bannerId)

        val moderationDiag = createModerationDiag(token = "tokenText")
        insertModReasons(bannerId, moderationReasonObjectType, listOf(moderationDiag))

        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(testData.campaignInfo.shard, listOf(testData.campaignInfo.id))


        val expectModerationDiagData = TRejectReason.newBuilder().apply {
            diagId = moderationDiag.id.toString()
            token = moderationDiag.token
            showDetailsUrl = false
            badReason = true
            unbanIsProhibited = false
            diagText = moderationDiag.diagText
            shortText = moderationDiag.shortText
            allowFirstAid = true
        }.build()
        // Собираем rejectReasons которые ожидаем получить
        val expectAssetIdToRejectReasons = mutableMapOf<String, TRejectReasons>()
        if (moderationReasonObjectType == BANNER) {
            expectAssetIdToRejectReasons[testData.assetIdsByAssetTypes[MT_TITLE]!!] = rejectReasonsOf(expectModerationDiagData)
            expectAssetIdToRejectReasons[testData.assetIdsByAssetTypes[MT_TEXT]!!] = rejectReasonsOf(expectModerationDiagData)
        } else {
            if (moderationReasonObjectType == IMAGE && hasImageContent) {
                expectAssetIdToRejectReasons[testData.assetIdsByAssetTypes[MT_IMAGE]!!] = rejectReasonsOf(expectModerationDiagData)
            } else if (moderationReasonObjectType == VIDEO_ADDITION && hasVideoContent) {
                expectAssetIdToRejectReasons[testData.assetIdsByAssetTypes[MT_VIDEO]!!] = rejectReasonsOf(expectModerationDiagData)
            }
        }

        // Добавляем те что без RejectReasons
        testData.assetIdsByAssetTypes.values
            .filter { !expectAssetIdToRejectReasons.containsKey(it) }
            .forEach { expectAssetIdToRejectReasons[it] = rejectReasonsOf() }

        checkRejectedReasons(testData.campaignId, expectAssetIdToRejectReasons)
    }

    /**
     * Если при отправке campaignContents в списке несколько причин отклонений
     * -> все причины отклонений сохраняются в reject_reasons
     */
    @Test
    fun updateRejectReasons_WithTwoRejectReasons_SaveAllRejectReasons() {
        // title и text всегда есть у баннера
        val assetTypes = mutableSetOf(MT_TITLE, MT_TEXT)
        val testData = createTestData(
            assetTypes,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        createGrutBanner(testData, bannerId)

        val moderationDiag = createModerationDiag(token = "tokenText")
        val moderationDiagAnotherToken = createModerationDiag(token = "tokenText2", shortText = "shortText2")
        insertModReasons(bannerId, BANNER, listOf(moderationDiag, moderationDiagAnotherToken))
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(testData.campaignInfo.shard, listOf(testData.campaignInfo.id))

        val expectModerationDiagData = listOf(
            TRejectReason.newBuilder().apply {
                diagId = moderationDiag.id.toString()
                token = moderationDiag.token
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiag.diagText
                shortText = moderationDiag.shortText
                allowFirstAid = true
            }.build(),
            TRejectReason.newBuilder().apply {
                diagId = moderationDiagAnotherToken.id.toString()
                token = moderationDiagAnotherToken.token
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiagAnotherToken.diagText
                shortText = moderationDiagAnotherToken.shortText
                allowFirstAid = true
            }.build(),
        )

        // Собираем rejectReasons которые ожидаем получить
        val expectAssetIdToRejectReasons = mutableMapOf(
            testData.assetIdsByAssetTypes[MT_TITLE]!! to rejectReasonsOf(expectModerationDiagData),
            testData.assetIdsByAssetTypes[MT_TEXT]!! to rejectReasonsOf(expectModerationDiagData),
        )

        checkRejectedReasons(testData.campaignId, expectAssetIdToRejectReasons)
    }


    /**
     * Проверяет, что если token у причины отклонения null, то не будет npe и причина корректно вернется
     */
    @Test
    fun updateRejectReasons_NullToken() {
        // title и text всегда есть у баннера
        val assetTypes = mutableSetOf(MT_TITLE, MT_TEXT)
        val testData = createTestData(
            assetTypes,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        createGrutBanner(testData, bannerId)
        val moderationDiagWithNullToken = createModerationDiag(token = null)
        insertModReasons(bannerId, BANNER, listOf(moderationDiagWithNullToken))

        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(testData.campaignInfo.shard, listOf(testData.campaignInfo.id))

        val expectModerationDiagData =
            TRejectReason.newBuilder().apply {
                diagId = moderationDiagWithNullToken.id.toString()
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiagWithNullToken.diagText
                shortText = moderationDiagWithNullToken.shortText
                allowFirstAid = true
            }.build()

        // Собираем rejectReasons которые ожидаем получить
        val expectAssetIdToRejectReasons = mutableMapOf(
            testData.assetIdsByAssetTypes[MT_TITLE]!! to rejectReasonsOf(expectModerationDiagData),
            testData.assetIdsByAssetTypes[MT_TEXT]!! to rejectReasonsOf(expectModerationDiagData),
        )

        checkRejectedReasons(testData.campaignId, expectAssetIdToRejectReasons)
    }

    /**
     * Проверяет, что если token у причин отклонения совпадает, то вернется только одна причина
     */
    @Test
    fun updateRejectReasons_SameToken() {
        // title и text всегда есть у баннера
        val assetTypes = mutableSetOf(MT_TITLE)
        val testData = createTestData(
            assetTypes,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        val bannerId2 = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        createGrutBanner(testData, bannerId)
        createGrutBanner(testData, bannerId2)
        val moderationDiag = createModerationDiag(token = "tokenText")
        val moderationDiag2 = createModerationDiag(token = "tokenText", shortText = "shortText2")
        insertModReasons(bannerId, BANNER, listOf(moderationDiag))
        insertModReasons(bannerId2, BANNER, listOf(moderationDiag2))

        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(testData.campaignInfo.shard, listOf(testData.campaignInfo.id))
        val expectModerationDiagData1 =
            TRejectReason.newBuilder().apply {
                diagId = moderationDiag.id.toString()
                token = moderationDiag.token
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiag.diagText
                shortText = moderationDiag.shortText
                allowFirstAid = true
            }.build()
        val expectModerationDiagData2 =
            TRejectReason.newBuilder().apply {
                diagId = moderationDiag2.id.toString()
                token = moderationDiag2.token
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiag2.diagText
                shortText = moderationDiag2.shortText
                allowFirstAid = true
            }.build()


        val assetIdToRejectReasons = grutApiService.briefGrutApi.getBrief(testData.campaignId)!!
            .spec.briefAssetLinksStatuses.linkStatusesList
            .associate { it.assetId.toIdString() to it.rejectReasons }


        assertThat(assetIdToRejectReasons[testData.assetIdsByAssetTypes[MT_TITLE]!!]!!.rejectReasonsList).hasSize(1)
        assertThat(assetIdToRejectReasons[testData.assetIdsByAssetTypes[MT_TITLE]!!]!!.rejectReasonsList.first()).isIn(expectModerationDiagData1, expectModerationDiagData2)

    }

    /**
     * Проверяет, что если token у причин отклонения совпадает и равен null, то вернутся обе причины
     */
    @Test
    fun updateRejectReasons_SameNullToken() {
        val assetTypes = mutableSetOf(MT_TITLE)
        val testData = createTestData(
            assetTypes,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        val bannerId2 = createMobileContentBanner(testData, bannerStatusModerate = BannerStatusModerate.NO)
        createGrutBanner(testData, bannerId)
        createGrutBanner(testData, bannerId2)

        val moderationDiagWithNullToken = createModerationDiag(token = null)
        val moderationDiagWithNullToken2 = createModerationDiag(token = null, shortText = "shortText2")
        insertModReasons(bannerId, BANNER, listOf(moderationDiagWithNullToken))
        insertModReasons(bannerId2, BANNER, listOf(moderationDiagWithNullToken2))

        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(testData.campaignInfo.shard, listOf(testData.campaignInfo.id))
        val expectModerationDiagData = setOf(
            TRejectReason.newBuilder().apply {
                diagId = moderationDiagWithNullToken.id.toString()
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiagWithNullToken.diagText
                shortText = moderationDiagWithNullToken.shortText
                allowFirstAid = true
            }.build(),
            TRejectReason.newBuilder().apply {
                diagId = moderationDiagWithNullToken2.id.toString()
                showDetailsUrl = false
                badReason = true
                unbanIsProhibited = false
                diagText = moderationDiagWithNullToken2.diagText
                shortText = moderationDiagWithNullToken2.shortText
                allowFirstAid = true
            }.build(),
        )

        // Собираем rejectReasons которые ожидаем получить
        val expectAssetIdToRejectReasons = mutableMapOf(
            testData.assetIdsByAssetTypes[MT_TITLE]!! to rejectReasonsOf(expectModerationDiagData)
        )

        checkRejectedReasons(testData.campaignId, expectAssetIdToRejectReasons)
    }

    private fun checkRejectedReasons(campaignId: Long, expectContentIdToRejectReasons: Map<String, TRejectReasons>) {
        val assetIdToRejectReasons = grutApiService.briefGrutApi.getBrief(campaignId)!!
            .spec.briefAssetLinksStatuses.linkStatusesList
            .associate {
                it.assetId.toIdString() to
                    sortRejectReasons(it.rejectReasons)
            }
        val expectContentIdToRejectReasonsSorted = expectContentIdToRejectReasons
            .mapValues { sortRejectReasons(it.value) }
        assertThat(assetIdToRejectReasons)
            .`as`("причины отклонения")
            .isEqualTo(expectContentIdToRejectReasonsSorted)
    }

    private fun sortRejectReasons(reasons: TRejectReasons): TRejectReasons {
        return TRejectReasons.newBuilder().addAllRejectReasons(reasons.rejectReasonsList.sortedBy { it.diagId }).build()
    }

    private fun insertModReasons(id: Long, type: ModerationReasonObjectType, moderationDiags: List<ModerationDiag>) {
        val modReasonDetailed = reasonsToDbFormat(moderationDiags.map { ModerationReasonDetailed().withId(it.id) })
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(MOD_REASONS)
            .columns(MOD_REASONS.ID, MOD_REASONS.CLIENT_ID, MOD_REASONS.TYPE, MOD_REASONS.STATUS_MODERATE, MOD_REASONS.STATUS_POST_MODERATE, MOD_REASONS.REASON)
            .values(id, clientInfo.clientId!!.asLong(), ModerationReasonObjectType.toSource(type), ModReasonsStatusmoderate.No, ModReasonsStatuspostmoderate.No, modReasonDetailed)
            .execute()
    }

    private fun generateDiagId() = TraceUtil.randomId() % 1000000L + 2000 // для того, чтобы не пересечься с существующими

    private fun createModerationDiag(token: String?, shortText: String = "shortText"): ModerationDiag {
        val moderationDiag = ModerationDiag()
            .withId(generateDiagId())
            .withToken(token)
            .withType(ModerationDiagType.COMMON)
            .withStrongReason(true)
            .withUnbanIsProhibited(false)
            .withDiagText(shortText) // diagText = nvl(fullText, shortText), в базе нет отдельного поля
            .withShortText(shortText)
            .withAllowFirstAid(true)
        moderationDiagRepository.insertModerationDiagOrUpdateTexts(listOf(moderationDiag))
        return moderationDiag
    }

    private fun rejectReasonsOf(rejectReasons: Collection<TRejectReason>): TRejectReasons {
        return TRejectReasons.newBuilder().addAllRejectReasons(rejectReasons).build()
    }

    private fun rejectReasonsOf(rejectReason: TRejectReason): TRejectReasons {
        return rejectReasonsOf(listOf(rejectReason))
    }

    private fun rejectReasonsOf(): TRejectReasons {
        return rejectReasonsOf(listOf())
    }
}
