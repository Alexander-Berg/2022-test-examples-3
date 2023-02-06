package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

class ClientReturnScShipmentAggregatedProcessorTest extends StartrekProcessorTest {

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2020-11-07T11:00:01.00Z"), MOSCOW_ZONE);
    }

    @DisplayName("Создание тикета в Startrek для группы просроченных план-фактов на сегменте забора из СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/clientreturn/cr_sc_shipment/create_group_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/clientreturn/cr_sc_shipment/create_group_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueTest() {
        clock.setFixed(Instant.parse("2020-11-07T12:00:01.00Z"), MOSCOW_ZONE);
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
            .isEqualTo("[MQM] 06-11-2020: СЦ Имя СЦ 1 не отправил клиентские возвраты на ЦТЭ за 3 дня.");
        softly.assertThat(values.getOrThrow("description")).isEqualTo("Список заказов в приложении (2 шт.)");
        softly.assertThat(values.getOrThrow("defectOrders")).isEqualTo(2);
        softly.assertThat(values.getOrThrow("queue")).isEqualTo("MONITORINGSNDBX");
        softly.assertThat((long[]) values.getOrThrow("components")).containsExactly(94921L);
        softly.assertThat((String[]) values.getOrThrow("tags")).containsExactlyInAnyOrder("Имя СЦ 1:10203");

        verify(attachments).upload(anyString(), any());
    }

    @DisplayName("Добавление комментариев в тикета Startrek для просроченных план-фактов на сегменте забора из СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/clientreturn/cr_sc_shipment/add_comment.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/clientreturn/cr_sc_shipment/add_comment.xml",
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
            "Удалены неактуальные заказы (1 шт.): 21.",
            "Добавлены новые заказы (1 шт.): 31.",
            "Список заказов в приложении (2 шт.)."
        ));

        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("defectOrders")).getSet().get())
            .isEqualTo(3);

        verify(attachments).upload(anyString(), any());
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов на сегменте забора из СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/clientreturn/cr_sc_shipment/close_issue.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/clientreturn/cr_sc_shipment/close_issue.xml",
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
    @DatabaseSetup("/service/processor/qualityrule/before/clientreturn/cr_sc_shipment/reopen_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/clientreturn/cr_sc_shipment/reopen_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void reopenClosedIssueFlowTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handleGroups();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }

    @DisplayName("Перевыставление создания тикета обработчиком на заданное время")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/clientreturn/cr_sc_shipment/rescheduling.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/clientreturn/cr_sc_shipment/rescheduling.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotCreateAggregatedIssueInWrongTimeTest() {
        handleGroups();
        verify(issues, Mockito.never()).create(any());
    }
}
