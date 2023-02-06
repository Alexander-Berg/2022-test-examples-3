package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.io.InputStream;
import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Transition;
import ru.yandex.startrek.client.model.Update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@DisplayName("Тесты процессора по созданию тикетов для просрочек отгрузки с ФФ")
public class FfShipmentProcessorTest extends StartrekProcessorTest {

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2020-11-05T20:00:01.00Z"), MOSCOW_ZONE);
    }

    @DisplayName("Создание тикета для одного просроченного план-факта отгрузки с ФФ")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSingleIssueTest() {
        clock.setFixed(Instant.parse("2021-11-02T09:00:01.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] 01-11-2020: Cофьино вовремя не отгрузил заказы в Какая-то СД.");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "Дедлайн отгрузки с ФФ: 01-11-2020 15:00:00",
                    "",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(((String[]) values.getOrThrow("tags")))
            .containsExactly("Cофьино:172", "Какая-то СД:12345");
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов отгрузки с ФФ")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/create_aggregated.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/create_aggregated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createAggregatedIssueTest() {
        clock.setFixed(Instant.parse("2021-11-02T09:00:01.00Z"), MOSCOW_ZONE);

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
            .isEqualTo("[MQM] 01-11-2020: Cофьино вовремя не отгрузил заказы в Какая-то СД.");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (кол-во заказов: 2)");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 888");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        String[] tags = (String[]) values.getOrThrow("tags");
        softly.assertThat(tags)
            .containsExactly("Cофьино:172", "Какая-то СД:12345");
    }

    @DisplayName("Игнорирование экспресса")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/express.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void expressProcessingTest() {
        clock.setFixed(Instant.parse("2021-11-02T09:00:01.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        verify(issues, never()).create(any());
    }

    @DisplayName("Создание тикета для одного просроченного план-факта отгрузки с ФФ игнорируется "
        + "для старой группировки")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/create_single_wrong_aggregation.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/create_single_wrong_aggregation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void ignoreCreationWhenWrongAggregationTest() {
        clock.setFixed(Instant.parse("2021-11-02T09:00:01.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        verify(issues, never()).create(any());
    }

    @DisplayName("Обновление списка заказов в тикете")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/update.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void updateIssueTest() {
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

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/reopen.xml")
    @Test
    void reopenIssueTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handleGroups();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }

    @DisplayName("Закрытие тикета, если в группе не осталось план-фактов")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/close.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/close.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
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

    @DisplayName("Создание тикета для одного просроченного план-факта отгрузки с ФФ игнорируется до промежутка 10-19")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/create_single_ignore.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void ignoreCreateIssueWhenNotTimeBeforeTest() {
        clock.setFixed(Instant.parse("2020-11-02T06:59:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        verify(issues, never()).create(any());
    }

    @DisplayName("Создание тикета для одного просроченного план-факта отгрузки с ФФ игнорируется "
        + "после промежутка 10-19")
    @DatabaseSetup("/service/processor/qualityrule/before/shipment/ff/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/shipment/ff/create_single_ignore.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void ignoreCreateIssueWhenNotTimeAfterTest() {
        clock.setFixed(Instant.parse("2020-11-02T16:01:00.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        verify(issues, never()).create(any());
    }
}
