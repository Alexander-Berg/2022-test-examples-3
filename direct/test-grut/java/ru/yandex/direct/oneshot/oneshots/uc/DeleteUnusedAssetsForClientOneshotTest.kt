package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDateTime
import java.util.function.Consumer
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.entity.uac.service.GrutUacDeleteImageContentService
import ru.yandex.direct.core.entity.uac.service.UacAvatarsService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.oneshot.configuration.GrutOneshotTest
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtField
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTableRow

@GrutOneshotTest
@ExtendWith(SpringExtension::class)
class DeleteUnusedAssetsForClientOneshotTest {
    @Mock
    private lateinit var uacAvatarsService: UacAvatarsService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    private lateinit var grutUacDeleteImageContentService: GrutUacDeleteImageContentService
    private lateinit var deleteUnusedAssetsForClientOneshot: DeleteUnusedAssetsForClientOneshot
    private lateinit var ytProvider: YtProvider
    private lateinit var operator: YtOperator

    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun init() {
        uacAvatarsService = mock()
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        grutSteps.createClient(clientInfo)

        ytProvider = mock()
        operator = mock()
        whenever(ytProvider.getOperator(any())).thenReturn(operator)
        whenever(operator.exists(any())).thenReturn(true)

        grutUacDeleteImageContentService = GrutUacDeleteImageContentService(grutUacContentService, uacAvatarsService)
        deleteUnusedAssetsForClientOneshot = DeleteUnusedAssetsForClientOneshot(
            ytProvider,
            grutUacDeleteImageContentService,
            grutUacContentService,
            grutUacCampaignService,
        )
    }

    /**
     * Проверка полного флоу удаления неиспользуемых ассетов, с гарантией сохранности - задействованных в кампаниях
     */
    @Test
    fun testAssetDelete_fullFlow_successful() {
        val unusedAssetIds = createImageAssets(10)
        val campaignAssetIds = createImageAssets(5)

        doAnswer {
            val field = YtField("client_id", String::class.javaObjectType)
            val field2 = YtField("asset_id", String::class.javaObjectType)
            unusedAssetIds.forEach { assetId ->
                val row = YtTableRow(listOf(field, field2))
                row.setValue(field, clientId.toString())
                row.setValue(field2, assetId)
                val consumer = it.getArgument(1, Consumer::class.java) as Consumer<YtTableRow>
                consumer.accept(row)
            }
        }.whenever(operator)
            .readTableByRowRange(any(), any<Consumer<YtTableRow>>(), any(), any(), any())

        whenever(uacAvatarsService.deleteImage(any()))
            .thenReturn(true)

        createCampaign(campaignAssetIds.toList())
        val param = DeleteUnusedAssetsForClientParam(
            YtCluster.ZENO,
            "tablePath",
            1,
            LocalDateTime.now().plusDays(1))
        val result = deleteUnusedAssetsForClientOneshot.execute(param, null)

        val campaignAssetIdsAfter = grutUacContentService.getAssets(campaignAssetIds).map { it.meta.id.toIdString() }
        val unusedAssetIdsAfter = grutUacContentService.getAssets(unusedAssetIds).map { it.meta.id.toIdString() }
        SoftAssertions().apply {
            assertThat(result!!.deletedCount)
                .`as`("Проверка количества удалений")
                .isEqualTo(10)
            assertThat(unusedAssetIdsAfter)
                .`as`("Проверка удаления ассетов")
                .isEmpty()
            assertThat(campaignAssetIdsAfter)
                .`as`("Проверка целостности ассетов привязанных к кампании")
                .isNotEmpty
                .containsExactlyInAnyOrderElementsOf(campaignAssetIds)
        }.assertAll()
    }

    /**
     * Проверка полного флоу удаления неиспользуемых ассетов, с гарантией сохранности - задействованных в кампаниях,
     * достигнут передел удаления
     */
    @Test
    fun testAssetDelete_fullFlow_activeDeleteLimit_successful() {
        val unusedAssetIds = createImageAssets(10)
        val campaignAssetIds = createImageAssets(5)
        createCampaign(campaignAssetIds.toList())
        whenever(uacAvatarsService.deleteImage(any()))
            .thenReturn(true)

        doAnswer {
            val field = YtField("client_id", String::class.javaObjectType)
            val field2 = YtField("asset_id", String::class.javaObjectType)
            unusedAssetIds.forEach { assetId ->
                val row = YtTableRow(listOf(field, field2))
                row.setValue(field, clientId.toString())
                row.setValue(field2, assetId)
                val consumer = it.getArgument(1, Consumer::class.java) as Consumer<YtTableRow>
                consumer.accept(row)
            }
        }.whenever(operator)
            .readTableByRowRange(any(), any<Consumer<YtTableRow>>(), any(), any(), any())

        val param = DeleteUnusedAssetsForClientParam(
            YtCluster.ZENO,
            "tablePath",
            1,
            LocalDateTime.now().plusDays(1),
            0,
            5
        )
        val result = deleteUnusedAssetsForClientOneshot.execute(param, null)

        val campaignAssetIdsAfter = grutUacContentService.getAssets(campaignAssetIds).map { it.meta.id.toIdString() }
        val unusedAssetIdsAfter = grutUacContentService.getAssets(unusedAssetIds).map { it.meta.id.toIdString() }
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка возвращенного параметра")
                .isNull()
            assertThat(unusedAssetIdsAfter)
                .`as`("Проверка частичного удаления ассетов")
                .hasSize(5)
            assertThat(campaignAssetIdsAfter)
                .`as`("Проверка целостности ассетов привязанных к кампании")
                .isNotEmpty
                .hasSize(campaignAssetIds.size)
                .containsExactlyInAnyOrderElementsOf(campaignAssetIds)
        }.assertAll()

        for (element in unusedAssetIdsAfter) {
            grutUacContentService.deleteContent(element)
        }
    }

    private fun createImageAssets(count: Int = 1): Set<String> {
        val result = mutableSetOf<String>()
        val top = maxOf(1, count)
        for (i in 1..top) {
            val assetId = grutSteps.createDefaultImageAsset(clientId)
            result.add(assetId)
        }
        return result
    }

    private fun createCampaign(assets: List<String>): Long {
        val campaignId = grutSteps.createTextCampaign(clientInfo)
        grutSteps.setAssetLinksToCampaign(campaignId, assets)
        return campaignId
    }
}
