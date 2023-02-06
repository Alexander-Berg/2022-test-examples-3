package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.impl.DefaultIteratorF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group.CreateIssueFlow
import ru.yandex.market.logistics.mqm.utils.clearCache
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.Relationship
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.SearchRequest
import java.time.Instant

@DisplayName("Тесты обработчика заказов без КИЗов")
class OrderWithoutRequiredCISQualityRuleProcessorTest : StartrekProcessorTest() {

    @Autowired
    @Qualifier("mbiApiClientLogged")
    lateinit var mbiApiClient: MbiApiClient

    @Autowired
    @Qualifier("caffeineCacheManager")
    lateinit var caffeineCacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        clearCache(caffeineCacheManager)
        clock.setFixed(Instant.parse("2021-03-01T19:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта отгрузки")
    @DatabaseSetup("/service/processor/qualityrule/before/order_without_cis/create_ticket_with_one_planfact.xml")
    fun createSingleIssueTest() {
        whenever(mbiApiClient.getPartnerSuperAdmin(1))
            .thenReturn(BusinessOwnerDTO(1, 2, "test", setOf("test@mail.com")))

        mockAlreadyCreatedIssues(listOf(
            Issue(null, null, "MQMDELIVEREDSYN-222", null, 1, EmptyMap(), null)
        ))

        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MQMDELIVEREDSYN-1", null, 1, EmptyMap(), null))

        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values
        val link = issueCreate.links.first()

        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] Партнер Партнер (shopId: 1) отгрузил заказы с карготипом CIS_REQUIRED без указания КИЗов."
        )
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                "https://partner.market.yandex.ru/supplier/1",
                "",
                "В заказе 777 в одном из Item'ов отсутствуют требуемые КИЗы."
            ).joinToString("\n")
        )
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MQMDELIVEREDSYN")
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(116833L)
        softly.assertThat(link.issue.get()).isEqualTo("MQMDELIVEREDSYN-222")
        softly.assertThat(link.relationship).isEqualTo(Relationship.RELATES.name.lowercase())
        softly.assertThat(values.getOrThrow("tags") as Array<*>).containsExactly("partner 987654321 send without cis")
        softly.assertThat(values.getOrThrow(CreateIssueFlow.FIELD_SHOP_ID)).isEqualTo("1")
        softly.assertThat(values.getOrThrow(CreateIssueFlow.FIELD_CUSTOMER_EMAIL)).isEqualTo("test@mail.com")

        verify(issues).find(refEq(
            SearchRequest.builder()
                .filter("tags", "partner 987654321 send without cis")
                .build()
        ))
         verify(mbiApiClient).getPartnerSuperAdmin(1)
    }

    @Test
    @DisplayName("Создание тикета в Startrek для просроченных план-фактов отгрузки")
    @DatabaseSetup("/service/processor/qualityrule/before/order_without_cis/create_ticket_with_some_planfacts.xml")
    fun createAggregatedIssueTest() {
        mockAlreadyCreatedIssues(listOf())

        val attachment = mock<Attachment>()

        whenever(issues.create(any()))
            .thenReturn(Issue(null, null, "MQMDELIVEREDSYN-1", null, 1, EmptyMap(), null))

        whenever(attachments.upload(any<String>(), any())).thenReturn(attachment)

        handleGroups()
        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.lastValue
        val values = issueCreate.values

        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] Партнер Партнер (shopId: 1) отгрузил заказы с карготипом CIS_REQUIRED без указания КИЗов."
        )
        softly.assertThat(values.getOrThrow("description")).isEqualTo(
            listOf(
                "https://partner.market.yandex.ru/supplier/1",
                "Список заказов в приложении (2 шт.)"
            ).joinToString("\n")
        )
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MQMDELIVEREDSYN")
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(2)
        softly.assertThat(values.getOrThrow("tags") as Array<*>).containsExactly("partner 987654321 send without cis")
        softly.assertThat(values.getOrThrow("components") as LongArray).containsExactly(116833L)
        softly.assertThat(values.getOrThrow(CreateIssueFlow.FIELD_SHOP_ID)).isEqualTo("1")
        softly.assertThat(values.getOrThrow(CreateIssueFlow.FIELD_CUSTOMER_EMAIL)).isEqualTo("test@mail.com")

        verify(attachments).upload(any<String>(), any())
        verify(issues).find(refEq(
            SearchRequest.builder()
                .filter("tags", "partner 987654321 send without cis")
                .build()
        ))
    }

    @Test
    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов отгрузки")
    @DatabaseSetup("/service/processor/qualityrule/before/order_without_cis/comment_with_some_planfacts.xml")
    fun commentAggregatedIssueTest() {
        val attachment = mock<Attachment>()
        whenever(attachments.upload(any<String>(), any())).thenReturn(attachment)

        handleGroups()
        val captor = argumentCaptor<IssueUpdate>()
        verify(issues).update(eq("MONITORINGSNDBX-1"), captor.capture())

        val issueUpdate = captor.lastValue
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        softly.assertThat(commentString).isEqualTo(
            listOf(
                "Информация в тикете была автоматически изменена.",
                "",
                "Добавлены новые заказы (1 шт.): 779.",
                "Список заказов в приложении (3 шт.)."
            ).joinToString("\n")
        )
        softly.assertThat((values.getOrThrow("customerOrderNumber") as ScalarUpdate<*>).set.get())
            .isEqualTo("777, 778, 779")
        softly.assertThat((values.getOrThrow("defectOrders") as ScalarUpdate<*>).set.get()).isEqualTo(3)
        verify(attachments).upload(any<String>(), any())
    }

    fun mockAlreadyCreatedIssues(issuesList: List<Issue>) {
        whenever(issues.find(refEq(
            SearchRequest.builder()
                .filter("tags", "partner 987654321 send without cis")
                .build()
        ))).thenReturn(DefaultIteratorF.wrap(issuesList.iterator()))
    }
}
