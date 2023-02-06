package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.time.Instant;
import java.util.List;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

public class LockerToSortingCenterIntakeFailQualityRuleProcessorTest extends StartrekProcessorTest {

    private static final List<Long> EXTERNAL_RETURN_IDS = List.of(10L, 20L, 30L);

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-04-15T16:20:00.00Z"), MOSCOW_ZONE);
    }

    @DisplayName("Создание тикетов в Startrek для план-фактов Локер-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/clientreturn/fail_locker_sc/create_ticket.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/clientreturn/fail_locker_sc/create_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createIssueTest() {
        Attachment attachment = Mockito.mock(Attachment.class);

        when(issues.create(any()))
            .thenReturn(new Issue(null, null, "MONITORINGSNDBX-1", null, 1, new EmptyMap<>(), null));

        when(attachments.upload(anyString(), any())).thenReturn(attachment);

        handlePlanFacts();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues, Mockito.times(3)).create(captor.capture());

        List<IssueCreate> createdIssues = captor.getAllValues();
        for (int i = 0; i < EXTERNAL_RETURN_IDS.size(); i++) {
            MapF<String, Object> values = createdIssues.get(i).getValues();

            softly.assertThat(values.getOrThrow("summary")).isEqualTo(String.format(
                "[MQM] Возвраты в постаматы Яндекс Маркета: Клиент заложил, курьер отметил \"Ячейка пуста\". "
                    + "Возврат %d",
                EXTERNAL_RETURN_IDS.get(i)
            ));
            softly.assertThat(values.getOrThrow("description"))
                .isEqualTo(String.format(
                    "Возврат %d\n" +
                        "Ссылка на заявление: " +
                        "https://market-checkouter-prod.s3.mds.yandex.net/return-application-%d.pdf\n" +
                        "Пустая ячейка обнаружена 14-04-2021 16:19\n" +
                        "Курьер: The best courier name\n" +
                        "Комментарий от курьера: EMPTY CELL",
                    EXTERNAL_RETURN_IDS.get(i),
                    EXTERNAL_RETURN_IDS.get(i)
                ));
            softly.assertThat(values.getOrThrow("defectOrders"))
                .isEqualTo(1);
            softly.assertThat(values.getOrThrow("queue"))
                .isEqualTo("MONITORINGSNDBX");
        }
    }
}
