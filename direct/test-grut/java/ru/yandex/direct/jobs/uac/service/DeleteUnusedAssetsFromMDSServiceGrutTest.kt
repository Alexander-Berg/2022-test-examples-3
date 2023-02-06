package ru.yandex.direct.jobs.uac.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import java.time.LocalDateTime
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory
import ru.yandex.direct.core.entity.uac.GrutTestHelpers
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider
import ru.yandex.direct.core.entity.uac.model.UacGrutPage
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacClientService
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.entity.uac.service.GrutUacDeleteImageContentService
import ru.yandex.direct.core.entity.uac.service.RmpCampaignService
import ru.yandex.direct.core.entity.uac.service.UacAvatarsService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.grut.objects.proto.AssetLink
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.MediaType
import ru.yandex.grut.objects.proto.client.Schema

@GrutJobsTest
@ExtendWith(SpringExtension::class)
class DeleteUnusedAssetsFromMDSServiceGrutTest {

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacContentService: GrutUacContentService

    @Autowired
    private lateinit var rmpCampaignService: RmpCampaignService

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var campaignSubObjectAccessCheckerFactory: CampaignSubObjectAccessCheckerFactory

    @Autowired
    private lateinit var uacClientService: GrutUacClientService

    @Autowired
    private lateinit var grut: GrutContext

    @Autowired
    private lateinit var grutTransactionProvider: GrutTransactionProvider

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    @Mock
    private lateinit var uacAvatarsService: UacAvatarsService

    private lateinit var deleteUnusedAssetsFromMDSService: DeleteUnusedAssetsFromMDSService

    private lateinit var grutUacCampaignService: GrutUacCampaignService

    private lateinit var grutUacDeleteImageContentService: GrutUacDeleteImageContentService

    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo

    @BeforeEach
    fun init() {
        uacAvatarsService = mock()
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        grutSteps.createClient(clientInfo)
        userInfo = clientInfo.chiefUserInfo!!

        grutUacDeleteImageContentService = GrutUacDeleteImageContentService(
            grutUacContentService,
            uacAvatarsService
        )

        grutUacCampaignService = GrutUacCampaignService(
            uacContentService,
            rmpCampaignService,
            campaignService,
            campaignSubObjectAccessCheckerFactory,
            uacClientService,
            grut,
            grutTransactionProvider,
            grutApiService,
        )

        deleteUnusedAssetsFromMDSService = DeleteUnusedAssetsFromMDSService(
            grutUacCampaignService,
            grutUacDeleteImageContentService,
            grutUacContentService,
            ppcPropertiesSupport
        )
    }

    /**
     * Тест проверяет механизм работы пагинации для метода getAssetsUpperHalfCreationTimeInterval
     * 1) контролирует количество выдаваемых страниц и их объем согласно указанному размеру страницы
     * 2) контролирует набор данных, подходящих по условию по времени
     * 3) проверяет, что все входные данные доступны для получения
     */
    @Test
    fun testAssetDelete_getAssetsUpperHalfCreationTimeInterval_paginationTest() {
        val limit = 3L
        val fromTime = UacYdbUtils.toEpochSecond(LocalDateTime.now()) * 1_000_000
        var toTime = fromTime
        val assetIds = mutableSetOf<String>()
        var extraId: String? = null
        for (i in 0..6) {
            val id = grutSteps.createDefaultImageAsset(clientId)
            assetIds.add(id)
            toTime = grutUacContentService.getAssets(setOf(id)).map { it.meta.creationTime }[0]
            assertThatKt(toTime)
                .isNotNull
                .isGreaterThan(fromTime)

            extraId = id
        }

        var token: String? = null
        val chunks = mutableListOf<UacGrutPage<List<UacYdbContent>>>()
        for (i in 0..2) {
            val result = grutUacContentService.getAssetsUpperHalfCreationTimeInterval(
                fromTime,
                toTime,
                MediaType.EMediaType.MT_IMAGE,
                token,
                limit
            )
            token = result.continuationToken
            assertThatKt(token != null).isTrue
            chunks.add(result)
        }

        val contentsWithoutExtraAsset = grutUacContentService.getAssetsUpperHalfCreationTimeInterval(
            fromTime,
            toTime,
            MediaType.EMediaType.MT_IMAGE
        )

        /**
         * incrementing toTime to get an extra asset, 'cause getAssetsUpperHalfCreationTimeInterval works on
         *  the interval [from, to)
         */
        val contentsWithExtraAsset = grutUacContentService.getAssetsUpperHalfCreationTimeInterval(
            fromTime,
            ++toTime,
            MediaType.EMediaType.MT_IMAGE
        )

        val fullAssetIds = contentsWithoutExtraAsset.content
            .map { it.id }
            .toSet()
        val extraAssetIds = contentsWithExtraAsset.content
            .map { it.id }
            .toSet()

        val expectedAssetIds = chunks.map { it.content }
            .flatten()
            .map { it.id }
            .toSet()
        val expectedAssetIdsWithExtra = expectedAssetIds
            .plus(extraId)
            .toSet()

        val expectedChunkSizes = listOf(3, 3, 0)
        val actualChunkSizes = chunks.map { it.content.size }

        SoftAssertions().apply {
            assertThat(extraAssetIds)
                .`as`("Проверка id созданных ассетов c extra")
                .hasSize(7)
                .containsAll(assetIds)
            assertThat(actualChunkSizes)
                .`as`("Проверка размеров полученных чанков с сохранением порядка выдачи")
                .hasSize(3)
                .containsExactlyElementsOf(expectedChunkSizes)
            assertThat(fullAssetIds)
                .`as`("Проверка id полученных ассетов")
                .hasSize(6)
                .containsAll(expectedAssetIds)
            assertThat(extraAssetIds)
                .`as`("Проверка id полученных ассетов + экстра")
                .hasSize(7)
                .containsAll(expectedAssetIdsWithExtra)
        }.assertAll()

        assetIds.forEach { grutUacContentService.deleteContent(it) }
    }

    /**
     * Проверка полного флоу удаления неиспользуемых ассетов
     * и контроль корректного изменения DELETE_UAC_ASSET_MIN_LOOKUP_TIME
     */
    @Test
    fun testAssetDelete_fullFlow_successful() {
        val fromTime = LocalDateTime.now()
        whenever(uacAvatarsService.deleteImage(any()))
            .thenReturn(true)

        val minLookupTimeInitial = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        val assetId = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)

        for (i in 0..4) {
            createGrutCampaign(setOf(grutSteps.createDefaultImageAsset(clientInfo.clientId!!)))
        }

        val toTime = LocalDateTime.now().plusSeconds(1)
        val result = deleteUnusedAssetsFromMDSService.execute(
            fromTime,
            toTime)

        val minLookupTimeFinal = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        val assets = grutUacContentService.getAssets(listOf(assetId))
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка количества удалений")
                .isEqualTo(1)
            assertThat(assets)
                .`as`("Проверка удаления ассета")
                .isEmpty()
            assertThat(minLookupTimeFinal)
                .`as`("Проверка изменения проперти DELETE_UAC_ASSET_MIN_LOOKUP_TIME для следующего запуска")
                .isEqualTo(toTime)
                .isNotEqualTo(minLookupTimeInitial)
                .isAfter(minLookupTimeInitial)
        }.assertAll()
    }

    /**
     * Тест проверяет работу getClientCampaignAssets с активной пагинацией
     * посредством контроля выходные данных (метод должен вернуть только id ассета, привязанного к кампании,
     * исключив ассет, который не был включен в кампанию)
     */
    @Test
    fun testAssetDelete_testGetClientCampaignAssets_withPagination() {
        val assetIds = listOf(0, 1, 2, 3, 4)
            .map { grutSteps.createDefaultImageAsset(clientInfo.clientId!!) }
        createGrutCampaign(assetIds.subList(0, 4).toSet())

        val assetId = assetIds[0]
        val absentAssetId = assetIds[4]

        val result = grutUacCampaignService
            .getClientCampaignAssets(clientInfo.clientId!!.toString(), setOf(assetId, absentAssetId), 2)
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка ассетов на вхождение в кампании клиентов")
                .hasSize(1)
                .containsAll(setOf(assetId))
                .doesNotContain(absentAssetId)
        }.assertAll()
    }

    private fun createGrutCampaign(assetIds: Set<String>): Schema.TCampaign {
        val campaignInfo = typedCampaignStepsUnstubbed.createDefaultTextCampaign(userInfo, clientInfo)

        val campaignSpec = UacGrutCampaignConverter.toCampaignSpec(createYdbCampaign())
        val campaignId = grutApiService.briefGrutApi
            .createBrief(
                GrutTestHelpers.buildCreateCampaignRequest(
                    clientId = clientInfo.clientId!!.asLong(),
                    campaignId = campaignInfo.campaign.id,
                    campaignType = Campaign.ECampaignTypeOld.CTO_MOBILE_APP,
                    campaignSpec = campaignSpec
                )
            )

        val campaign = grutApiService.briefGrutApi.getBrief(campaignId)!!

        val links = assetIds.associateBy { UacYdbUtils.generateUniqueRandomId() }
        setAssetLinksToCampaign(campaign, links)
        return campaign
    }

    private fun setAssetLinksToCampaign(
        grutCampaign: Schema.TCampaign,
        assetLinkIdToAssetId: Map<String, String>,
        assetLinksRemoveAt: Long? = null,
    ) {
        val assetLinks = assetLinkIdToAssetId.map { (assetLinkId, assetId) ->
            AssetLink.TAssetLink.newBuilder().apply {
                id = assetLinkId.toIdLong()
                this.assetId = assetId.toIdLong()
                if (assetLinksRemoveAt != null) {
                    removeTime = assetLinksRemoveAt.toInt()
                }
                order = 1
                createTime = Instant.now().epochSecond.toInt()
            }.build()
        }
        grutApiService.briefGrutApi.updateBriefFull(
            Schema.TCampaign.newBuilder().apply {
                meta = Schema.TCampaignMeta.newBuilder().setId(grutCampaign.meta.id).build()
                spec = Campaign.TCampaignSpec.newBuilder(grutCampaign.spec)
                    .setBriefAssetLinks(AssetLink.TAssetLinks.newBuilder().addAllLinks(assetLinks).build()).build()
            }.build()
        )
    }
}
