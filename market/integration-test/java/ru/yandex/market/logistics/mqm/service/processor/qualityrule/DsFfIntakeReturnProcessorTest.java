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

@DisplayName("Тесты процессора по созданию тикетов для просрочек приемки на ФФ с СД для обратной логистики")
public class DsFfIntakeReturnProcessorTest extends StartrekProcessorTest {
    @BeforeEach
    public void setUp() {
        clock.setFixed(Instant.parse("2020-11-02T09:00:00.00Z"), MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Создание тикета для одного просроченного план-факта приемки на ФФ с СД")
    @DatabaseSetup("/service/processor/qualityrule/before/ds_ff_intake_return/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/ds_ff_intake_return/create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueTest() {
        when(issues.create(any())).thenReturn(issue);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary"))
            .isEqualTo("[MQM] 01-11-2020: ФФ Cофьино вовремя не принял заказы обратно от Какая-то СД.");
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111",
                    "Дата создания заказа: 01-11-2020",
                    "Дедлайн приемки обратно на ФФ: 01-11-2020 15:00",
                    "Трек ФФ: 101",
                    "Трек СД: 102"
                )
            );
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(((String[]) values.getOrThrow("tags")))
            .containsExactly("Cофьино:172", "Какая-то СД:123");
    }

    @Test
    @DisplayName("Создание тикета для группы просроченных план-фактов приемки на ФФ с СД")
    @DatabaseSetup("/service/processor/qualityrule/before/ds_ff_intake_return/create_aggregated.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/ds_ff_intake_return/create_aggregated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueTest() {
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
            .isEqualTo("[MQM] 01-11-2020: ФФ Cофьино вовремя не принял заказы обратно от Какая-то СД.");
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
            .containsExactly("Cофьино:172", "Какая-то СД:123");
    }
}
