package ru.yandex.market.checkout.checkouter.delivery;

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.common.util.DeliveryChange;

public class DeliveryUpdateValidationResultTest {

    @Test
    public void onlyAllowedChanges() throws Exception {
        DeliveryUpdateValidationResult<DeliveryChange> dvr = new DeliveryUpdateValidationResult<>(
                false, EnumSet.of(DeliveryChange.TRACK_LIST)
        );
        Assertions.assertTrue(dvr.onlyAllowedChanges(DeliveryChange.TRACK_LIST, DeliveryChange.TRACK_STATUS,
                DeliveryChange.TRACKER_ID));
    }

}
