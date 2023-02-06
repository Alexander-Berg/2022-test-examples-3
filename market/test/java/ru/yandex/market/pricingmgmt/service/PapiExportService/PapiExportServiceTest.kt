package ru.yandex.market.pricingmgmt.service.PapiExportService

import org.apache.commons.httpclient.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.TestUtils
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.repository.postgres.PriceSelectionRepository
import ru.yandex.market.pricingmgmt.service.EnvironmentService
import ru.yandex.market.pricingmgmt.service.PapiExportService
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.configurers.PapiConfigurer
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PapiExportServiceTest : ControllerTest() {

    @Autowired
    private lateinit var papiExportService: PapiExportService

    @Autowired
    private lateinit var environmentService: EnvironmentService

    @Autowired
    private lateinit var priceSelectionRepository: PriceSelectionRepository

    @Autowired
    private lateinit var papiConfigurer: PapiConfigurer

    @MockBean
    private var timeService: TimeService? = null

    companion object {
        private val PAPI_SUCCESS = "<response><status>OK</status></response>"
        private val PAPI_ERROR = "<response><status>ERROR</status></response>"
    }

    private var lastUploaded = OffsetDateTime.of(2022, 3, 20, 0, 0, 0, 0, ZoneOffset.UTC)

    @BeforeEach
    fun init() {
        Mockito.`when`(timeService!!.getNowDateTime())
            .thenReturn(LocalDateTime.of(2022, 3, 20, 12, 34, 56))
    }

    @Test
    @DbUnitDataSet(before = ["PapiExportServiceTest.before.csv"])
    fun createXmlFrom_willCreateValidXml() {
        // смотрим какой XML будет создан для отправки

        val prices = priceSelectionRepository.selectPriceLogCreatedAfter(lastUploaded, true)

        val xml = papiExportService.createXmlFrom(prices, indent = true)
        val expected = TestUtils.readResourceFile("/papi/upload-offers.xml")

        assertEquals(expected, xml)
    }

    @Test
    @DbUnitDataSet(before = ["PapiExportServiceTest.before.zero_purch_price.csv"])
    fun createXmlFrom_zeroPurchPrice() {
        // в итоговом XML один из офферов будет без тега purchase-price

        val prices = priceSelectionRepository.selectPriceLogCreatedAfter(lastUploaded, true)

        val xml = papiExportService.createXmlFrom(prices, indent = true)
        val expected = TestUtils.readResourceFile("/papi/zero-purch-price.xml")

        assertEquals(expected, xml)
    }

    @Test
    @DbUnitDataSet(
        before = ["PapiExportServiceTest.before.csv"],
        after = ["PapiExportServiceTest.after.success.csv"]
    )
    fun uploadPricesToPapi_success() {
        // все успешно отправилось, в логе появилась запись об успехе

        papiConfigurer.mockPapiResponseForAxUpdatePrices(HttpStatus.SC_OK, PAPI_SUCCESS)
        papiExportService.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["PapiExportServiceTest.before.csv"],
        after = ["PapiExportServiceTest.after.error.csv"]
    )
    fun uploadPricesToPapi_error() {
        // PAPI ответило ошибкой, пишем про это в лог, помещаем ssku в таблицу ретраев

        papiConfigurer.mockPapiResponseForAxUpdatePrices(HttpStatus.SC_FORBIDDEN, PAPI_ERROR)
        papiExportService.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["PapiExportServiceTest.before.retry.csv"],
        after = ["PapiExportServiceTest.after.retry.success.csv"]
    )
    fun uploadPricesToPapi_RetrySuccess() {
        // все успешно отправилось, в логе появилась запись об успехе,
        // ретраи ушли

        papiConfigurer.mockPapiResponseForAxUpdatePrices(HttpStatus.SC_OK, PAPI_SUCCESS)
        papiExportService.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["PapiExportServiceTest.before.retry.csv"],
        after = ["PapiExportServiceTest.after.retry.error.csv"]
    )
    fun uploadPricesToPapi_RetryError() {
        // ошибка от PAPI, таблица ретраев пополнится

        papiConfigurer.mockPapiResponseForAxUpdatePrices(HttpStatus.SC_FORBIDDEN, PAPI_ERROR)
        papiExportService.run()
    }

    @Test
    @DbUnitDataSet(
        before = ["PapiExportServiceTest.before.retry.csv"],
        after = ["PapiExportServiceTest.after.retry.error_multiply.csv"]
    )
    fun uploadPricesToPapi_RetryErrorMultiply() {
        papiConfigurer.mockPapiResponseForAxUpdatePrices(HttpStatus.SC_FORBIDDEN, PAPI_ERROR)

        // 1 прогон
        papiExportService.run()

        // после 1 прогона в ретраях должны остаться записи за 2022-03-20 12:34:56.0 (34 минуты)

        // эмулируем, что прошла 1 минута с момента последнего запуска
        Mockito.`when`(timeService!!.getNowDateTime())
            .thenReturn(LocalDateTime.of(2022, 3, 20, 12, 35, 56))

        // заставляем пройти полный цикл отправки (а не только ретраи)
        environmentService.setDateTime(
            EnvironmentService.PRICES_LAST_UPLOADED_TO_PAPI,
            OffsetDateTime.of(2022, 3, 20, 16, 21, 35, 0, ZoneOffset.UTC)
        )

        // 2 прогон
        papiExportService.run()

        // в финале в ретраях должны остаться записи за 2022-03-20 12:35:56.0 (35 минут),
        // при этом дублей по ssku остаться не должно
    }
}
