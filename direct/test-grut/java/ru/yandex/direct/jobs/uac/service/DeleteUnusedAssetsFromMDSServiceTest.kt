package ru.yandex.direct.jobs.uac.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
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
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.UacGrutPage
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.entity.uac.service.GrutUacDeleteImageContentService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.result.Result
import ru.yandex.grut.client.RequestThrottledException
import ru.yandex.yt.TError

@GrutJobsTest
@ExtendWith(SpringExtension::class)
class DeleteUnusedAssetsFromMDSServiceTest {

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Mock
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    @Mock
    private lateinit var grutUacDeleteImageContentService: GrutUacDeleteImageContentService

    @Mock
    private lateinit var grutUacContentService: GrutUacContentService

    private lateinit var deleteUnusedAssetsFromMDSService: DeleteUnusedAssetsFromMDSService

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var clientId: ClientId

    companion object {
        private const val THUMB = "https://avatars.mds.yandex.net:443/get-uac-test/6211996/0a5f31c3/thumb"
        private const val DIRECT_HASH = "directHash"
    }

    @BeforeEach
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!
        clientId = grutSteps.createClient(clientInfo)
        grutUacCampaignService = mock()
        grutUacDeleteImageContentService = mock()
        grutUacContentService = mock()
        deleteUnusedAssetsFromMDSService = DeleteUnusedAssetsFromMDSService(
            grutUacCampaignService,
            grutUacDeleteImageContentService,
            grutUacContentService,
            ppcPropertiesSupport)

        whenever(grutUacContentService.deleteContent(any()))
            .thenReturn(Result.successful(null))
    }

    /**
     * Юнит для проверки успешного флоу, стабы вместо всех внешних сервисов (Грут, MDS)
     */
    @Test
    fun testAssetDeleteBuildingBlocks_successful() {
        getClientCampaignAssets()
        val minLookupTimeInitial = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        whenever(grutUacDeleteImageContentService.deleteUnusedAsset(any(), any(), any()))
            .thenReturn(true)

        getAssetsUpperHalfCreationTimeIntervalMock()

        val time = LocalDateTime.now()
        val result = deleteUnusedAssetsFromMDSService.execute(
            time.minusMinutes(45),
            time)

        val minLookupTimeFinal = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка количества удалений")
                .isEqualTo(1)
            assertThat(minLookupTimeFinal)
                .`as`("Проверка изменения проперти DELETE_UAC_ASSET_MIN_LOOKUP_TIME для следующего запуска")
                .isEqualTo(time)
                .isNotEqualTo(minLookupTimeInitial)
                .isAfter(minLookupTimeInitial)
        }.assertAll()

    }

    /**
     * Юнит для проверки корректного завершения джобы, ошибка на стороне MDS
     */
    @Test
    fun testAssetDeleteBuildingBlocks_unsuccessful_MDS_error() {
        getClientCampaignAssets()
        val minLookupTimeInitial = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        whenever(grutUacDeleteImageContentService.deleteUnusedAsset(any(), any(), any()))
            .thenReturn(false)

        getAssetsUpperHalfCreationTimeIntervalMock()

        val time = LocalDateTime.now()
        val result = deleteUnusedAssetsFromMDSService.execute(
            time.minusMinutes(45),
            time)

        val minLookupTimeFinal = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка количества удалений")
                .isEqualTo(0)
            assertThat(minLookupTimeFinal)
                .`as`("Проверка изменения проперти DELETE_UAC_ASSET_MIN_LOOKUP_TIME для следующего запуска")
                .isEqualTo(time)
                .isNotEqualTo(minLookupTimeInitial)
                .isAfter(minLookupTimeInitial)
        }.assertAll()
    }

    /**
     * Юнит для проверки корректного завершения джобы, ошибка получения ассетов из грута
     */
    @Test
    fun testAssetDeleteBuildingBlocks_unsuccessful_getAssetException() {
        getAssetsUpperHalfCreationTimeIntervalExceptionMock()

        val minLookupTimeInitial = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        val time = LocalDateTime.now()
        val result = deleteUnusedAssetsFromMDSService.execute(
            time.minusMinutes(45),
            time)

        val minLookupTimeFinal = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка количества удалений")
                .isEqualTo(0)

            assertThat(minLookupTimeFinal)
                .`as`("Проверка изменения проперти DELETE_UAC_ASSET_MIN_LOOKUP_TIME для следующего запуска")
                .isEqualTo(time)
                .isNotEqualTo(minLookupTimeInitial)
                .isAfter(minLookupTimeInitial)
        }.assertAll()
    }

    /**
     * Юнит для проверки корректного завершения джобы, ошибка получения данных об ассетах кампаний из грута
     */
    @Test
    fun testAssetDeleteBuildingBlocks_unsuccessful_getClientCampaignAssetsException() {
        getAssetsUpperHalfCreationTimeIntervalMock()
        getClientCampaignAssetsException()

        val minLookupTimeInitial = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        val time = LocalDateTime.now()
        val result = deleteUnusedAssetsFromMDSService.execute(
            time.minusMinutes(45),
            time)

        val minLookupTimeFinal = ppcPropertiesSupport
            .get(PpcPropertyNames.DELETE_UAC_ASSET_MIN_LOOKUP_TIME)
            .getOrDefault(LocalDateTime.MIN)

        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка количества удалений")
                .isEqualTo(0)

            assertThat(minLookupTimeFinal)
                .`as`("Проверка изменения проперти DELETE_UAC_ASSET_MIN_LOOKUP_TIME для следующего запуска")
                .isEqualTo(time)
                .isNotEqualTo(minLookupTimeInitial)
                .isAfter(minLookupTimeInitial)
        }.assertAll()
    }

    private fun getAssetsUpperHalfCreationTimeIntervalExceptionMock() {
        whenever(grutUacContentService.getAssetsUpperHalfCreationTimeInterval(
            any(),
            any(),
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )).thenThrow(RequestThrottledException(TError.getDefaultInstance()))
    }

    private fun getAssetsUpperHalfCreationTimeIntervalMock() {
        val assetId = grutSteps.createDefaultImageAsset(clientId)
        whenever(grutUacContentService.getAssetsUpperHalfCreationTimeInterval(
            any(),
            any(),
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )).thenReturn(
            UacGrutPage(
                listOf(UacYdbContent(
                    assetId,
                    clientId.toString(),
                    MediaType.IMAGE,
                    THUMB,
                    THUMB,
                    null,
                    emptyMap(),
                    null,
                    null,
                    clientId.toString(),
                    DIRECT_HASH
                )), null)
        )

        whenever(grutUacDeleteImageContentService.uacThumbToAvatarId(THUMB, "/"))
            .thenReturn("get-uac-test/6211996/0a5f31c3")
    }

    private fun getClientCampaignAssets() {
        whenever(grutUacCampaignService.getClientCampaignAssets(any(), any(), any()))
            .thenReturn(emptySet())

    }

    private fun getClientCampaignAssetsException() {
        whenever(grutUacCampaignService.getClientCampaignAssets(any(), any(), any()))
            .thenThrow(RequestThrottledException(TError.getDefaultInstance()))
    }
}
