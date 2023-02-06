package ru.yandex.market.logistics.management.service.dbqueue;

import java.time.LocalTime;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.queue.model.SnapshotActivationPayload;
import ru.yandex.market.logistics.management.queue.processor.SnapshotActivationProcessingService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/prepare_snapshots_for_activation.xml")
class SnapshotActivationProcessingServiceTest extends AbstractContextualTest {

    private static final long PARTNER_ID_1 = 1;
    private static final long PARTNER_ID_2 = 2;
    private static final long SNAPSHOT_ID_1 = 1;
    private static final long SNAPSHOT_ID_3 = 3;

    @Autowired
    private SnapshotActivationProcessingService snapshotActivationProcessingService;

    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/after/activate_snapshot.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void testActivateSnapshot() {
        snapshotActivationProcessingService.processPayload(
            new SnapshotActivationPayload("", Map.of(PARTNER_ID_2, SNAPSHOT_ID_3))
        );
    }

    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/after/activate_snapshot_several_partners.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void testActivateSnapshotsForSeveralPartners() {
        ArgumentCaptor<MessageDto> telegramMessageCaptor = ArgumentCaptor.forClass(MessageDto.class);

        snapshotActivationProcessingService.processPayload(
            new SnapshotActivationPayload("", Map.of(PARTNER_ID_2, SNAPSHOT_ID_3, PARTNER_ID_1, SNAPSHOT_ID_1))
        );

        Mockito.verify(pechkinHttpClient, Mockito.timeout(1000)).sendMessage(telegramMessageCaptor.capture());

        softly.assertThat(telegramMessageCaptor.getValue().getMessage()).contains(
            "[Активирован снэпшот](https://lms-url/lms/delivery-interval-schedule-day?partner=1):\n" +
                "Служба: Delivery_1\n" +
                "ID службы: 1\n"
        );

        softly.assertThat(telegramMessageCaptor.getValue().getMessage()).contains(
            "[Активирован снэпшот](https://lms-url/lms/delivery-interval-schedule-day?partner=2):\n" +
                "Служба: Delivery_2\n" +
                "ID службы: 2\n"
        );
    }

    @DatabaseSetup(
        "/data/controller/admin/deliveryIntervalSnapshots/prepare_snapshots_for_activation_save_update_delete.xml"
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/after/activate_snapshot_save_update_delete.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void testActivateSaveUpdateAndDelete() {
        snapshotActivationProcessingService.processPayload(
            new SnapshotActivationPayload("", Map.of(PARTNER_ID_1, SNAPSHOT_ID_1, PARTNER_ID_2, SNAPSHOT_ID_3))
        );
    }

    private LocalTime time(String text) {
        return LocalTime.parse(text);
    }

}
