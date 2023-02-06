package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Transition;
import ru.yandex.startrek.client.model.Update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@DisplayName("Тесты обработки просрочки отгрузки СЦ-СД")
class SortingCenterToDeliveryServiceShipmentProcessorTest extends StartrekProcessorTest {

    @Autowired
    ConversionService conversionService;

    @Autowired
    Configuration freemarkerConfiguration;

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2020-11-07T11:15:30.00Z"), MOSCOW_ZONE);
    }

    @DisplayName("Проверка, что процессор не создает тикеты, если вызвать до рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_ds/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_ds/scheduled_today_when_work_starts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotCreateTicketsIfBeforeWorkingHoursTest() {
        clock.setFixed(
            LocalDateTime.of(
                LocalDate.of(2021, 3, 22),
                LocalTime.of(9, 59)
            )
                .atZone(MOSCOW_ZONE)
                .toInstant(),
            MOSCOW_ZONE
        );

        handleGroups();
        verifyZeroInteractions(issues);
    }

    @DisplayName("Проверка, что процессор не создает тикеты, если вызвать после рабочих часов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_ds/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_ds/scheduled_tomorrow_when_work_starts.xml",
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
        verifyZeroInteractions(issues);
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта отгрузки СЦ-СД")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_ds/create_ticket_with_one_planfact.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_ds/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueTest() {
        when(issues.create(any()))
            .thenReturn(new Issue(null, null, "MONITORINGSNDBX-1", null, 1, new EmptyMap<>(), null));

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 07-11-2020: СЦ Тестовый СЦ вовремя не отгрузил заказы."
            );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "Дата создания заказа: 01-11-2020",
                    "Дедлайн приема в СД: 07-11-2020 11:11",
                    "Трек СЦ: 101",
                    "Трек СД: 102"
                )
            );
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat((String[]) values.getOrThrow("tags"))
            .containsExactly("Тестовый СЦ:987654321");
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов отгрузки СЦ-СД")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/shipment/sc_ds/create_ticket_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_ds/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueTest() {
        Attachment attachment = Mockito.mock(Attachment.class);

        when(issues.create(any()))
            .thenReturn(new Issue(null, null, "MONITORINGSNDBX-1", null, 1, new EmptyMap<>(), null));

        when(attachments.upload(anyString(), any())).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 07-11-2020: СЦ Тестовый СЦ вовремя не отгрузил заказы."
            );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (2 шт.)");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 778");
        softly.assertThat((String[]) values.getOrThrow("tags"))
            .containsExactly("Тестовый СЦ:987654321");

        verify(attachments).upload(anyString(), any());
    }

    @DisplayName("Добавление комментариев в тикета Startrek для просроченных план-фактов отгрузки СЦ-СД")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_ds/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_ds/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void commentAggregatedIssueTest() {
        Attachment attachment = Mockito.mock(Attachment.class);
        when(attachments.upload(anyString(), any())).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issues).update(eq("MONITORINGSNDBX-1"), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();
        String commentString = issueUpdate.getComment().get().getComment().get();
        MapF<String, Update<?>> values = issueUpdate.getValues();

        softly.assertThat(commentString).isEqualTo(String.join(
            "\n",
            "Информация в тикете была автоматически изменена.",
            "",
            "Удалены неактуальные заказы (1 шт.): 778.",
            "Удаленные заказы, в которых был получен 10 чекпоинт без 130 (1 шт.): 778.",
            "Добавлены новые заказы (1 шт.): 779.",
            "Список заказов в приложении (2 шт.)."
        ));

        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("customerOrderNumber")).getSet().get())
            .isEqualTo("777, 778, 779");
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("defectOrders")).getSet().get())
            .isEqualTo(3);

        verify(attachments).upload(anyString(), any());
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов отгрузки СЦ-СД")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_ds/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/sc_ds/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeAllAggregatedIssueTest() {
        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(transitions).execute(eq("MONITORINGSNDBX-1"), any(String.class), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();
        String commentString = issueUpdate.getComment().get().getComment().get();
        MapF<String, Update<?>> values = issueUpdate.getValues();

        softly.assertThat(commentString).isEqualTo("Тикет автоматически закрыт.");

        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("resolution")).getSet().get())
            .isEqualTo("can'tReproduce");
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/sc_ds/reopen_ticket.xml")
    void reopenClosedIssueFlowTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handleGroups();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }
}
