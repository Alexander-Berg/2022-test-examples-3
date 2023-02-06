package ru.yandex.market.replenishment.autoorder.service.tender

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.dto.AxCreatePurchasePriceDto
import ru.yandex.market.replenishment.autoorder.exception.BadRequestException
import ru.yandex.market.replenishment.autoorder.model.AxHandlerResponse
import ru.yandex.market.replenishment.autoorder.model.TenderStatus
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.utils.AxHandlerResponseUtils.getMockedResponseEntity
import java.time.LocalDateTime

@DbUnitDataBaseConfig(DbUnitDataBaseConfig.Entry(name = "tableType", value = "TABLE,MATERIALIZED VIEW"))
@WithMockLogin
class TenderServiceAxaptaTest : FunctionalTest() {

    companion object {
        val MOCKED_DATE_TIME: LocalDateTime = LocalDateTime.of(2020, 9, 6, 8, 0)
    }

    @Value("\${ax.handlers}")
    private lateinit var axaptaServerUrl: String

    @Autowired
    @Qualifier("axRestTemplate")
    private lateinit var axRestTemplate: RestTemplate

    @Autowired
    private lateinit var tenderService: TenderService

    @Before
    fun mockDateTime() {
        setTestTime(MOCKED_DATE_TIME)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_isOk.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_isOk.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_isOk() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity("123")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs5"
            },
            eq(AxHandlerResponse::class.java)
        )

        doReturn(getMockedResponseEntity("456")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs6"
            },
            eq(AxHandlerResponse::class.java)
        )

        doReturn(getMockedResponseEntity("789")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs1"
            },
            eq(AxHandlerResponse::class.java)
        )
        tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_throwsException.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_axReturnError_throwsException() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity("123")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs5"
            },
            eq(AxHandlerResponse::class.java)
        )

        doReturn(getMockedResponseEntity(null, "Test axapta error"))
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs6"
                },
                eq(AxHandlerResponse::class.java)
            )
        val exception = assertThrows<BadRequestException> {
            tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
        }
        assertEquals(
            "Errors occurred when price specifications sending to axapta for tender with id 1:\n" +
                "For supplier with rs_id rs6: Test axapta error",
            exception.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_throwsException.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_axReturnSeveralErrors_throwsOneException() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity(null, "Test axapta error 1"))
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs5"
                },
                eq(AxHandlerResponse::class.java)
            )

        doReturn(getMockedResponseEntity(null, "Test axapta error 2"))
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs6"
                },
                eq(AxHandlerResponse::class.java)
            )
        val exception = assertThrows<BadRequestException> {
            tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
        }
        val lines = exception.message?.split("\n") ?: emptyList()
        Assertions.assertThat(lines).containsExactlyInAnyOrder(
            "Errors occurred when price specifications sending to axapta for tender with id 1:",
            "For supplier with rs_id rs5: Test axapta error 1",
            "For supplier with rs_id rs6: Test axapta error 2"
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_throwsException.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_httpClientThrowsResourceAccessException_throwsException() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity("123")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs5"
            },
            eq(AxHandlerResponse::class.java)
        )

        doThrow(ResourceAccessException::class)
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs6"
                },
                eq(AxHandlerResponse::class.java)
            )
        val exception = assertThrows<BadRequestException> {
            tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
        }
        assertEquals(
            "Errors occurred when price specifications sending to axapta for tender with id 1:\n" +
                "For supplier with rs_id rs6: ru.yandex.market.replenishment.autoorder.exception.AxHandlerCreatePriceSpecError",
            exception.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_throwsException.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_httpClientThrowsNPE_throwsBadRequest() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity("123")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs5"
            },
            eq(AxHandlerResponse::class.java)
        )

        doThrow(NullPointerException::class)
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs6"
                },
                eq(AxHandlerResponse::class.java)
            )
        val exception = assertThrows<BadRequestException> {
            tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
        }
        assertEquals(
            "Errors occurred when price specifications sending to axapta for tender with id 1:\n" +
                "For supplier with rs_id rs6: java.lang.NullPointerException",
            exception.message
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_throwsException.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_severalErrors_throwsOneException() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity(null, "Test axapta error 1"))
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs5"
                },
                eq(AxHandlerResponse::class.java)
            )

        doThrow(NullPointerException::class)
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs6"
                },
                eq(AxHandlerResponse::class.java)
            )
        val exception = assertThrows<BadRequestException> {
            tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
        }
        val lines = exception.message?.split("\n") ?: emptyList()
        Assertions.assertThat(lines).containsExactlyInAnyOrder(
            "Errors occurred when price specifications sending to axapta for tender with id 1:",
            "For supplier with rs_id rs5: Test axapta error 1",
            "For supplier with rs_id rs6: java.lang.NullPointerException"
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_noAgreement.before.csv"],
        after = ["TenderServiceAxaptaTest.testSetTenderStatusWinnerFixed_throwsException.after.csv"]
    )
    fun testSetTenderStatusWinnerFixed_noAgreement() {
        val createPurchPriceUrl = axaptaServerUrl + "create-purch-price"
        doReturn(getMockedResponseEntity("123")).`when`(axRestTemplate).exchange(
            eq(createPurchPriceUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                request.body?.rsId == "rs5"
            },
            eq(AxHandlerResponse::class.java)
        )

        doReturn(getMockedResponseEntity(null, "Test axapta error"))
            .`when`(axRestTemplate).exchange(
                eq(createPurchPriceUrl),
                eq(HttpMethod.POST),
                ArgumentMatchers.argThat { request: HttpEntity<AxCreatePurchasePriceDto> ->
                    request.body?.rsId == "rs6"
                },
                eq(AxHandlerResponse::class.java)
            )
        val exception = assertThrows<BadRequestException> {
            tenderService.setStatus(1L, TenderStatus.WINNER_FIXED)
        }
        assertEquals(
            "AgreementId was not found for demandId=1 and rs_id=rs6",
            exception.message
        )
    }
}
