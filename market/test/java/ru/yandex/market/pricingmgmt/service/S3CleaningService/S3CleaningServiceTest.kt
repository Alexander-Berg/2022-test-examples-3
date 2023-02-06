package ru.yandex.market.pricingmgmt.service.S3CleaningService

import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.service.S3CleaningService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import ru.yandex.market.pricingmgmt.util.s3.S3ClientFactory
import java.time.LocalDateTime
import kotlin.collections.HashSet

class S3CleaningServiceTest : ControllerTest() {
    companion object {

        private fun makeSummaries(dates: Collection<LocalDateTime>): List<S3ObjectSummary> =
            dates.mapIndexed { i, date ->
                val summary = S3ObjectSummary()
                summary.lastModified = DateTimeTestingUtil.createDateFromLocalDateTime(date)
                summary.key = "${i + 1}/1"
                summary
            }

        private fun makeListing(summaries: Collection<S3ObjectSummary>) = ObjectListing().let { listing ->
            listing.objectSummaries.addAll(summaries)
            listing.isTruncated = false
            listing
        }

        private val dates = listOf(
            LocalDateTime.of(2022, 2, 13, 12, 0),
            LocalDateTime.of(2022, 2, 14, 12, 0),
            LocalDateTime.of(2022, 2, 15, 12, 0),
            LocalDateTime.of(2022, 2, 16, 12, 0),
            LocalDateTime.of(2022, 2, 17, 12, 0),
            LocalDateTime.of(2022, 2, 18, 12, 0),
            LocalDateTime.of(2022, 2, 19, 12, 0),
            LocalDateTime.of(2022, 2, 20, 12, 0),
            LocalDateTime.of(2022, 2, 21, 12, 0),
            LocalDateTime.of(2022, 2, 22, 12, 0),
            LocalDateTime.of(2022, 2, 23, 12, 0),
            LocalDateTime.of(2022, 2, 24, 12, 0),
        )
    }

    @Autowired
    private lateinit var s3CleaningService: S3CleaningService

    @Autowired
    private lateinit var s3ClientFactory: S3ClientFactory

    @Test
    fun testNoVersions() {
        testDeleted(emptyList(), emptySet())
    }

    @Test
    fun testDelete() {
        testDeleted(makeSummaries(dates), setOf("1/1", "2/1"))
    }

    @Test
    @DbUnitDataSet(before = ["S3CleaningServiceTest.hasForced.csv"])
    fun testWithForced() {
        testDeleted(makeSummaries(dates), setOf("2/1"))
    }

    private fun testDeleted(summaries: Collection<S3ObjectSummary>, expected: Collection<String>) {
        val mockListing = makeListing(summaries)

        `when`(s3ClientFactory.s3Client.listObjects(Mockito.anyString(), Mockito.anyString())).then { invocation ->
            val prefix = invocation.arguments[1].toString()
            println(prefix)
            makeListing(summaries.filter { it.key.startsWith(prefix, true) })
        }

        `when`(s3ClientFactory.s3Client.listObjects(Mockito.anyString())).thenReturn(mockListing)

        val deletedKeys = HashSet<String>()
        `when`(s3ClientFactory.s3Client.deleteObject(Mockito.anyString(), Mockito.anyString())).then { invocation ->
            deletedKeys.add(invocation.arguments[1].toString())
        }

        assertDoesNotThrow(s3CleaningService::run)

        val expectedKeys = expected.toHashSet()
        Assertions.assertEquals(expectedKeys.size, deletedKeys.size)

        val missingKeys = expectedKeys.minus(deletedKeys)
        Assertions.assertEquals(0, missingKeys.size)

        val redundantKeys = deletedKeys.minus(expectedKeys)
        Assertions.assertEquals(0, redundantKeys.size)
    }
}
