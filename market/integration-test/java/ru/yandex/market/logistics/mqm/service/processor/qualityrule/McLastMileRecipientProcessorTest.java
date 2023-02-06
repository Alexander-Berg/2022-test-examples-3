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

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@DisplayName("Тесты процессора по созданию тикетов для просрочек по передаче на последнюю милю для своей курьерки")
public class McLastMileRecipientProcessorTest extends StartrekProcessorTest {

    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2020-11-05T20:00:00.00Z"), MOSCOW_ZONE);
    }


    @DisplayName("Создание тикета для одного просроченного план-факта для курьерской доставки (своя курьерка)")
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_single_mc.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/create_single_mc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSingleIssueTest() {
        clock.setFixed(Instant.parse("2020-11-02T11:00:01.00Z"), MOSCOW_ZONE);

        when(issues.create(any())).thenReturn(issue);

        Attachment attachment = mock(Attachment.class);
        when(attachments.upload(anyString(), any(InputStream.class))).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo(
                "[MQM] 01-11-2020: МК (Своя курьерка) – Своя курьерка" +
                    " – вовремя не передали на последнюю милю"
            );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "Дедлайн передачи на последнюю милю: 01-11-2020 14:00:00"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat((long[]) values.getOrThrow("components"))
            .containsExactly(92195);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(((String[]) values.getOrThrow("tags")))
            .containsExactly("Своя курьерка:987654321");

        verify(attachments).upload(anyString(), any());
    }

    @DisplayName("Создание тикета для группы просроченных план-фактов для курьерской доставки (своя курьерка)")
    @DatabaseSetup("/service/processor/qualityrule/before/last_mile_recipient/create_aggregated_mc.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/last_mile_recipient/create_aggregated_mc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createAggregatedIssueTest() {
        clock.setFixed(Instant.parse("2020-11-02T11:00:01.00Z"), MOSCOW_ZONE);

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
            .isEqualTo(
                "[MQM] 01-11-2020: МК (Своя курьерка) – Своя курьерка" +
                    " – вовремя не передали на последнюю милю"
            );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo("Список заказов в приложении (кол-во заказов: 2)");
        softly.assertThat((long[]) values.getOrThrow("components"))
            .containsExactly(92195);
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 888");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        String[] tags = (String[]) values.getOrThrow("tags");
        softly.assertThat(tags)
            .containsExactly("Своя курьерка:987654321");
    }
}
