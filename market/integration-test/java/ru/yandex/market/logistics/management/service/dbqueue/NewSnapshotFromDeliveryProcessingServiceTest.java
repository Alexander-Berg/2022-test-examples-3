package ru.yandex.market.logistics.management.service.dbqueue;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.queue.model.NewSnapshotFromDeliveryPayload;
import ru.yandex.market.logistics.management.queue.processor.NewSnapshotFromDeliveryProcessingService;

@DatabaseSetup("/data/service/dbqueue/deliveryIntervalSnapshots/prepare_data.xml")
class NewSnapshotFromDeliveryProcessingServiceTest extends AbstractContextualTest {

    @Autowired
    private NewSnapshotFromDeliveryProcessingService newSnapshotFromDeliveryProcessingService;

    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @ExpectedDatabase(
        value = "/data/service/dbqueue/deliveryIntervalSnapshots/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testNoNewStatusNoDiffChange() {
        ArgumentCaptor<MessageDto> telegramMessageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        newSnapshotFromDeliveryProcessingService
            .processPayload(new NewSnapshotFromDeliveryPayload("", 2L));
        Mockito.verify(pechkinHttpClient, Mockito.timeout(1000).times(1))
            .sendMessage(telegramMessageCaptor.capture());
        softly.assertThat(telegramMessageCaptor.getValue().getChannel()).isEqualTo("Delivery_interval_snapshots");
        softly.assertThat(telegramMessageCaptor.getValue().getMessage()).isEqualTo(
            "Получен новый снэпшот:\n" +
                "Служба: Delivery_2\n" +
                "ID службы: 2\n" +
                "Время получения: 2020-09-17T17:59:58\n" +
                "Отличия от текущей конфигурации: Есть расхождения"
        );

    }

    @ExpectedDatabase(
        value = "/data/service/dbqueue/deliveryIntervalSnapshots/diff_intervals.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testNewStatusDiffChange() {
        ArgumentCaptor<MessageDto> telegramMessageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        newSnapshotFromDeliveryProcessingService
            .processPayload(new NewSnapshotFromDeliveryPayload("", 3L));
        Mockito.verify(pechkinHttpClient, Mockito.timeout(1000).times(1))
            .sendMessage(telegramMessageCaptor.capture());
        softly.assertThat(telegramMessageCaptor.getValue().getChannel()).isEqualTo("Delivery_interval_snapshots");
        softly.assertThat(telegramMessageCaptor.getValue().getMessage()).isEqualTo(
            "Получен новый снэпшот:\n" +
                "Служба: Delivery_3\n" +
                "ID службы: 3\n" +
                "Время получения: 2020-09-17T17:59:58\n" +
                "Отличия от текущей конфигурации: Есть расхождения"
        );
    }

}
