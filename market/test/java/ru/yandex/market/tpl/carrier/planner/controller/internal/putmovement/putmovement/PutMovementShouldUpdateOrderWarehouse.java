package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.Location;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.delivery.request.PutMovementRequest;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PutMovementShouldUpdateOrderWarehouse extends BasePlannerWebTest {

    private static final ResourceId SHIPPER_LOCATION_ID = new ResourceId("200", "5678");
    private static final ResourceId RECEIVER_LOCATION_ID = new ResourceId("20", "1234");

    private final DbQueueTestUtil dbQueueTestUtil;
    private final PutMovementHelper putMovementHelper;
    private final OrderWarehouseRepository orderWarehouseRepository;

    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    @Test
    void shouldUpdateOrderWarehouse() {
        RequestWrapper<PutMovementRequest> request1 = PutMovementControllerTestUtil.wrap(PutMovementControllerTestUtil.prepareMovement(
                new ResourceId("TMM1", null),
                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                BigDecimal.ONE,
                new ResourceId("200", "5678"),
                new ResourceId("20", "1234")
        ));

        putMovementHelper.performPutMovement(request1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_WAREHOUSE_ADDRESS, 0);

        RequestWrapper<PutMovementRequest> request2 = PutMovementControllerTestUtil.wrap(PutMovementControllerTestUtil.prepareMovement(
                new ResourceId("TMM2", null),
                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                BigDecimal.ONE,
                SHIPPER_LOCATION_ID,
                RECEIVER_LOCATION_ID,
                PutMovementControllerTestUtil.INBOUND_DEFAULT_INTERVAL,
                PutMovementControllerTestUtil.OUTBOUND_DEFAULT_INTERVAL,
                m -> m.setShipper(new Party(LogisticPoint.builder(SHIPPER_LOCATION_ID)
                        .setLocation(new Location(
                                "Белоруссия",
                                "БФО",
                                "Минск",
                                "Минск",
                                "Минск",
                                "",
                                "Минский бульвар",
                                "12",
                                null,
                                null,
                                null,
                                "223456",
                                "-",
                                5,
                                "Смоленская",
                                BigDecimal.valueOf(53.123456),
                                BigDecimal.valueOf(38.234567),
                                213L,
                                ""

                                )
                        )
                        .setPhones(List.of(
                                new Phone("88005553535", "123"),
                                new Phone("123456789", null)
                        ))
                        .setContact(new Person("Имя", "Фамилия", null))
                        .build(),
                        PutMovementControllerTestUtil.legalEntity(),
                        null
                ))
        ));

        putMovementHelper.performPutMovement(request2);

        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_WAREHOUSE_ADDRESS, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.UPDATE_WAREHOUSE_ADDRESS);

        OrderWarehouse orderWarehouse = orderWarehouseRepository.findByYandexIdCacheable(SHIPPER_LOCATION_ID.getYandexId()).get();

        Assertions.assertThat(orderWarehouse.getAddress()).isNotNull();
        Assertions.assertThat(orderWarehouse.getAddress().getCountry()).isEqualTo("Белоруссия");
        Assertions.assertThat(orderWarehouse.getAddress().getRegion()).isEqualTo("Минск");
        Assertions.assertThat(orderWarehouse.getAddress().getCity()).isEqualTo("Минск");
        Assertions.assertThat(orderWarehouse.getAddress().getStreet()).isEqualTo("Минский бульвар");
        Assertions.assertThat(orderWarehouse.getAddress().getHouse()).isEqualTo("12");
        Assertions.assertThat(orderWarehouse.getAddress().getFloor()).isEqualTo(5);
        Assertions.assertThat(orderWarehouse.getAddress().getLatitude()).isEqualTo("53.123456");
        Assertions.assertThat(orderWarehouse.getAddress().getLongitude()).isEqualTo("38.234567");


    }
}
