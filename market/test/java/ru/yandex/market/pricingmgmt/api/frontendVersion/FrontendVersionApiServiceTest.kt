package ru.yandex.market.pricingmgmt.api.frontendVersion

import java.util.UUID

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.config.MockS3Config
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import ru.yandex.market.pricingmgmt.util.s3.S3ClientFactory
import ru.yandex.mj.generated.server.model.FrontendEnvironment
import ru.yandex.mj.generated.server.model.FrontendVersion

class FrontendVersionApiServiceTest : ControllerTest() {

    companion object {
        private const val HANDLE_URL = "/api/v1/frontend-version"

        private val MOCK_DATE_TIME = DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 11, 0, 0)

        private fun makeSummary(version: FrontendVersion): S3ObjectSummary = S3ObjectSummary().let { summary ->
            summary.lastModified = DateTimeTestingUtil.createDateFromOffsetDateTime(version.createdAt)
            summary.key = "${version.versionHash}/${UUID.randomUUID()}"
            summary
        }

        private fun makeListing(versions: Collection<FrontendVersion>) = ObjectListing().let { listing ->
            listing.objectSummaries.addAll(versions.map(::makeSummary))
            listing.isTruncated = false
            listing
        }
    }

    @Autowired
    private lateinit var s3ClientFactory: S3ClientFactory

    @MockBean
    private lateinit var timeService: TimeService

    private val s3Client: AmazonS3 get() = s3ClientFactory.s3Client

    @BeforeEach
    fun mockNowDateTime() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(MOCK_DATE_TIME)
    }

    @Test
    fun getAllVersionsTest() {
        val expectedResponseDto = listOf(
            FrontendVersion().versionHash("hash-125").environment(FrontendEnvironment.PRODUCTION)
                .createdAt(MOCK_DATE_TIME.plusYears(2)),
            FrontendVersion().versionHash("hash-123").environment(FrontendEnvironment.PRODUCTION)
                .createdAt(MOCK_DATE_TIME),
        )

        `when`(s3Client.listObjects(MockS3Config.bucketName)).thenReturn(makeListing(expectedResponseDto))

        mockMvc.perform(MockMvcRequestBuilders.get(HANDLE_URL).contentType("application/json"))
            .andDo { println(it.response.contentAsString) }
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponseDto)))
    }
}
