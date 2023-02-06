package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyLong
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseFlow.FIELD_CUSTOMER_EMAIL
import ru.yandex.market.logistics.mqm.service.yt.PvzContactInformationCache
import ru.yandex.market.logistics.mqm.service.yt.dto.PvzContactInformation
import ru.yandex.startrek.client.model.IssueCreate

class FinalStatusAggregatedProcessorTest: StartrekProcessorTest() {

    @Autowired
    private lateinit var pvzContactInformationCache: PvzContactInformationCache

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-02T07:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета для одного просроченного план-факта финального статуса для курьерской доставки")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/final_status/create_single_courier.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/final_status/create_single_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssueCourierTest() {
        whenever(issues.create(any())).thenReturn(issue)
        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())
        val values = captor.firstValue.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe "[MQM] 01-11-2020: МК (Доставка курьером) – МК Какая-то" +
                " – не был вовремя получен финальный статус"
            values.getOrThrow("description") shouldBe
                """
                    https://abo.market.yandex-team.ru/order/777
                    https://ow.market.yandex-team.ru/order/777
                    https://lms-admin.market.yandex-team.ru/lom/orders/100111

                    Дата создания заказа: 01-11-2020
                    Дедлайн финального статуса: 01-11-2020 15:00:00

                    Трек СД: 111222333
                    Последний чекпоинт: 49



                    Адрес точки доставки: Россия Московская область Красногорск Светлая улица 3А 143409
            """.trimIndent()
            values.getOrThrow("components") shouldBe longArrayOf(88998)
            values.getOrThrow("tags") shouldBe arrayOf(
                "МК Какая-то",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "Лог. точка:10000010736",
            )
        }
    }

    @DisplayName(
        "Заказ YANDEX_GO. Создание тикета для одного просроченного " +
            "план-факта финального статуса для курьерской доставки"
    )
    @Test
    @DatabaseSetups(
        value = [
            DatabaseSetup("/service/processor/qualityrule/before/final_status/create_single_courier.xml"),
            DatabaseSetup(
                value = ["/service/processor/qualityrule/before/final_status/create_single_courier_go_order.xml"],
                type = DatabaseOperation.UPDATE
            )
        ]
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/final_status/create_single_courier_go_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun goOrderCreateSingleIssueCourierTest() {
        whenever(issues.create(any())).thenReturn(issue)
        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())
        val values = captor.firstValue.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe "[MQM][Доставка Наружу] 01-11-2020: " +
                "МК (Доставка курьером) – МК Какая-то – не был вовремя получен финальный статус"
            values.getOrThrow("description") shouldBe
                """
                    https://abo.market.yandex-team.ru/order/777

                    https://lms-admin.market.yandex-team.ru/lom/orders/100111

                    Дата создания заказа: 01-11-2020
                    Дедлайн финального статуса: 01-11-2020 15:00:00

                    Трек СД: 111222333
                    Последний чекпоинт: 49



                    Адрес точки доставки: Россия Московская область Красногорск Светлая улица 3А 143409
            """.trimIndent()
            values.getOrThrow("components") shouldBe longArrayOf(88998)
            values.getOrThrow("tags") shouldBe arrayOf(
                "yandex_go-доставка_наружу",
                "МК Какая-то",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "Лог. точка:10000010736",
            )
        }
    }

    @DisplayName("Создание тикета для одного просроченного план-факта финального статуса для доставки в пункт выдачи")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/final_status/create_single_pickup.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/final_status/create_single_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSingleIssuePickupTest() {
        whenever(issues.create(any())).thenReturn(issue)
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(anyLong()))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone12"))
        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())
        val values = captor.firstValue.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe "[MQM] 01-11-2020: Маркет ПВЗ (Самовывоз) – Какой-то ПВЗ" +
                " – МК Какая-то – не был вовремя получен финальный статус"
            values.getOrThrow("description") shouldBe
                """
                    https://abo.market.yandex-team.ru/order/777
                    https://ow.market.yandex-team.ru/order/777
                    https://lms-admin.market.yandex-team.ru/lom/orders/100111

                    Дата создания заказа: 01-11-2020
                    Дедлайн финального статуса: 01-11-2020 15:00:00

                    Трек СД: 111222333
                    Последний чекпоинт: 49
                    Email ПВЗ: email1@mail.com
                    Телефон ПВЗ: phone11
                    Телефон руководителя ПВЗ: phone12
                    Адрес точки доставки: Россия Московская область Красногорск Светлая улица 3А 143409
                    """.trimIndent()
            values.getOrThrow("components") shouldBe longArrayOf(88997)
            values.getOrThrow("tags") shouldBe arrayOf(
                "Какой-то ПВЗ",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "Лог. точка:10000010736",
            )
            values.getOrThrow(FIELD_CUSTOMER_EMAIL) shouldBe "email1@mail.com"
        }
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов финального статуса для доставки почтой")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/final_status/create_aggregated_post.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/final_status/create_aggregated_post.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAggregatedIssuePostTest() {
        whenever(issues.create(any())).thenReturn(issue)
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10000010736L)))
            .thenReturn(PvzContactInformation(1, "email1@mail.com", "phone11", "phone12"))
        whenever(pvzContactInformationCache.getPvzContactInformationByPvz(ArgumentMatchers.eq(10001687232L)))
            .thenReturn(PvzContactInformation(2, "email2@mail.com", "phone21", "phone22"))
        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())
        val values = captor.firstValue.values
        assertSoftly {
            values.getOrThrow("summary") shouldBe "[MQM] 01-11-2020: Контрактная доставка (Почта) – Какая-то почта" +
                " – не был вовремя получен финальный статус"
            values.getOrThrow("description") shouldBe "Список заказов в приложении (2 заказа)"
            values.getOrThrow("components") shouldBe longArrayOf(88995)
            values.getOrThrow("tags") shouldBe arrayOf(
                "Какая-то почта",
                "Адрес точки доставки:Россия Московская область Красногорск Светлая улица 3А 143409",
                "Лог. точка:10000010736",
            )
            values.getOrThrow(FIELD_CUSTOMER_EMAIL) shouldBe "email1@mail.com"
        }
    }
}
