package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;

public class CheckouterSetOrderStatusTransformerTest extends MockContextualTest {
    @Autowired
    private CheckouterSetOrderStatusTransformer transformer;

    @Test
    public void serializationWorksAsExpected() {
        var payload = new SetOrderStatusDto(1111L, OrderStatus.DELIVERY, null);

        Assertions.assertThat(transformer.toObject(transformer.fromObject(payload)))
            .as("Serialized and deserialize")
            .isEqualTo(payload);
    }
}
