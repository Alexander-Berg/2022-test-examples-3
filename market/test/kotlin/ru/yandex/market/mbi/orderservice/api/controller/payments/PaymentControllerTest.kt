package ru.yandex.market.mbi.orderservice.api.controller.payments

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.api.assertErrorResponse
import ru.yandex.market.mbi.orderservice.common.model.yt.payments.PaymentHeader
import ru.yandex.market.mbi.orderservice.common.model.yt.payments.PaymentHeaderWithStatus
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.DefaultYtCrudRepository
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.common.util.DEFAULT_TIMEZONE
import ru.yandex.market.mbi.orderservice.model.PaymentOebsStatusDto
import ru.yandex.market.mbi.orderservice.model.PaymentsCountResponse
import ru.yandex.market.mbi.orderservice.model.PaymentsResponse
import ru.yandex.market.mbi.orderservice.model.PaymentsTotalResponse
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

/**
 * Тесты для [PaymentsController]
 */
class PaymentControllerTest : FunctionalTest() {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var readOnlyClient: YtClientProxySource

    @Autowired
    lateinit var readWriteClient: YtClientProxy

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

    @BeforeAll
    fun init() {
        val paymentsWithStatusRepository = DefaultYtCrudRepository(
            tableBindingHolder,
            PaymentHeaderWithStatus.Key::class.java,
            PaymentHeaderWithStatus::class.java,
            readWriteClient,
            readOnlyClient
        )

        val paymentsRepository = DefaultYtCrudRepository(
            tableBindingHolder,
            PaymentHeader.Key::class.java,
            PaymentHeader::class.java,
            readWriteClient,
            readOnlyClient
        )

        val paymentsWithStatus = this::class.loadTestEntities<PaymentHeaderWithStatus>(paymentsWithStatusResourceName)
            .sortedBy { it.key.paymentId }

        val payments = this::class.loadTestEntities<PaymentHeader>(paymentsResourceName)
            .sortedBy { it.paymentId }

        paymentsWithStatusRepository.insertRows(paymentsWithStatus, null)
        paymentsRepository.insertRows(payments, null)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getPaymentsSource")
    fun `verify get payments source`(
        description: String,
        verifier: (PaymentsResponse, PaymentsCountResponse, PaymentsTotalResponse) -> Unit,
        contractIds: List<String>,
        paymentId: Long?,
        paymentOebsStatuses: List<PaymentOebsStatusDto>?,
        page: Long?,
        size: Long?,
        dateTimeFrom: OffsetDateTime?,
        dateTimeTo: OffsetDateTime?
    ) {
        val paymentsResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/payments", contractIds, paymentId,
                paymentOebsStatuses, page, size, dateTimeFrom, dateTimeTo
            )
        )

        val paymentsCountResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/payments/count", contractIds, paymentId,
                paymentOebsStatuses, page, size, dateTimeFrom, dateTimeTo
            )
        )

        val paymentsTotalResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/payments/total", contractIds, paymentId,
                paymentOebsStatuses, page, size, dateTimeFrom, dateTimeTo
            )
        )

        val payments = objectMapper.readValue(
            paymentsResponse.entity.content,
            PaymentsResponse::class.java
        )

        val paymentsCount = objectMapper.readValue(
            paymentsCountResponse.entity.content,
            PaymentsCountResponse::class.java
        )

        val paymentsTotal = objectMapper.readValue(
            paymentsTotalResponse.entity.content,
            PaymentsTotalResponse::class.java
        )

        verifier(payments, paymentsCount, paymentsTotal)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getPaymentsUniqueSource")
    fun `verify get payments unique source`(
        description: String,
        verifier: (PaymentsResponse, PaymentsCountResponse, PaymentsTotalResponse) -> Unit,
        contractIds: List<String>,
        paymentId: Long?,
        paymentOebsStatuses: List<PaymentOebsStatusDto>?,
        page: Long?,
        size: Long?,
        dateTimeFrom: OffsetDateTime?,
        dateTimeTo: OffsetDateTime?
    ) {
        val paymentsResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/paymentsUnique", contractIds, paymentId,
                paymentOebsStatuses, page, size, dateTimeFrom, dateTimeTo
            )
        )

        val paymentsCountResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/paymentsUnique/count", contractIds, paymentId,
                paymentOebsStatuses, page, size, dateTimeFrom, dateTimeTo
            )
        )

        val paymentsTotalResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/paymentsUnique/total", contractIds, paymentId,
                paymentOebsStatuses, page, size, dateTimeFrom, dateTimeTo
            )
        )

        val payments = objectMapper.readValue(
            paymentsResponse.entity.content,
            PaymentsResponse::class.java
        )

        val paymentsCount = objectMapper.readValue(
            paymentsCountResponse.entity.content,
            PaymentsCountResponse::class.java
        )

        val paymentsTotal = objectMapper.readValue(
            paymentsTotalResponse.entity.content,
            PaymentsTotalResponse::class.java
        )

        verifier(payments, paymentsCount, paymentsTotal)
    }

    private fun prepareGetPaymentsRequest(
        path: String,
        contractIds: List<String>,
        paymentId: Any?,
        paymentOebsStatuses: List<PaymentOebsStatusDto>?,
        page: Long?,
        size: Long?,
        dateTimeFrom: OffsetDateTime?,
        dateTimeTo: OffsetDateTime?
    ): HttpGet {
        val uriBuilder = URIBuilder(baseUrl)
        uriBuilder.path = path

        for (contractId in contractIds) {
            uriBuilder.addParameter("contractIds", contractId)
        }

        if (paymentId != null) {
            uriBuilder.addParameter("paymentId", paymentId.toString())
        }

        if (paymentOebsStatuses != null) {
            for (paymentOebsStatus in paymentOebsStatuses) {
                uriBuilder.addParameter("paymentOebsStatuses", paymentOebsStatus.value)
            }
        }

        if (page != null && size != null) {
            uriBuilder.addParameter("page", page.toString())
            uriBuilder.addParameter("size", size.toString())
        }

        if (dateTimeFrom != null && dateTimeTo != null) {
            uriBuilder.addParameter("dateTimeFrom", dateTimeFrom.format(DateTimeFormatter.ISO_DATE_TIME))
            uriBuilder.addParameter("dateTimeTo", dateTimeTo.format(DateTimeFormatter.ISO_DATE_TIME))
        }

        val request = HttpGet(uriBuilder.build())
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    @Test
    fun `verify that parameter mismatch returns code 400`() {
        val paymentsResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                path = "/business/1/payments",
                contractIds = listOf("2"),
                paymentId = "Договор091234/12",
                null,
                null,
                null,
                null,
                null
            )
        )

        assertThat(paymentsResponse.statusLine.statusCode == 400)
        assertErrorResponse(
            paymentsResponse,
            // language=json
            """
                [
                   {
                     "code": "BAD_PARAM",
                     "message": "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; nested exception is java.lang.NumberFormatException: For input string: \"Договор091234/12\"",
                     "details": {
                        "reason": "paymentId"
                     }
                   }
                ]
            """.trimIndent()
        )
    }

    @Test
    fun `verify return 400 error with incorrect date`() {
        val paymentsResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/payments", listOf("2"), null, null, null, null,
                dateTimeFrom = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1502713068L), ZoneId.of(DEFAULT_TIMEZONE)),
                dateTimeTo = OffsetDateTime.ofInstant(Instant.ofEpochMilli(1502713060L), ZoneId.of(DEFAULT_TIMEZONE))
            )
        )

        assertThat(paymentsResponse.statusLine.statusCode == 400)
    }

    @Test
    fun `verify return 400 error with incorrect contractId`() {
        val paymentsResponse = HttpClientBuilder.create().build().execute(
            prepareGetPaymentsRequest(
                "/business/1/payments", listOf(), null, null, null, null, null, null
            )
        )

        assertThat(paymentsResponse.statusLine.statusCode == 400)
    }

    companion object {
        const val paymentsWithStatusResourceName = "payments_with_status.json"
        const val paymentsResourceName = "payments.json"

        @JvmStatic
        fun getPaymentsSource(): Stream<Arguments> {
            val listOfAllStatuses = PaymentOebsStatusDto.values().toList()

            return Stream.of(
                Arguments.of(
                    "Get payments for contracts", // description
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse, // verifier
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(2)

                            assertThat(payments[1].paymentId).isEqualTo(10)
                            assertThat(payments[1].contractId).isEqualTo("1")
                            assertThat(payments[1].paymentAmount).isEqualTo(
                                BigDecimal(100).divide(BigDecimal(100)).stripTrailingZeros()
                            )

                            assertThat(payments[0].paymentId).isEqualTo(30)
                            assertThat(payments[0].contractId).isEqualTo("3")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(300).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(400).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("1", "3"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contract",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(1)

                            assertThat(payments[0].paymentId).isEqualTo(30)
                            assertThat(payments[0].contractId).isEqualTo("3")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(300).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(1)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(300).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("3"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts by paymentId",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(1)

                            assertThat(payments[0].paymentId).isEqualTo(20L)
                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(1)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("2"), // contractIds
                    20L, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts with paging",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)
                            val payments = requireNotNull(result.payments)

                            assertThat(payments.size).isEqualTo(1)

                            assertThat(payments[0].paymentId).isEqualTo(20)
                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(410).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("2"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    1L,   // page
                    1L,   // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts by date",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(2)

                            assertThat(payments[0].paymentId).isEqualTo(21)
                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(210).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(410).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("2"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(1502713062L), ZoneId.of(DEFAULT_TIMEZONE)), // dateTimeFrom
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(1502713066L), ZoneId.of(DEFAULT_TIMEZONE)), // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts with status",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)
                            val payments = requireNotNull(result.payments)

                            assertThat(payments.size).isEqualTo(2)

                            assertThat(payments[1].contractId).isEqualTo("1")
                            assertThat(payments[1].paymentId).isEqualTo(10)
                            assertThat(payments[1].paymentAmount).isEqualTo(
                                BigDecimal(100).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                            assertThat(payments[1].paymentOebsStatusDto!!.value).isEqualTo("RECONCILED")
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(310).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("1", "2"), // contractIds
                    null, // paymentId
                    listOf(PaymentOebsStatusDto.RECONCILED), // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts with statuses",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)
                            val payments = requireNotNull(result.payments)

                            assertThat(payments.size).isEqualTo(3)

                            assertThat(payments[2].contractId).isEqualTo("1")
                            assertThat(payments[2].paymentId).isEqualTo(10)
                            assertThat(payments[2].paymentAmount).isEqualTo(
                                BigDecimal(100).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                            assertThat(payments[2].paymentOebsStatusDto!!.value).isEqualTo("RECONCILED")

                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentId).isEqualTo(21)
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(210).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                            assertThat(payments[0].paymentOebsStatusDto!!.value).isEqualTo("RECONCILED")
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(3)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(510).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("1", "2"), // contractIds
                    null, // paymentId
                    listOf(PaymentOebsStatusDto.RECONCILED, PaymentOebsStatusDto.CREATED), // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                )
            )
        }

        @JvmStatic
        fun getPaymentsUniqueSource(): Stream<Arguments> {
            val listOfAllStatuses = PaymentOebsStatusDto.values().toList()

            return Stream.of(
                Arguments.of(
                    "Get payments for contracts", // description
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse, // verifier
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(3)

                            assertThat(payments[2].paymentId).isEqualTo(10)
                            assertThat(payments[2].contractId).isEqualTo("1")
                            assertThat(payments[2].paymentAmount).isEqualTo(
                                BigDecimal(100).divide(BigDecimal(100)).stripTrailingZeros()
                            )

                            assertThat(payments[1].paymentId).isEqualTo(30)
                            assertThat(payments[1].contractId).isEqualTo("3")
                            assertThat(payments[1].paymentAmount).isEqualTo(
                                BigDecimal(300).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(3)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(710).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("1", "3"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contract",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(2)

                            assertThat(payments[0].paymentId).isEqualTo(31)
                            assertThat(payments[0].contractId).isEqualTo("3")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(310).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(610).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("3"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts by paymentId",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(1)

                            assertThat(payments[0].paymentId).isEqualTo(20L)
                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(1)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("2"), // contractIds
                    20L, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts with paging",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)
                            val payments = requireNotNull(result.payments)

                            assertThat(payments.size).isEqualTo(1)

                            assertThat(payments[0].paymentId).isEqualTo(20)
                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(1)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("2"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    0L,   // page
                    1L,   // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts by date",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)

                            val payments = requireNotNull(result.payments)
                            assertThat(payments.size).isEqualTo(1)

                            assertThat(payments[0].paymentId).isEqualTo(20)
                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(1)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("2"), // contractIds
                    null, // paymentId
                    listOfAllStatuses, // paymentOebsStatuses
                    null, // page
                    null, // size
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(1502713062L), ZoneId.of(DEFAULT_TIMEZONE)), // dateTimeFrom
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(1502713066L), ZoneId.of(DEFAULT_TIMEZONE)), // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts with status",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)
                            val payments = requireNotNull(result.payments)

                            assertThat(payments.size).isEqualTo(2)

                            assertThat(payments[1].contractId).isEqualTo("1")
                            assertThat(payments[1].paymentId).isEqualTo(10)
                            assertThat(payments[1].paymentAmount).isEqualTo(
                                BigDecimal(100).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                            assertThat(payments[1].paymentOebsStatusDto!!.value).isEqualTo("RECONCILED")
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(300).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("1", "2"), // contractIds
                    null, // paymentId
                    listOf(PaymentOebsStatusDto.RECONCILED), // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                ),
                Arguments.of(
                    "Get payments for contracts with statuses",
                    { response: PaymentsResponse, countResponse: PaymentsCountResponse,
                        totalResponse: PaymentsTotalResponse ->
                        assertThat(response).satisfies {
                            val result = requireNotNull(it.result)
                            val payments = requireNotNull(result.payments)

                            assertThat(payments.size).isEqualTo(2)

                            assertThat(payments[1].contractId).isEqualTo("1")
                            assertThat(payments[1].paymentId).isEqualTo(10)
                            assertThat(payments[1].paymentAmount).isEqualTo(
                                BigDecimal(100).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                            assertThat(payments[1].paymentOebsStatusDto!!.value).isEqualTo("RECONCILED")

                            assertThat(payments[0].contractId).isEqualTo("2")
                            assertThat(payments[0].paymentId).isEqualTo(20)
                            assertThat(payments[0].paymentAmount).isEqualTo(
                                BigDecimal(200).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                            assertThat(payments[0].paymentOebsStatusDto!!.value).isEqualTo("RECONCILED")
                        }

                        assertThat(countResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(2)
                        }

                        assertThat(totalResponse).satisfies {
                            val result = requireNotNull(it.result)
                            assertThat(result.total).isEqualTo(
                                BigDecimal(300).divide(BigDecimal(100)).stripTrailingZeros()
                            )
                        }
                    },
                    listOf("1", "2"), // contractIds
                    null, // paymentId
                    listOf(PaymentOebsStatusDto.RECONCILED, PaymentOebsStatusDto.CREATED), // paymentOebsStatuses
                    null, // page
                    null, // size
                    null, // dateTimeFrom
                    null  // dateTimeTo
                )
            )
        }
    }
}
