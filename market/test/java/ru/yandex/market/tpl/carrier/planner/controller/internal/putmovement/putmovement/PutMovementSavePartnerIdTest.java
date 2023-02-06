package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.Movement;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.request.PutMovementRequest;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PutMovementSavePartnerIdTest extends BasePlannerWebTest {

    private final PutMovementHelper putMovementHelper;
    private final MovementRepository movementRepository;

    @Test
    void shouldSavePartnerId() {
        ResourceId shipperLocationId = new ResourceId("20", "1234");
        ResourceId shipperId = new ResourceId("9000", null);
        ResourceId receiverLocationId = new ResourceId("200", "5678");
        ResourceId receiverId = new ResourceId("9001", null);


        Movement movement = Movement.builder(
                new ResourceId("TMM1", null),
                DateTimeInterval.fromFormattedValue("2021-03-03T20:00:00+03:00/2021-03-03T21:00:00+03:00"),
                BigDecimal.ONE
        )
                .setWeight(null)
                .setShipper(new Party(PutMovementControllerTestUtil.fromLocation(shipperLocationId), PutMovementControllerTestUtil.legalEntity(), shipperId))
                .setReceiver(new Party(PutMovementControllerTestUtil.toLocation(receiverLocationId), PutMovementControllerTestUtil.legalEntity(), receiverId))
                .setOutboundInterval(PutMovementControllerTestUtil.OUTBOUND_DEFAULT_INTERVAL)
                .setInboundInterval(PutMovementControllerTestUtil.INBOUND_DEFAULT_INTERVAL)
                .setComment(null)
                .setMaxPalletCapacity(1)
                .build();

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                new PutMovementRequest(movement, null)
        ));

        transactionTemplate.execute(tc -> {

            var saved = movementRepository.findByExternalId("TMM1").get();
            Assertions.assertThat(saved.getWarehouse().getPartner()).isNotNull();
            Assertions.assertThat(saved.getWarehouse().getPartner().getYandexId()).isEqualTo("9000");
            Assertions.assertThat(saved.getWarehouseTo().getPartner()).isNotNull();
            Assertions.assertThat(saved.getWarehouseTo().getPartner().getYandexId()).isEqualTo("9001");
            return null;
        });
    }
}
