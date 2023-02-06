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
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Transition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@DisplayName("Тесты обработчика для очереди CallCourier")
class CallCourierQualityRuleProcessorTest extends StartrekProcessorTest {

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-03-01T20:00:50.00Z"), MOSCOW_ZONE);
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/call_courier-create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createIssueTest() {
        clock.setFixed(Instant.parse("2020-11-07T11:00:50.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(issues.create(any()))
            .thenReturn(new Issue(null, null, "MONITORINGSNDBX-1", null, 1, new EmptyMap<>(), null));

        handlePlanFacts();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] Заказ 777 от 01-11-2020 получил ошибку на вызове callCourier от СД Почта");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(String.join(
                "\n",
                "https://abo.market.yandex-team.ru/order/777",
                "https://ow.market.yandex-team.ru/order/777",
                "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                "",
                "Номер заказа: 777",
                "Дата создания заказа: 01-11-2020",
                "Дата отгрузки: 02-11-2020"
            ));
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat((String[]) values.getOrThrow("tags"))
            .containsExactly("Почта:987654321", "СД");
    }

    @DisplayName("Закрытие тикета в Startrek для просроченных план-фактов, после получения статуса")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/call_courier-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeIssueTest() {
        handlePlanFacts();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(transitions).execute(any(String.class), any(String.class), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();

        softly.assertThat(((ScalarUpdate<?>) issueUpdate.getValues().getOrThrow("resolution")).getSet().get())
            .isEqualTo("fixed");
        softly.assertThat(issueUpdate.getComment().get().getComment().get())
            .isEqualTo("Тикет автоматически закрыт.");
    }

    @DisplayName("Попытка закрыть тикет, который уже был закрыт вручную")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-close_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/call_courier-close_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeClosedIssueTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        handlePlanFacts();
        verify(transitions, never()).execute(any(String.class), any(Transition.class), any(IssueUpdate.class));
    }

    @DisplayName("При достижении последней попытки на обработку тега, план-факт отмечается обработанным")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-tag_last_attempt.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/call_courier-tag_last_attempt.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void lastAttemptForSettingTagTest() {
        handlePlanFacts();
    }

    @DisplayName("Добавить информацию об ошибке в тикет, если не смогли классифицировать")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-error_comment.xml")
    void commentErrorTest() {
        handlePlanFacts();
        ArgumentCaptor<CommentCreate> captor = ArgumentCaptor.forClass(CommentCreate.class);
        verify(comments).create(eq("MONITORINGSNDBX-1"), captor.capture());
        CommentCreate commentCreate = captor.getValue();

        softly.assertThat(commentCreate.getComment().get())
            .isEqualTo("Сообщение об ошибке: ABC–XYZ.");
    }

    @DisplayName("Закрытие тикета в Startrek для неактуальных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-close_not_actual_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/call_courier-close_not_actual_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeIssueForExpiredPlanFactTest() {
        handlePlanFacts();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(transitions).execute(any(String.class), any(String.class), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();

        softly.assertThat(((ScalarUpdate<?>) issueUpdate.getValues().getOrThrow("resolution")).getSet().get())
            .isEqualTo("fixed");
        softly.assertThat(issueUpdate.getComment().get().getComment().get())
            .isEqualTo("Тикет автоматически закрыт.");
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/call_courier-reopen_ticket.xml")
    void reopenClosedIssueFlowTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handlePlanFacts();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }
}
