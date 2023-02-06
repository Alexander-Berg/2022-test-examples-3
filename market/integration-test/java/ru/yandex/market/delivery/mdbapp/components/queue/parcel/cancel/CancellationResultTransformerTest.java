package ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancelResultDto;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancellationResultQueueTransformer;
import ru.yandex.market.logistics.lom.model.dto.ChangedItemDto;
import ru.yandex.market.logistics.lom.model.dto.MissingItemsCancellationOrderRequestReasonDetailsDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;

public class CancellationResultTransformerTest extends AllMockContextualTest {
    @Autowired
    private CancellationResultQueueTransformer transformer;

    @Test
    public void serializationWorksAsExpected() {
        var payload = new CancelResultDto(
            1111L,
            CancellationOrderStatus.CREATED,
            CancellationOrderReason.MISSING_ITEM,
            new MissingItemsCancellationOrderRequestReasonDetailsDto()
                .setItems(List.of(
                    ChangedItemDto.builder()
                        .article("article")
                        .vendorId(1L)
                        .count(1L)
                        .build()
                ))
        );

        Assertions.assertThat(transformer.toObject(transformer.fromObject(payload)))
            .as("Serialized and deserialize")
            .isEqualTo(payload);
    }

    @Test
    public void serializationWithoutReasonWorksAsExpected() {
        var payload = new CancelResultDto(
            1111L,
            CancellationOrderStatus.CREATED,
            null,
            null
        );

        Assertions.assertThat(transformer.toObject(transformer.fromObject(payload)))
            .as("Serialized and deserialize")
            .isEqualTo(payload);
    }
}
