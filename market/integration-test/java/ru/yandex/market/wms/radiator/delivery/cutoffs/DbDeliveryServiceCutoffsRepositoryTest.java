package ru.yandex.market.wms.radiator.delivery.cutoffs;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

class DbDeliveryServiceCutoffsRepositoryTest extends IntegrationTestBackend {

    @Autowired
    private DbDeliveryServiceCutoffsRepository repository;

    @Autowired
    private Dispatcher dispatcher;


    @Test
    @Disabled
    void refill() {
        doRefill(WH_1_ID);
    }

    private void doRefill(String warehouseId) {
        dispatcher.withWarehouseId(
                warehouseId, () -> {
                    var ds1 = ds("DS1", "12:00", "12:00", "15:00", "11:00", "who", "who", null, null);
                    var ds2 = ds("DS2", "12:00", "15:00", "18:00", "14:00", "who", null, null, null);
                    repository.refill(List.of(ds1, ds2));
                    assertThat(
                            repository.findAll(),
                            is(equalTo(List.of(ds1, ds2)))
                    );

                    var addDate = OffsetDateTime.parse("2020-10-01T11:00:00Z");
                    var editDate = OffsetDateTime.parse("2020-10-01T10:00:00Z");
                    var ds2New = ds("DS2", "15:00", "15:00", "18:00", "14:00", "robot", "robot", null, editDate);
                    var ds3 = ds("DS3", "12:00", "15:00", "18:00", "14:00", "robot", "robot", addDate, null);
                    repository.refill(List.of(ds2New, ds3));
                    assertThat(
                            repository.findAll(),
                            is(equalTo(
                                    List.of(
                                            ds("DS2", "15:00", "15:00", "18:00", "14:00","who", "robot", null, editDate),
                                            ds3
                                    )
                            ))
                    );
                }
        );
    }

    private static DeliveryServiceCutoff ds(
            String ds,
            String orderCreationCutoff,
            String pickingCutoff,
            String shippingCutoff,
            String warehouseCutoff,
            String addWho,
            String editWho,
            OffsetDateTime addDate,
            OffsetDateTime editDate) {

        return DeliveryServiceCutoff.builder()
                .deliveryServiceCode(ds)
                .orderCreationCutoff(LocalTime.parse(orderCreationCutoff))
                .pickingCutoff(LocalTime.parse(pickingCutoff))
                .shippingCutoff(LocalTime.parse(shippingCutoff))
                .warehouseCutoff(LocalTime.parse(warehouseCutoff))
                .addWho(addWho)
                .editWho(editWho)
                .addDate(addDate)
                .editDate(editDate)
                .build();
    }
}
