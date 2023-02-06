package ru.yandex.market.delivery.mdbapp.components.queue.track.add.lgw;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.dto.AddTrackForLgwDsOrder;

public class AddTrackForLgwDsOrderTransformerTest extends MockContextualTest {

    @Autowired
    private AddTrackForLgwDsOrderTransformer transformer;

    @Test
    @DisplayName("Проверка корректной сериализации и десериализации")
    public void serializationWorksAsExpected() {
        AddTrackForLgwDsOrder addTrackForLgwDsOrder = AddTrackForLgwDsOrder.builder()
                .orderId(1L)
                .trackId("123")
                .build();

        Assertions.assertThat(transformer.toObject(transformer.fromObject(addTrackForLgwDsOrder)))
                .as("Serialized and deserialize")
                .isEqualTo(addTrackForLgwDsOrder);
    }
}
