package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.market.logistics.mqm.service.yt.PvzContactInformationCache;
import ru.yandex.market.logistics.mqm.service.yt.dto.PvzContactInformation;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Transition;
import ru.yandex.startrek.client.model.Update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@DisplayName("Тесты процессора по созданию тикетов для просрочек по отгрузке последней мили")
public class LastMileDeliveryAggregatedProcessorTest extends StartrekProcessorTest {

    @Autowired
    PvzContactInformationCache pvzContactInformationCache;

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2020-11-05T20:00:00.00Z"), MOSCOW_ZONE);
        when(pvzContactInformationCache.getPvzContactInformationByPvz(anyLong()))
            .thenReturn(new PvzContactInformation(1, "email1", "phone1", "phone2"));
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-close_expired_ticket.xml")
    void reopenClosedIssueFlowTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handleGroups();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }

    @DisplayName("Создание тикета для одного просроченного план-факта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-create_single_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-create_single_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueFlowTest() {
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 01-11-2020: Контрактная доставка (Самовывоз) – 5Post – не был вовремя получен финальный статус"
            );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "",
                    "Дата создания заказа: 01-11-2020",
                    "Дедлайн финального статуса: 01-11-2020 15:00:00",
                    "",
                    "Трек СД: 111222333",
                    "Последний чекпоинт: 49"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat((long[]) values.getOrThrow("components"))
            .containsExactly(88999);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(((String[]) values.getOrThrow("tags")))
            .containsExactly("5Post");
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-create_aggregated_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-create_aggregated_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueFlowTest() {
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
            .isEqualTo("[MQM] 01-11-2020: МК (Своя курьерка) – Яндекс.Go – не был вовремя получен финальный статус");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (2 заказа)");
        softly.assertThat((long[]) values.getOrThrow("components"))
            .containsExactly(88999);
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 888");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        String[] tags = (String[]) values.getOrThrow("tags");
        softly.assertThat(tags)
            .containsExactly("Яндекс.Go");
    }

    @DisplayName("Обновление списка заказов в тикете")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-update_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-update_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateIssueFlowTest() {
        Attachment attachment = mock(Attachment.class);
        when(attachment.getId()).thenReturn("AT-123");
        when(attachments.upload(anyString(), any(InputStream.class))).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issues).update(any(String.class), captor.capture());
        IssueUpdate issueUpdate = captor.getValue();

        MapF<String, Update<?>> values = issueUpdate.getValues();
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("defectOrders")).getSet().get())
            .isEqualTo(3);
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("customerOrderNumber")).getSet().get())
            .isEqualTo("777, 888, 999");

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
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-close_ticket.xml",
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

    @DisplayName("Группа должна быть запланирована на ближайшее время, пока тикет не закрыт")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-reschedule.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-reschedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void rescheduleIssueFlowTest() {
        clock.setFixed(Instant.parse("2020-11-02T20:00:00.00Z"), MOSCOW_ZONE);

        handleGroups();
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-create_pickup_point_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-create_pickup_point_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createPickupPointIssueTest() {
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
        softly.assertThat(listOfAttachments).containsExactly("AT-123");

        MapF<String, Object> values = issueCreate.getValues();
        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] 01-11-2020: Самовывоз – Маркет ПВЗ – Тестовый СД – не был вовремя получен финальный статус"
        );
        softly.assertThat(values.getOrThrow("description")).isEqualTo("Список заказов в приложении (2 заказа)");
        softly.assertThat((long[]) values.getOrThrow("components")).containsExactly(11111);
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777, 888");
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX");
    }

    @DisplayName("Создание тикета для одного просроченного план-факта доставки в ПВЗ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery_to_pvz-create_single_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery_to_pvz-create_single_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueToPvzFlowTest() {
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 01-11-2020: Самовывоз – Маркет ПВЗ – Тестовый СД – не был вовремя получен финальный статус"
            );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "",
                    "Дата создания заказа: 01-11-2020",
                    "Дедлайн финального статуса: 01-11-2020 15:00:00",
                    "",
                    "Трек СД: 111222333",
                    "Последний чекпоинт: ",
                    "Email ПВЗ: email1",
                    "Телефон ПВЗ: phone1",
                    "Телефон руководителя ПВЗ: phone2"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(1);
        softly.assertThat((long[]) values.getOrThrow("components")).containsExactly(11111);
        softly.assertThat(values.getOrThrow("customerOrderNumber")).isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX");
    }

    @DisplayName("Проверка, что процессор не создает тикеты, если вызвать после рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery_to_pvz-create_single_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery_to_pvz-create_single_ticket_ignored.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotCreateTicketsIfAfterWorkingHoursTest() {
        clock.setFixed(
            LocalDateTime.of(
                    LocalDate.of(2021, 3, 22),
                    LocalTime.of(19, 1)
                )
                .atZone(MOSCOW_ZONE)
                .toInstant(),
            MOSCOW_ZONE
        );

        handleGroups();
        verify(issues, never()).create(any());
    }

    @DisplayName("Проверка, что процессор не создает тикеты, если вызвать до рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery_to_pvz-create_single_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery_to_pvz-create_single_ticket_ignored.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotCreateTicketsIfBeforeWorkingHoursTest() {
        clock.setFixed(
            LocalDateTime.of(
                    LocalDate.of(2021, 3, 23),
                    LocalTime.of(9, 59)
                )
                .atZone(MOSCOW_ZONE)
                .toInstant(),
            MOSCOW_ZONE
        );

        handleGroups();
        verify(issues, never()).create(any());
    }

    @DisplayName("Проверка, что процессор не обработает группировку DATE_PARTNER_19 если такая группировка "
        + "подходит под другую с другой агрегацией")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_delivery-create_pickup_point_ticket_skip.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_delivery-create_pickup_point_ticket_skip.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void ignoreIfDatePartnerConflictsWithOthers() {
        clock.setFixed(
            LocalDateTime.of(
                    LocalDate.of(2021, 3, 23),
                    LocalTime.of(10, 59)
                )
                .atZone(MOSCOW_ZONE)
                .toInstant(),
            MOSCOW_ZONE
        );

        handleGroups();
        verify(issues, never()).create(any());
    }
}
