package ru.yandex.market.abo.tms.hiding.rules.blue

import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.ytree.`object`.YTreeSerializer
import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason
import ru.yandex.market.abo.core.export.hidden.HiddenOffersManager
import ru.yandex.market.abo.core.export.hidden.snapshot.white.HiddenOfferSnapshotService
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRuleService
import ru.yandex.market.abo.core.shop.org.ShopOrgService
import ru.yandex.market.abo.core.yt.YtService
import ru.yandex.market.abo.cpa.cart_diff.CartDiffService
import ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff
import ru.yandex.market.abo.cpa.cart_diff.diff.logfields.LogOfferInfo
import ru.yandex.market.abo.logbroker.hidings.DatacampMessageForHidingsConverter
import ru.yandex.market.abo.tms.hiding.rules.AbstractYtUploader

class BlueOfferHidingRuleYtUploaderTest {
    private val cartDiffService: CartDiffService = mock()
    private val ytServiceArnold: YtService = mock()
    private val ytServiceHahn: YtService = mock()
    private val blueOfferHidingRuleService: BlueOfferHidingRuleService = mock()
    private val shopOrgService: ShopOrgService = mock()
    private val hiddenOffersManager: HiddenOffersManager = mock()
    private val hiddenOfferSnapshotService: HiddenOfferSnapshotService = mock()
    private val datacampMessageForHidingsConverter: DatacampMessageForHidingsConverter = mock()
    private val uploader = BlueOfferHidingRuleYtUploader(
        cartDiffService,
        blueOfferHidingRuleService,
        hiddenOffersManager,
        hiddenOfferSnapshotService,
        datacampMessageForHidingsConverter,
        listOf(ytServiceArnold, ytServiceHahn),
        shopOrgService,
        "//root/dir"
    )

    @Test
    fun testUpload() {
        val hidingRules = listOf(
            BlueOfferHidingRule.fromCartDiff(1L, "1", 1L, BlueOfferHidingReason.OTHER),
            BlueOfferHidingRule.fromOrderItem(2L, 2L, "2", BlueOfferHidingReason.MISSING_ITEM)
        )

        whenever(blueOfferHidingRuleService.getActualOrDeletedAfter(any())).thenReturn(hidingRules)

        val actualCartDiff = CartDiff().apply {
            status = CartDiffStatus.APPROVED
            cartId = 3L
            shopId = 3L
            logOfferInfo = LogOfferInfo("", 0L, "", 3L, "3", "", null)
            diffDate = Date()
        }
        val removedCartDiff = CartDiff().apply {
            status = CartDiffStatus.CANCELLED
            cartId = 4L
            shopId = 4L
            logOfferInfo = LogOfferInfo("", 0L, "", 4L, "4", "", null)
            diffDate = Date()
        }
        val cartDiffs = listOf(actualCartDiff, removedCartDiff)

        whenever(cartDiffService.getByColorAndModifiedAfterDate(any(), any())).thenReturn(cartDiffs)

        val expected = hidingRules.asSequence()
            .plus(cartDiffs.map { BlueOfferHidingRule.fromCartDiff(it) })
            .map { uploader.convertToYtRow(it) }
            .toList()

        uploader.uploadActualHidings()

        argumentCaptor<Iterable<BlueOfferHidingRuleYtRow>>().apply {
            verify(ytServiceArnold).writeTable(any(), any<YTreeSerializer<BlueOfferHidingRuleYtRow>>(), capture())
            assertEquals(expected, allValues.flatten())
        }

        argumentCaptor<Iterable<BlueOfferHidingRuleYtRow>>().apply {
            verify(ytServiceHahn).writeTable(any(), any<YTreeSerializer<BlueOfferHidingRuleYtRow>>(), capture())
            assertEquals(expected, allValues.flatten())
        }
    }

    @Test
    fun oneServiceIsDisabled() {
        ytServiceArnold.stub {
            on {
                writeTable(
                    any(),
                    any<YTreeSerializer<BlueOfferHidingRuleYtRow>>(),
                    any<Iterable<BlueOfferHidingRuleYtRow>>()
                )
            } doThrow RuntimeException("Service is down")
        }

        val ex = assertThrows<RuntimeException> { uploader.uploadActualHidings() }
        assertEquals("Service is down", ex.message)
        verify(ytServiceHahn).writeTable(any(), any<YTreeSerializer<Any>>(), any<Iterable<BlueOfferHidingRuleYtRow>>())
    }

    @Test
    fun rootPathCreatesIfNotExists() {
        ytServiceArnold.stub {
            on { exists(any()) } doReturn false
        }
        uploader.uploadActualHidings()
        verify(ytServiceArnold)
            .writeTable(any(), any<YTreeSerializer<Any>>(), any<Iterable<BlueOfferHidingRuleYtRow>>())
    }

    @Test
    fun tableAlreadyExists() {
        val tableName = "2021-03-10T14:00:00"

        ytServiceArnold.stub {
            on { ytServiceArnold.exists(any()) } doReturn true
            on { ytServiceArnold.list(any()) } doReturn listOf(tableName)
        }

        uploader.uploadHidings(YPath.simple("//root/dir/$tableName"), emptyList(), ytServiceArnold)
        verify(ytServiceArnold, never())
            .writeTable(any(), any<YTreeSerializer<Any>>(), any<Iterable<BlueOfferHidingRuleYtRow>>())
    }

    @Test
    fun checkTablesDeletion() {
        whenever(ytServiceArnold.list(any())).thenReturn(
            listOf(
                "2021-03-10T14:00:00",
                "2021-03-10T14:05:00",
                "2021-03-10T14:10:00",
                "2021-03-10T14:15:00",
                "2021-03-10T14:20:00",
                "2021-03-10T14:25:00"
            )
        )

        val yPathCaptor = argumentCaptor<YPath>()
        uploader.deleteOldestTables(ytServiceArnold)
        verify(ytServiceArnold, times(1)).remove(yPathCaptor.capture())
        assertEquals("2021-03-10T14:00:00", yPathCaptor.firstValue.name())
    }

    @ParameterizedTest
    @CsvSource(
        "2021-03-10T14:00:00, 2021-03-10T14:00:00",
        "2021-03-10T14:00:01, 2021-03-10T14:00:00",
        "2021-03-10T14:04:59, 2021-03-10T14:00:00",
        "2021-03-10T14:05:00, 2021-03-10T14:05:00",
        "2021-03-10T14:05:01, 2021-03-10T14:05:00",
        "2021-03-10T14:09:59, 2021-03-10T14:05:00"
    )
    fun truncateToFiveMinTest(inputTime: String, expected: String) {
        val dateTime = LocalDateTime.parse(inputTime, AbstractYtUploader.FORMATTER)
        assertEquals(expected, AbstractYtUploader.FORMATTER.format(AbstractYtUploader.truncateToFiveMin(dateTime)))
    }
}
