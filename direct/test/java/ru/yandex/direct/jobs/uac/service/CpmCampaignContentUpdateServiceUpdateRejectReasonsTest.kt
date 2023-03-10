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
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagData
import ru.yandex.direct.core.entity.moderationdiag.repository.ModerationDiagRepository
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.HTML5_CREATIVE
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.VIDEO_ADDITION
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createMediaCampaignContent
import ru.yandex.direct.core.entity.uac.createModerationDiagModel
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestNewCpmBanners
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.dbschema.ppc.Tables.MOD_REASONS
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsStatuspostmoderate
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest

/**
 * ?????????????????? ?????????????? updateRejectReasons ?? [CampaignContentUpdateService]
 */
@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CpmCampaignContentUpdateServiceUpdateRejectReasonsTest : AbstractUacRepositoryJobTest() {

    @Autowired
    private lateinit var campaignContentUpdateService: CampaignContentUpdateService

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var moderationDiagRepository: ModerationDiagRepository

    @Autowired
    private lateinit var moderationDiagService: ModerationDiagService

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var uacCampaign: UacYdbCampaign
    private lateinit var uacDirectAdGroup: UacYdbDirectAdGroup

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        campaignInfo = typedCampaignStepsUnstubbed.createCpmBannerCampaign(
            userInfo, clientInfo,
            TestCampaigns.defaultCpmBannerCampaignWithSystemFields()
                .withUid(clientInfo.uid)
                .withClientId(clientInfo.clientId!!.asLong())
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
        )

        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo.toCampaignInfo())

        uacCampaign = createYdbCampaign(advType = AdvType.CPM_BANNER)
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        uacDirectAdGroup = createDirectAdGroup(
            directAdGroupId = adGroupInfo.adGroupId,
            directCampaignId = uacCampaign.id,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)
        moderationDiagService.invalidateAll()
    }

    @AfterEach
    fun after() {
        moderationDiagService.invalidateAll()
    }

    @Suppress("unused")
    fun parametersForRejectReasons() =
        listOf(
            Arguments.of(
                "?????? BANNER ?????? title/text ???????????????? -> ???????????????? ???????????? ????????????",
                BANNER, false, true,
            ),
            Arguments.of(
                "?????? BANNER ?????? title/text ???????????????? -> ???????????????? ???????????? ????????????",
                BANNER, true, false,
            ),
            Arguments.of(
                "?????? BANNER ?????? title/text ???????????????? -> ???????????????? ???????????? ????????????",
                BANNER, true, true,
            ),
            Arguments.of(
                "?????? HTML5_CREATIVE ?????? html5 ???????????????? -> ???????????????? ???????????? ????????????",
                HTML5_CREATIVE, true, false,
            ),
            Arguments.of(
                "?????? HTML5_CREATIVE ?????? video ???????????????? -> ???????????????? ???????????? ????????????",
                HTML5_CREATIVE, false, true,
            ),
            Arguments.of(
                "?????? VIDEO_ADDITION ?????? video ???????????????? -> ???????????????? ???????????? ????????????",
                VIDEO_ADDITION, false, true,
            ),
            Arguments.of(
                "?????? VIDEO_ADDITION ?????? image ???????????????? -> ???? ???????????????? ???????????? ????????????",
                VIDEO_ADDITION, true, false
            ),
        )

    /**
     * ?????????????????? ???????????????????? ???????????? ???????????????????? ?????? ???????????? ????????????????????
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForRejectReasons")
    fun updateRejectReasons(
        description: String,
        moderationReasonObjectType: ModerationReasonObjectType,
        hasHtml5Content: Boolean,
        hasVideoContent: Boolean,
    ) {
        // ???????????????????? ?????? ?? ???????????????????? ??????????????, ???? ?????????? ?????? ???? ????????????, ?????? ?????? ???????????? ???????????? ???????????????????? ???? ?????? ???? ????????????????
        val bannerId = createMobileContentBanner()

        val uacDirectAd = createDirectAd(
            directHtml5ContentId = if (hasHtml5Content) UacYdbUtils.generateUniqueRandomId() else null,
            directVideoContentId = if (hasVideoContent) UacYdbUtils.generateUniqueRandomId() else null,
            directContentId = null,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)
        val moderationDiag = createModerationDiag(token = "tokenText")
        insertModReasons(bannerId, moderationReasonObjectType, moderationDiag)

        val html5CampaignContent = if (!hasHtml5Content) null else createCampaignContent(
            id = uacDirectAd.directHtml5ContentId!!, type = MediaType.HTML5,
            campaignId = campaignInfo.id.toString(),
        )
        val videoCampaignContent = if (!hasVideoContent) null else createCampaignContent(
            id = uacDirectAd.directVideoContentId!!, type = MediaType.VIDEO,
            campaignId = campaignInfo.id.toString(),
        )

        val campaignContents = listOfNotNull(
            html5CampaignContent,
            videoCampaignContent,
        )

        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(
            campaignInfo.id.toString(),
            campaignInfo.clientId,
            listOf(uacDirectAdGroup),
            campaignContents
        )

        val expectModerationDiagData = setOf(
            ModerationDiagData(
                diagId = moderationDiag.id.toString(),
                token = moderationDiag.token,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiag.diagText,
                shortText = moderationDiag.shortText,
                allowFirstAid = "Yes"
            )
        )

        // ???????????????? rejectReasons ?????????????? ?????????????? ????????????????
        val expectContentIdToRejectReasons = mutableMapOf<String, Set<ModerationDiagData>>()
        if (moderationReasonObjectType == BANNER) {
            videoCampaignContent?.let { expectContentIdToRejectReasons[it.id] = expectModerationDiagData }
            html5CampaignContent?.let { expectContentIdToRejectReasons[it.id] = expectModerationDiagData }
        } else {
            if (moderationReasonObjectType == HTML5_CREATIVE && html5CampaignContent != null) {
                expectContentIdToRejectReasons[html5CampaignContent.id] = expectModerationDiagData
            } else if (moderationReasonObjectType == VIDEO_ADDITION && videoCampaignContent != null) {
                expectContentIdToRejectReasons[videoCampaignContent.id] = expectModerationDiagData
            }
        }

        // ?????????????????? ???? ?????? ?????? RejectReasons
        campaignContents
            .filter { !expectContentIdToRejectReasons.containsKey(it.id) }
            .forEach { expectContentIdToRejectReasons[it.id] = emptySet() }

        checkRejectedReasons(expectContentIdToRejectReasons)
    }

    /**
     * ???????? ?????? ???????????????? campaignContents ?? ???????????? ?????????????????? ???????????? ????????????????????
     * -> ?????? ?????????????? ???????????????????? ?????????????????????? ?? reject_reasons
     */
    @Test
    fun updateRejectReasons_WithTwoRejectReasons_SaveAllRejectReasons() {
        val bannerId = createMobileContentBanner()

        // title ?? text ???????????? ???????? ?? ??????????????
        val titleCampaignContent = createCampaignContent(
            campaignId = campaignInfo.id.toString(),
        )
        val textCampaignContent = createCampaignContent(
            campaignId = campaignInfo.id.toString(),
        )
        val uacDirectAd = createDirectAd(
            titleContentId = titleCampaignContent.id,
            textContentId = textCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)
        val campaignContents = mutableListOf(titleCampaignContent, textCampaignContent)
        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)

        val moderationDiag = createModerationDiag(token = "tokenText")
        val moderationDiagAnotherToken = createModerationDiag(token = "tokenText2", shortText = "shortText2")
        insertModReasons(bannerId, BANNER, listOf(moderationDiag, moderationDiagAnotherToken))
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(
            campaignInfo.id.toString(),
            campaignInfo.clientId,
            listOf(uacDirectAdGroup),
            campaignContents
        )

        val expectModerationDiagData = setOf(
            ModerationDiagData(
                diagId = moderationDiag.id.toString(),
                token = moderationDiag.token,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiag.diagText,
                shortText = moderationDiag.shortText,
                allowFirstAid = "Yes"
            ),
            ModerationDiagData(
                diagId = moderationDiagAnotherToken.id.toString(),
                token = moderationDiagAnotherToken.token,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiagAnotherToken.diagText,
                shortText = moderationDiagAnotherToken.shortText,
                allowFirstAid = "Yes"
            ),
        )

        // ???????????????? rejectReasons ?????????????? ?????????????? ????????????????
        val expectContentIdToRejectReasons = mutableMapOf(
            titleCampaignContent.id to expectModerationDiagData,
            textCampaignContent.id to expectModerationDiagData,
        )

        checkRejectedReasons(expectContentIdToRejectReasons)
    }

    /**
     * ??????????????????, ?????? ???????? token ?? ?????????????? ???????????????????? null, ???? ???? ?????????? npe ?? ?????????????? ?????????????????? ????????????????
     */
    @Test
    fun updateRejectReasons_NullToken() {
        val bannerId = createMobileContentBanner()
        // title ?? text ???????????? ???????? ?? ??????????????

        val videoCampaignContent =
            createMediaCampaignContent(
                campaignId = campaignInfo.id.toString(), type = MediaType.VIDEO
            )

        val campaignContents = listOf(videoCampaignContent)

        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)

        val uacDirectAd = createDirectAd(
            directVideoContentId = videoCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)
        val moderationDiagWithNullToken = createModerationDiag(token = null)
        insertModReasons(bannerId, BANNER, moderationDiagWithNullToken)

        campaignContentUpdateService
            .updateCampaignContentsAndCampaignFlags(
                campaignInfo.id.toString(),
                campaignInfo.clientId,
                listOf(uacDirectAdGroup),
                campaignContents
            )

        val expectModerationDiagData = setOf(
            ModerationDiagData(
                diagId = moderationDiagWithNullToken.id.toString(),
                token = null,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiagWithNullToken.diagText,
                shortText = moderationDiagWithNullToken.shortText,
                allowFirstAid = "Yes"
            ),
        )

        // ???????????????? rejectReasons ?????????????? ?????????????? ????????????????
        val expectContentIdToRejectReasons = mutableMapOf(
            videoCampaignContent.id to expectModerationDiagData,
        )

        checkRejectedReasons(expectContentIdToRejectReasons)
    }

    /**
     * ??????????????????, ?????? ???????? token ?? ???????????? ???????????????????? ??????????????????, ???? ???????????????? ???????????? ???????? ??????????????
     */
    @Test
    fun updateRejectReasons_SameToken() {
        val bannerId = createMobileContentBanner()
        val bannerId2 = createMobileContentBanner()

        // title ?? text ???????????? ???????? ?? ??????????????
        val titleCampaignContent = createCampaignContent(
            campaignId = campaignInfo.id.toString(),
        )
        val textCampaignContent = createCampaignContent(
            campaignId = campaignInfo.id.toString(),
        )
        val campaignContents = listOf(titleCampaignContent, textCampaignContent)
        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)
        val uacDirectAd1 = createDirectAd(
            titleContentId = titleCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId,
        )
        val uacDirectAd2 = createDirectAd(
            titleContentId = titleCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId2,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd1)
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd2)
        val moderationDiag = createModerationDiag(token = "tokenText")
        val moderationDiag2 = createModerationDiag(token = "tokenText", shortText = "shortText2")
        insertModReasons(bannerId, BANNER, moderationDiag)
        insertModReasons(bannerId2, BANNER, moderationDiag2)

        campaignContentUpdateService
            .updateCampaignContentsAndCampaignFlags(
                campaignInfo.id.toString(),
                campaignInfo.clientId,
                listOf(uacDirectAdGroup),
                campaignContents
            )

        val expectModerationDiagData1 =
            ModerationDiagData(
                diagId = moderationDiag.id.toString(),
                token = moderationDiag.token,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiag.diagText,
                shortText = moderationDiag.shortText,
                allowFirstAid = "Yes"
            )
        val expectModerationDiagData2 =
            ModerationDiagData(
                diagId = moderationDiag2.id.toString(),
                token = moderationDiag2.token,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiag2.diagText,
                shortText = moderationDiag2.shortText,
                allowFirstAid = "Yes"
            )

        val campaignContentIdToRejectedReasons =
            uacYdbCampaignContentRepository.getCampaignContents(campaignInfo.id.toString())
                .associateBy({ it.id }, { HashSet(it.rejectReasons) })

        assertThat(campaignContentIdToRejectedReasons[titleCampaignContent.id]).hasSize(1)
        assertThat(campaignContentIdToRejectedReasons[titleCampaignContent.id]!!.first()).isIn(
            expectModerationDiagData1,
            expectModerationDiagData2
        )
    }

    /**
     * ??????????????????, ?????? ???????? token ?? ???????????? ???????????????????? ?????????????????? ?? ?????????? null, ???? ???????????????? ?????? ??????????????
     */
    @Test
    fun updateRejectReasons_SameNullToken() {
        val bannerId = createMobileContentBanner()
        val bannerId2 = createMobileContentBanner()
        // title ?? text ???????????? ???????? ?? ??????????????
        val titleCampaignContent = createCampaignContent(
            campaignId = campaignInfo.id.toString(),
        )
        val textCampaignContent = createCampaignContent(
            campaignId = campaignInfo.id.toString(),
        )
        val uacDirectAd1 = createDirectAd(
            titleContentId = titleCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId,
        )
        val uacDirectAd2 = createDirectAd(
            titleContentId = titleCampaignContent.id,
            directAdGroupId = uacDirectAdGroup.id,
            directAdId = bannerId2,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd1)
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd2)
        val moderationDiagWithNullToken = createModerationDiag(token = null)
        val moderationDiagWithNullToken2 = createModerationDiag(token = null, shortText = "shortText2")
        insertModReasons(bannerId, BANNER, moderationDiagWithNullToken)
        insertModReasons(bannerId2, BANNER, moderationDiagWithNullToken2)

        val campaignContents = listOf(titleCampaignContent, textCampaignContent)
        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)

        campaignContentUpdateService
            .updateCampaignContentsAndCampaignFlags(
                campaignInfo.id.toString(),
                campaignInfo.clientId,
                listOf(uacDirectAdGroup),
                campaignContents
            )

        val expectModerationDiagData = setOf(
            ModerationDiagData(
                diagId = moderationDiagWithNullToken.id.toString(),
                token = null,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiagWithNullToken.diagText,
                shortText = moderationDiagWithNullToken.shortText,
                allowFirstAid = "Yes"
            ),
            ModerationDiagData(
                diagId = moderationDiagWithNullToken2.id.toString(),
                token = null,
                showDetailsUrl = 0,
                badReason = "Yes",
                unbanIsProhibited = "No",
                diagText = moderationDiagWithNullToken2.diagText,
                shortText = moderationDiagWithNullToken2.shortText,
                allowFirstAid = "Yes"
            ),
        )

        val campaignContentIdToRejectedReasons =
            uacYdbCampaignContentRepository.getCampaignContents(campaignInfo.id.toString())
                .associateBy({ it.id }, { HashSet(it.rejectReasons) })

        assertThat(campaignContentIdToRejectedReasons[titleCampaignContent.id]).hasSize(2)
        assertThat(campaignContentIdToRejectedReasons[titleCampaignContent.id]).isEqualTo(expectModerationDiagData)
    }

    private fun checkRejectedReasons(expectContentIdToRejectReasons: Map<String, Set<ModerationDiagData>>) {
        val campaignContentIdToRejectedReasons =
            uacYdbCampaignContentRepository.getCampaignContents(campaignInfo.id.toString())
                .associateBy({ it.id }, { HashSet(it.rejectReasons) })

        assertThat(campaignContentIdToRejectedReasons)
            .`as`("?????????????? ????????????????????")
            .isEqualTo(expectContentIdToRejectReasons)
    }

    private fun createMobileContentBanner(
        bannerStatusModerate: BannerStatusModerate = BannerStatusModerate.NO,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate = BannerCreativeStatusModerate.YES,
    ): Long {
        val creativeInfo = steps.creativeSteps().createCreative(clientInfo)

        val banner = TestNewCpmBanners
            .fullCpmBanner(campaignInfo.id, adGroupInfo.adGroupId, creativeInfo.creativeId)
            .withStatusModerate(bannerStatusModerate)
            .withBsBannerId(0L)
            .withCreativeStatusModerate(bannerCreativeStatusModerate)
            .withCreativeId(creativeInfo.creativeId)
            .withBsBannerId(0L)

        return steps.cpmBannerSteps()
            .createCpmBanner(
                NewCpmBannerInfo()
                    .withClientInfo(clientInfo)
                    .withAdGroupInfo(adGroupInfo)
                    .withBanner(banner)
            ).bannerId
    }

    private fun insertModReasons(id: Long, type: ModerationReasonObjectType, moderationDiags: List<ModerationDiag>) {
        val modReasonDetailed = reasonsToDbFormat(moderationDiags.map { ModerationReasonDetailed().withId(it.id) })
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(MOD_REASONS)
            .columns(
                MOD_REASONS.ID,
                MOD_REASONS.CLIENT_ID,
                MOD_REASONS.TYPE,
                MOD_REASONS.STATUS_MODERATE,
                MOD_REASONS.STATUS_POST_MODERATE,
                MOD_REASONS.REASON
            )
            .values(
                id,
                clientInfo.clientId!!.asLong(),
                ModerationReasonObjectType.toSource(type),
                ModReasonsStatusmoderate.No,
                ModReasonsStatuspostmoderate.No,
                modReasonDetailed
            )
            .execute()
    }

    private fun insertModReasons(id: Long, type: ModerationReasonObjectType, moderationDiag: ModerationDiag) {
        insertModReasons(id, type, listOf(moderationDiag))
    }

    private fun createModerationDiag(token: String?, shortText: String = "shortText"): ModerationDiag {
        val moderationDiag = createModerationDiagModel(token, shortText, shortText)
        moderationDiagRepository.insertModerationDiagOrUpdateTexts(listOf(moderationDiag))
        return moderationDiag
    }
}
