package ru.yandex.market.delivery.tracker;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryTrackService;

@TestPropertySource(properties = {"delivery.dsbs.external-tracking.excluded-partners=101,2"})
public class DeliveryTrackServiceExcludedPartnersTest extends AbstractContextualTest {

    private static final String TRACK_CODE_1 = "TRACK_CODE_1";
    private static final long DS_1_ID = 101;
    private static final long CONSUMER_ID = 10;
    private static final String DIFF_ORDER_ID = "DIFF_ORDER_ID";

    @Autowired
    private DeliveryTrackService deliveryTrackService;

    @Test
    @DatabaseSetup("/database/states/order_ds.xml")
    void testRegisterExternalOrderTrackExcludedPartner() {
        DeliveryTrackMeta deliveryTrackMeta = registerExternalOrderTrack();

        assertions().assertThat(deliveryTrackMeta)
            .extracting(DeliveryTrackMeta::getDeliveryServiceId)
            .isEqualTo(DS_1_ID);
    }

    DeliveryTrackMeta registerExternalOrderTrack() {
        return deliveryTrackService.registerDeliveryTrack(
            TRACK_CODE_1,
            DS_1_ID,
            CONSUMER_ID,
            DIFF_ORDER_ID,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            DeliveryType.DELIVERY,
            true,
            EntityType.EXTERNAL_ORDER,
            null,
            null
        );
    }
}
