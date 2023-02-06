package ru.yandex.market.logistics.mqm.service.ytevents

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.secondValue
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.YtEvent
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.ytevents.reader.CLIENT_RETURN_BARCODE_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.EVENT_TIME_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.FAIL_REASON_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.ORDER_EXTERNAL_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.STATUS_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.SUBSTATUS_COLUMN
import ru.yandex.market.logistics.mqm.utils.createYtRow
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.util.function.Function

class CourierShiftValidateServiceImplTest: AbstractContextualTest() {

    @Autowired
    private lateinit var service: CourierShiftValidateService

    @Autowired
    private lateinit var ytService: YtService

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @BeforeEach
    private fun setUp() {
        val todData = listOf(
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "100",
                STATUS_COLUMN to "DELIVERED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to null
            ),
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "101",
                STATUS_COLUMN to "DELIVERY_FAILED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to "NO_CONTACT"
            ),
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "102",
                STATUS_COLUMN to "CANCELLED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to null
            ),
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "103",
                STATUS_COLUMN to "CANCELLED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to "COURIER_REASSIGNED"
            )
        )
        val sldData = listOf(
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "104",
                STATUS_COLUMN to "FINISHED",
                SUBSTATUS_COLUMN to "FINISHED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to null
            ),
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "105",
                STATUS_COLUMN to "DELIVERY_FAILED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to "NO_CONTACT"
            ),
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to null,
                STATUS_COLUMN to "FINISHED",
                SUBSTATUS_COLUMN to "FINISHED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to null,
                CLIENT_RETURN_BARCODE_COLUMN to "VOZVRAT_SF_PVZ_11",
            )
        )
        Mockito.doAnswer(createAnswer(sldData, todData))
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
    }

    @Test
    @DisplayName("Валидация без ошибок")
    @DatabaseSetup("/service/ytevents/courier/shiftvalidate/before/without_errors.xml")
    fun withoutErrors() {
        service.validateCourierShift(123)

        val queryCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(ytService, times(2)).selectRowsFromTable<Any>(
            queryCaptor.capture() ?: "",
            anyOrNull(),
            any()
        )

        val firstSearchedIds = queryCaptor.firstValue.extractSearchedIds()
        val secondSearchedIds = queryCaptor.secondValue.extractSearchedIds()
        assertSoftly {
            firstSearchedIds shouldContainAll listOf(1000, 1001, 1002, 1003)
            firstSearchedIds shouldNotContainAnyOf listOf(1004, 1005, 1006)
            secondSearchedIds shouldContainAll listOf(1004, 1005, 1006)
            secondSearchedIds shouldNotContainAnyOf listOf(1000, 1001, 1002, 1003)
            backLogCaptor.results.filter { it.contains("Found problems in courier shift") }.shouldBeEmpty()
        }
    }

    @Test
    @DisplayName("Валидация со всеми ошибками")
    @DatabaseSetup("/service/ytevents/courier/shiftvalidate/before/all_errors.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/courier/shiftvalidate/after/all_errors.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun allErrors() {
        service.validateCourierShift(123)

        val queryCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(ytService, times(2)).selectRowsFromTable<Any>(
            queryCaptor.capture() ?: "",
            anyOrNull(),
            any()
        )

        val firstSearchedIds = queryCaptor.firstValue.extractSearchedIds()
        val secondSearchedIds = queryCaptor.secondValue.extractSearchedIds()
        assertSoftly {
            firstSearchedIds shouldContainAll listOf(1000, 1001, 1002, 1003)
            firstSearchedIds shouldNotContainAnyOf listOf(1004, 1005, 1006)
            secondSearchedIds shouldContainAll listOf(1004, 1005, 1006)
            secondSearchedIds shouldNotContainAnyOf listOf(1000, 1001, 1002, 1003)
            backLogCaptor.results.filter { it.contains("Found problems in courier shift") }.shouldBeSingleton()
            backLogCaptor.results.filter { it.contains("Found problems in courier shift") }[0] shouldContain
                "Found problems in courier shift. \\n" +
                "Orders that should be returned to sc but not [101, 102, 105, VOZVRAT_SF_PVZ_11]. \\n" +
                "Orders that should be delivered but returned to sc [100, 104]."

        }
    }

    @Test
    @DisplayName("Валидация с несколькими таксами в по одному заказу")
    @DatabaseSetup("/service/ytevents/courier/shiftvalidate/before/with_repeats.xml")
    fun withRepeats() {
        val todData = listOf(
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "100",
                STATUS_COLUMN to "DELIVERED",
                EVENT_TIME_COLUMN to "2021-08-11T18:00:00.123456+03:00",
                FAIL_REASON_COLUMN to null
            ),
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "100",
                STATUS_COLUMN to "DELIVERY_FAILED",
                EVENT_TIME_COLUMN to "2021-08-11T16:00:00.123456+03:00",
                FAIL_REASON_COLUMN to "NO_CONTACT"
            )
        )
        val sldData = listOf(
            mapOf(
                ORDER_EXTERNAL_ID_COLUMN to "100",
                STATUS_COLUMN to "DELIVERY_FAILED",
                EVENT_TIME_COLUMN to "2021-08-11T17:00:00.123456+03:00",
                FAIL_REASON_COLUMN to "NO_CONTACT"
            )
        )
        Mockito.doAnswer(createAnswer(sldData, todData))
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())

        service.validateCourierShift(123)

        val queryCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(ytService, times(2)).selectRowsFromTable<Any>(
            queryCaptor.capture() ?: "",
            anyOrNull(),
            any()
        )

        assertSoftly {
            queryCaptor.firstValue.extractSearchedIds() shouldContainAll listOf(1000, 1001)
            queryCaptor.secondValue.extractSearchedIds() shouldContainAll listOf(1002)
            backLogCaptor.results.filter { it.contains("Found problems in courier shift") }.shouldBeEmpty()
        }
    }

    @Test
    @DisplayName("Валидация с пустым временем старта смены")
    @DatabaseSetup("/service/ytevents/courier/shiftvalidate/before/empty_start_time.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/courier/shiftvalidate/after/empty_start_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun emptyStartTime() {
        service.validateCourierShift(123)

        val queryCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(ytService, times(2)).selectRowsFromTable<Any>(
            queryCaptor.capture() ?: "",
            anyOrNull(),
            any()
        )

        val firstSearchedIds = queryCaptor.firstValue.extractSearchedIds()
        val secondSearchedIds = queryCaptor.secondValue.extractSearchedIds()
        assertSoftly {
            firstSearchedIds shouldContainAll listOf(1000, 1001, 1002, 1003)
            firstSearchedIds shouldNotContainAnyOf listOf(1004, 1005, 1006)
            secondSearchedIds shouldContainAll listOf(1004, 1005, 1006)
            secondSearchedIds shouldNotContainAnyOf listOf(1000, 1001, 1002, 1003)
            backLogCaptor.results.filter { it.contains("Found problems in courier shift") }.shouldBeSingleton()
            backLogCaptor.results.filter { it.contains("Found problems in courier shift") }[0] shouldContain
                "Found problems in courier shift. \\n" +
                "Orders that should be returned to sc but not [101, 102, 105, VOZVRAT_SF_PVZ_11]. \\n" +
                "Orders that should be delivered but returned to sc [100, 104]."
        }
    }

    private fun createAnswer(sldData: List<Map<String, Any?>>, todData: List<Map<String, Any?>>) =
        Answer { invocation ->
            val query = invocation.getArgument<String>(0)
            val argument = invocation.getArgument<Function<Iterator<YTreeMapNode>, List<YtEvent<*>>>>(2)
            val data = if (query.contains("AS tod")) todData
            else if (query.contains("AS sld")) sldData
            else throw IllegalStateException("Unknown query")
            val ytIterator = data.map { createYtRow(it) }.iterator()
            argument.apply(ytIterator)
        }

    private fun String.extractSearchedIds() =
        Regex(""" in \((.*)\)""")
            .find(this)
            ?.groupValues
            ?.get(1)
            ?.let { idsString ->
                Regex("""\d+""")
                    .findAll(idsString)
                    .map { it.value.toLong() }
                    .toList()
            }
            ?: throw IllegalAccessException("Can't find searched ids in string = '$this'")

}

