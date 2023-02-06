package ru.yandex.market.mbi.api.controller.delivery;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.model.DeliveryServiceInfoDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * @author komarovns
 * @date 06.08.2020
 */
public class DeliveryServicesControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "DeliveryServicesControllerTest.getCommonDeliveryServicesTest.before.csv")
    public void getCommonDeliveryServicesTest() {
        Assertions.assertEquals(
                List.of(new DeliveryServiceInfoDTO(1, "name 1")),
                mbiApiClient.getCommonDeliveryServices()
        );
    }
}
