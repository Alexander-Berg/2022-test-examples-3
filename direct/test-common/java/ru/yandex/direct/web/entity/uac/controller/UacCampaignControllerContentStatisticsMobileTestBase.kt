package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.campaign_content.AssetStat
import ru.yandex.direct.core.entity.uac.repository.stat.AssetStatRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.test.utils.checkContains
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.createCreativeStatistics
import ru.yandex.direct.web.entity.uac.model.EMPTY_STAT
import ru.yandex.direct.web.entity.uac.model.UacContentStatisticsResponse

abstract class UacCampaignControllerContentStatisticsMobileTestBase {

    @Autowired
    protected lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    protected lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var assetStatRepositoryMock: AssetStatRepository

    protected lateinit var mockMvc: MockMvc
    protected lateinit var clientInfo: ClientInfo
    protected var uacCampaignId: Long = 0L
    protected lateinit var imageAssetId: String
    protected lateinit var imageAssetId2: String
    protected lateinit var imageAssetId3: String
    protected lateinit var videoAssetId: String
    protected lateinit var uacCampaignContents: List<UacYdbCampaignContent>

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    protected fun doContentStatisticsRequest(
        from: Long = LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.UTC),
        to: Long = LocalDateTime.now().plusDays(1).toEpochSecond(ZoneOffset.UTC),
        mediaType: MediaType? = null,
        sortBy: String? = null,
        orderBy: String? = null,
    ): String {
        val mediaTypeParam = mediaType?.let { "&media_type=${it.getType()}" } ?: ""
        val sortByParam = sortBy?.let { "&sortby=$it" } ?: ""
        val orderByParam = orderBy?.let { "&orderby=$it" } ?: ""
        return mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/$uacCampaignId/content_statistics?" +
                    "ulogin=${clientInfo.login}&from=${from}&to=${to}$mediaTypeParam$sortByParam$orderByParam")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
    }

    private fun buildAssetsStat(
        id: String,
        mediaType: MediaType,
        shows: Long = 1234,
        clicks: Long = 50,
        clicksCost: Long = 2,
        clicksCostCur: Long = 3,
        costMicros: Long = 4000000,
        costCurMicros: Long = 5000000,
        costTaxFreeMicros: Long = 234000000,
        conversions: Long = 6,
        installs: Long = 7,
        postViewConversions: Long = 8,
        postViewInstalls: Long = 9,
    ) = AssetStat(
        id,
        mediaType,
        shows.toBigDecimal(),
        clicks.toBigDecimal(),
        clicksCost.toBigDecimal(),
        clicksCostCur.toBigDecimal(),
        costMicros.toBigDecimal(),
        costCurMicros.toBigDecimal(),
        costTaxFreeMicros.toBigDecimal(),
        conversions.toBigDecimal(),
        installs.toBigDecimal(),
        postViewConversions.toBigDecimal(),
        postViewInstalls.toBigDecimal(),
    )

    private fun setAssetStatsResponse(assetStats: List<AssetStat>) {
        `when`(assetStatRepositoryMock.getStatsByAssetIds(any(), any(), any(), any()))
            .thenReturn(assetStats)
    }

    @Test
    fun `get content statistics mobile`() {
        setAssetStatsResponse(
            listOf(
                buildAssetsStat(imageAssetId, MediaType.IMAGE),
                buildAssetsStat(videoAssetId, MediaType.VIDEO, shows = 0, clicks = 0),
            )
        )
        val result = doContentStatisticsRequest()

        val uacContentStatisticsResponse = fromJson<UacContentStatisticsResponse>(result)
        val uacContentStatisticsWithMobileData = uacContentStatisticsResponse.results.filter {
            it.creativeStatistics != EMPTY_STAT
        }
        uacContentStatisticsWithMobileData.checkSize(2)

        val imageStats = uacContentStatisticsWithMobileData.first { it.id == imageAssetId }
        imageStats.creativeStatistics.checkEquals(
            createCreativeStatistics(
                shows = 1234,
                clicks = 50,
                conversions = 6,
                costMicros = 4000000,
                costCurMicros = 5000000,
                costTaxFreeMicros = 234000000,
                installs = 7,
                postViewConversions = 8,
                postViewInstalls = 9,
                ctr = 4.051863857374392,
                cr = 12.0,
                cpa = 39.0,
                cpc = 4.68,
                cpm = 189.63,
            )
        )

        val videoStats = uacContentStatisticsWithMobileData.first { it.id == videoAssetId }
        videoStats.creativeStatistics.checkEquals(
            createCreativeStatistics(
                conversions = 6,
                costMicros = 4000000,
                costCurMicros = 5000000,
                costTaxFreeMicros = 234000000,
                installs = 7,
                postViewConversions = 8,
                postViewInstalls = 9,
                cpa = 39.0,
            )
        )
        val expectedIds = uacCampaignContents.map { it.id }.toSet()
        verify(assetStatRepositoryMock).getStatsByAssetIds(eq(expectedIds), any(), any(), any())
    }

    @Test
    fun `get content statistics mobile no data from repository`() {
        setAssetStatsResponse(emptyList())
        val result = doContentStatisticsRequest()

        val uacContentStatisticsResponse = fromJson<UacContentStatisticsResponse>(result)
        val uacContentStatisticsWithMobileData = uacContentStatisticsResponse.results.filter {
            it.creativeStatistics != null
        }
        uacContentStatisticsWithMobileData.checkNotEmpty()
        uacContentStatisticsWithMobileData.forEach {
            it.creativeStatistics.checkEquals(EMPTY_STAT)
        }

        val expectedIds = uacCampaignContents.map { it.id }.toSet()
        verify(assetStatRepositoryMock).getStatsByAssetIds(eq(expectedIds), any(), any(), any())
    }

    @Test
    fun `get content statistics mobile filter by media type`() {
        setAssetStatsResponse(
            listOf(
                buildAssetsStat(imageAssetId, MediaType.IMAGE),
            )
        )
        val result = doContentStatisticsRequest(mediaType = MediaType.IMAGE)

        val uacContentStatisticsResponse = fromJson<UacContentStatisticsResponse>(result)
        val uacContentStatisticsWithMobileData = uacContentStatisticsResponse.results.filter {
            it.creativeStatistics != EMPTY_STAT
        }
        uacContentStatisticsWithMobileData.checkSize(1)

        uacContentStatisticsWithMobileData[0].creativeStatistics.checkEquals(
            createCreativeStatistics(
                shows = 1234,
                clicks = 50,
                conversions = 6,
                costMicros = 4000000,
                costCurMicros = 5000000,
                costTaxFreeMicros = 234000000,
                installs = 7,
                postViewConversions = 8,
                postViewInstalls = 9,
                ctr = 4.051863857374392,
                cr = 12.0,
                cpa = 39.0,
                cpc = 4.68,
                cpm = 189.63,
            )
        )
        val expectedIds = uacCampaignContents
            .filter { it.type == MediaType.IMAGE }
            .map { it.id }
            .toSet()
        verify(assetStatRepositoryMock).getStatsByAssetIds(eq(expectedIds), any(), any(), any())
    }

    @Test
    fun `sort by shows test`() {
        setAssetStatsResponse(
            listOf(
                buildAssetsStat(imageAssetId, MediaType.IMAGE, shows = 55),
                buildAssetsStat(imageAssetId2, MediaType.IMAGE, shows = 11),
                buildAssetsStat(imageAssetId3, MediaType.IMAGE, shows = 25),
            )
        )
        val result = doContentStatisticsRequest(mediaType = MediaType.IMAGE, sortBy = "shows", orderBy = "asc")

        val uacContentStatisticsResponse = fromJson<UacContentStatisticsResponse>(result)
        val uacContentStatisticsWithMobileData = uacContentStatisticsResponse.results.filter {
            it.creativeStatistics != EMPTY_STAT
        }

        uacContentStatisticsWithMobileData.checkSize(3)
        uacContentStatisticsWithMobileData.map { it.id }.checkContains(imageAssetId2, imageAssetId3, imageAssetId)
        uacContentStatisticsWithMobileData.map { it.creativeStatistics!!.shows }.checkContains(11, 25, 55)
    }

    @Test
    fun `sort by shows desc test`() {
        setAssetStatsResponse(
            listOf(
                buildAssetsStat(imageAssetId, MediaType.IMAGE, shows = 55),
                buildAssetsStat(imageAssetId2, MediaType.IMAGE, shows = 11),
                buildAssetsStat(imageAssetId3, MediaType.IMAGE, shows = 25),
            )
        )
        val result = doContentStatisticsRequest(mediaType = MediaType.IMAGE, sortBy = "shows", orderBy = "desc")

        val uacContentStatisticsResponse = fromJson<UacContentStatisticsResponse>(result)
        val uacContentStatisticsWithMobileData = uacContentStatisticsResponse.results.filter {
            it.creativeStatistics != EMPTY_STAT
        }

        uacContentStatisticsWithMobileData.checkSize(3)
        uacContentStatisticsWithMobileData.map { it.id }.checkContains(imageAssetId, imageAssetId3, imageAssetId2)
        uacContentStatisticsWithMobileData.map { it.creativeStatistics!!.shows }.checkContains(55, 25, 11)
    }

    @Test
    fun `sort by ctr test`() {
        setAssetStatsResponse(
            listOf(
                buildAssetsStat(imageAssetId, MediaType.IMAGE, clicks = 55, shows = 100),
                buildAssetsStat(imageAssetId2, MediaType.IMAGE, clicks = 11, shows = 100),
                buildAssetsStat(imageAssetId3, MediaType.IMAGE, clicks = 25, shows = 100),
            )
        )
        val result = doContentStatisticsRequest(mediaType = MediaType.IMAGE, sortBy = "ctr", orderBy = "asc")

        val uacContentStatisticsResponse = fromJson<UacContentStatisticsResponse>(result)
        val uacContentStatisticsWithMobileData = uacContentStatisticsResponse.results.filter {
            it.creativeStatistics != EMPTY_STAT
        }

        uacContentStatisticsWithMobileData.checkSize(3)
        uacContentStatisticsWithMobileData.map { it.id }.checkContains(imageAssetId2, imageAssetId3, imageAssetId)
        uacContentStatisticsWithMobileData.map { it.creativeStatistics!!.ctr }.checkContains(11.0, 25.0, 55.0)
    }
}
