package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import java.time.Instant

 class FfReturnPreparingSenderAggregatedProcessorTest: StartrekProcessorTest() {
    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-12-20T10:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

     @DisplayName("Создание тикета для одного просроченного план-факта приемки ЦТЭ - ФФ")
     @Test
     @DatabaseSetup("/service/processor/qualityrule/ff_return_preparing_sender/before/setup_single_group.xml")
     @ExpectedDatabase(
         value = "/service/processor/qualityrule/ff_return_preparing_sender/after/create_single.xml",
         assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
     )
     fun createIssueTest() {
         whenever(issues.create(ArgumentMatchers.any()))
             .thenReturn(
                 Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null)
             )

         handleGroups()

         val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
         verify(issues).create(captor.capture())
         val issueValues = captor.value.values

         assertSoftly {
             issueValues.getOrThrow("summary") shouldBe "[MQM] 20-12-2021: TestPartner1 вовремя не принял заказы на вторичной приемке."
             issueValues.getOrThrow("description") shouldBe
                 """
                https://abo.market.yandex-team.ru/order/111
                https://ow.market.yandex-team.ru/order/111
                https://lms-admin.market.yandex-team.ru/lom/orders/1
                Дата создания заказа: 21-12-2021
                Дедлайн вторичной приемки: 21-12-2021 20:41
                Трек ФФ: 104
                """.trimIndent()
             issueValues.getOrThrow("defectOrders") shouldBe 1
             issueValues.getOrThrow("components") shouldBe listOf(106500L)
             issueValues.getOrThrow("customerOrderNumber") shouldBe "111"
             issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
             (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder listOf("TestPartner1:ДШ")
         }
     }

     @DisplayName("Создание тикета для группы просроченных план-фактов приемки на ЦТЭ - ФФ")
     @Test
     @DatabaseSetup("/service/processor/qualityrule/ff_return_preparing_sender/before/setup_aggregated_group.xml")
     @ExpectedDatabase(
         value = "/service/processor/qualityrule/ff_return_preparing_sender/after/create_aggregated.xml",
         assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
     )
     fun createAggregatedIssueTest() {
         val attachment = Mockito.mock(Attachment::class.java)
         whenever(issues.create(ArgumentMatchers.any()))
             .thenReturn(Issue(null, null, "MONITORINGSNDBX-1", null, 1, EmptyMap(), null))
         whenever(attachments.upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(attachment)

         handleGroups()

         val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
         verify(issues).create(captor.capture())
         val issueValues = captor.value.values

         verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())

         assertSoftly {
             issueValues.getOrThrow("summary") shouldBe "[MQM] 20-12-2021: TestPartner1 вовремя не принял заказы на вторичной приемке."
             issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (кол-во заказов: 2)"
             issueValues.getOrThrow("defectOrders") shouldBe 2
             issueValues.getOrThrow("customerOrderNumber") shouldBe "111, 222"
             issueValues.getOrThrow("queue") shouldBe "MONITORINGSNDBX"
             (issueValues.getOrThrow("tags") as Array<*>).toList() shouldContainExactlyInAnyOrder listOf("TestPartner1:ДШ")
         }
     }
 }
