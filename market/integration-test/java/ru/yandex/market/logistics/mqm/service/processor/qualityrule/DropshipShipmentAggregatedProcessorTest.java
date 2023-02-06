package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseFlow;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Transition;
import ru.yandex.startrek.client.model.Update;
import ru.yandex.startrek.client.model.UserRef;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.logistics.mqm.utils.CacheUtilsKt.clearCache;

@DisplayName("Тесты процессора по созданию тикетов для отгрузок из ДШ")
public class DropshipShipmentAggregatedProcessorTest extends StartrekProcessorTest {

    private static final Long TEST_SHOP_ID = 1L;
    private static final Set<String> TEST_EMAILS = Set.of("test1@mail.com", "test2@mail.com");

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Autowired
    @Qualifier("mbiApiClientLogged")
    private MbiApiClient mbiApiClient;

    @BeforeEach
    public void setUp() {
        clearCache(caffeineCacheManager);
        clock.setFixed(Instant.parse("2020-11-05T20:00:00.00Z"), MOSCOW_ZONE);
    }

    @DisplayName("Создание тикета должно происходить на следующий день после создания группы")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-reschedule_next_day.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-reschedule_next_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void rescheduleOnTheNextDayTest() {
        clock.setFixed(Instant.parse("2020-11-01T15:00:00.00Z"), MOSCOW_ZONE);

        handleGroups();
    }

    @DisplayName("Создание тикета должно происходить на следующий не раньше указанного времени")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-reschedule_next_day.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-reschedule_next_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void rescheduleOnTheNextDayAtTheMorningTest() {
        clock.setFixed(Instant.parse("2020-11-02T06:59:00.00Z"), MOSCOW_ZONE);

        handleGroups();
    }

    @DisplayName("Группа должна быть запланирована на ближайшее время, пока тикет не закрыт")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-reschedule.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-reschedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void rescheduleIssueFlowTest() {
        clock.setFixed(Instant.parse("2020-11-02T20:00:00.00Z"), MOSCOW_ZONE);

        handleGroups();
    }

    @DisplayName("Создание тикета для одного просроченного план-факта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-create_single_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-create_single_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueFlowTest() {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] 01-11-2020: магазин Muzmart (id: 1) вовремя не отгрузил заказы");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "",
                    "Дата создания заказа: 01-11-2020",
                    "Дата отгрузки: 02-11-2020",
                    "Дедлайн отгрузки ДШ в СЦ СДТ,Климовск: 01-11-2020 15:00:00",
                    "",
                    "Трек ДШ: ws1",
                    "Трек СЦ СДТ,Климовск: ws2",
                    "Email главного представителя: test1@mail.com,test2@mail.com"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(((String[]) values.getOrThrow("tags")))
            .containsExactly("Muzmart:ДШ", "СДТ Климовск:СЦ");
        softly.assertThat(values.getOrThrow("idMagazina"))
            .isEqualTo(1L);
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL))
            .isEqualTo("test1@mail.com");

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID);
    }

    @DisplayName("Игнорирование группировки с неподходящим типом аггрегации (DATE_PARTNER)")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-create_single_ticket_date_partner.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-create_single_ticket_date_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void ignoreOldAggregation() {
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);

        handleGroups();
        verify(issues, never()).create(any());
    }

    @SuppressWarnings("unchecked")
    @DisplayName("Создание тикета для группы просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-create_aggregated_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-create_aggregated_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueFlowTest() {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        Attachment attachment = mock(Attachment.class);
        when(attachment.getId()).thenReturn("AT-123");
        when(attachments.upload(anyString(), any(InputStream.class))).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();

        ListF<String> listOfAttachments = issueCreate.getAttachments();
        softly.assertThat(listOfAttachments)
            .containsExactly("AT-123");

        MapF<String, Object> values = issueCreate.getValues();
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] 01-11-2020: магазин Muzmart (id: 1) вовремя не отгрузил заказы");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (2 заказа)");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 888");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        List<String> tags = (List<String>) values.getOrThrow("tags");
        softly.assertThat(tags)
            .containsExactly("Muzmart:ДШ", "СЦ СДТ Климовск:СЦ", "СЦ ПЭК БУТОВО:СЦ");
        softly.assertThat(values.getOrThrow("idMagazina"))
            .isEqualTo(1L);
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL))
            .isEqualTo("test1@mail.com");

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID);
    }

    @DisplayName("Обновление списка заказов в тикете")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-update_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-update_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateIssueFlowTest() {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        Attachment attachment = mock(Attachment.class);
        when(attachment.getId()).thenReturn("AT-123");
        when(attachments.upload(anyString(), any(InputStream.class))).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issues).update(any(String.class), captor.capture());
        IssueUpdate issueUpdate = captor.getValue();

        MapF<String, Update<?>> values = issueUpdate.getValues();
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("defectOrders")).getSet().get())
            .isEqualTo(4);
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("customerOrderNumber")).getSet().get())
            .isEqualTo("666, 777, 888, 999");

        CommentCreate comment = issueUpdate.getComment().get();
        softly.assertThat(comment.getComment().get())
            .isEqualTo(String.join(
                "\n",
                "Информация в тикете была автоматически изменена.",
                "",
                "Удалены неактуальные заказы (1 шт.): 999.",
                "Список заказов в приложении (2 шт.)."
            ));
        softly.assertThat(comment.getAttachments().get(0))
            .isEqualTo("AT-123");
    }

    @DisplayName("Закрытие тикета, если в группе не осталось план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeIssueTest() {
        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(transitions).execute(any(String.class), any(String.class), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();

        MapF<String, Update<?>> values = issueUpdate.getValues();
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("resolution")).getSet().get())
            .isEqualTo("can'tReproduce");

        CommentCreate comment = issueUpdate.getComment().get();
        softly.assertThat(comment.getComment().get())
            .isEqualTo("Тикет автоматически закрыт.");
    }

    @DisplayName("Закрытие тикета, если назначен исполнитель")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-close_assigned_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-close_assigned_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeAssignedIssueTest() {
        when(issue.getAssignee()).thenReturn(Option.of(Mockito.mock(UserRef.class)));

        handleGroups();
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-close_expired_ticket.xml")
    void reopenClosedIssueFlowTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handleGroups();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }

    @DisplayName("Очистка списка заказов в тикете, когда актуальных заказов не осталось")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-update_ticket_with_empty_list.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-update_ticket_with_empty_list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateIssueFlowWithEmptyListTest() {
        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issues).update(any(String.class), captor.capture());
        IssueUpdate issueUpdate = captor.getValue();

        MapF<String, Update<?>> values = issueUpdate.getValues();
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("defectOrders")).getSet().get())
            .isEqualTo(3);

        CommentCreate comment = issueUpdate.getComment().get();
        softly.assertThat(comment.getComment().get())
            .isEqualTo(String.join(
                "\n",
                "Информация в тикете была автоматически изменена.",
                "",
                "Удалены неактуальные заказы (3 шт.): 777, 888, 999."
            ));
        softly.assertThat(comment.getAttachments())
            .hasSize(0);
    }

    @DisplayName("Создание тикета для одного просроченного план-факта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/dropship_shipment-create_single_ticket_date_partner_19.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-create_single_ticket_date_partner_19.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueFlowTestDatePartner19() {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] 01-11-2020: магазин Muzmart (id: 1) вовремя не отгрузил заказы");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "",
                    "Дата создания заказа: 01-11-2020",
                    "Дата отгрузки: 02-11-2020",
                    "Дедлайн отгрузки ДШ в СЦ СДТ,Климовск: 01-11-2020 15:00:00",
                    "",
                    "Трек ДШ: ws1",
                    "Трек СЦ СДТ,Климовск: ws2",
                    "Email главного представителя: test1@mail.com,test2@mail.com"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(((String[]) values.getOrThrow("tags")))
            .containsExactly("Muzmart:ДШ", "СДТ Климовск:СЦ");
        softly.assertThat(values.getOrThrow("idMagazina"))
            .isEqualTo(1L);
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL))
            .isEqualTo("test1@mail.com");

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID);
    }

    @SuppressWarnings("unchecked")
    @DisplayName("Создание тикета для группы просроченных план-фактов")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/dropship_shipment-create_aggregated_ticket_date_partner_19.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/dropship_shipment-create_aggregated_ticket_date_partner_19.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueFlowTestDatePartner19() {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        clock.setFixed(Instant.parse("2020-11-02T07:01:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        Attachment attachment = mock(Attachment.class);
        when(attachment.getId()).thenReturn("AT-123");
        when(attachments.upload(anyString(), any(InputStream.class))).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();

        ListF<String> listOfAttachments = issueCreate.getAttachments();
        softly.assertThat(listOfAttachments)
            .containsExactly("AT-123");

        MapF<String, Object> values = issueCreate.getValues();
        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] 01-11-2020: магазин Muzmart (id: 1) вовремя не отгрузил заказы");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (2 заказа)");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 888");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        List<String> tags = (List<String>) values.getOrThrow("tags");
        softly.assertThat(tags)
            .containsExactly("Muzmart:ДШ", "СЦ СДТ Климовск:СЦ", "СЦ ПЭК БУТОВО:СЦ");
        softly.assertThat(values.getOrThrow("idMagazina"))
            .isEqualTo(1L);
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL))
            .isEqualTo("test1@mail.com");

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID);
    }

}
