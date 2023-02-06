package ru.yandex.market.tpl.core.domain.order.batch;

import java.util.Collection;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.util.TplCoreTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OrderBatchScDtoMapperTest {

    public static final String SINGLE_PLACE_ORDER_YANDEX_ID = "1";
    public static final int BATCH_COUNT = 2;

    @DisplayName("Сериализация батчей многоместных заказов")
    @Test
    @SneakyThrows
    public void serializeOrderBatchScDtoTest() {
        OrderBatchesScDto batchesScDto = TplCoreTestUtils.mapFromResource("/batch/valid_order_batch_registry_sc_info.json",
                OrderBatchesScDto.class);

        assertNotNull(batchesScDto);
        assertEquals(batchesScDto.getBatches().size(), BATCH_COUNT);
        assertNotNullAllFields(batchesScDto);
    }

    @DisplayName("Сериализация батча с одноместным заказом")
    @Test
    @SneakyThrows
    public void serializeOrderBatchScWithSingleOrderPLaceTest() {
        OrderBatchesScDto batchesScDto = TplCoreTestUtils.mapFromResource("/batch/valid_order_batch_registry_sc_with_single_order_place_info.json",
                OrderBatchesScDto.class);


        assertNotNull(batchesScDto);
        assertEquals(batchesScDto.getBatches().size(), BATCH_COUNT);
        OrderBatchesScDto.BoxBatchSc singleOrderPlace = getSingleOrderPLace(batchesScDto);
        assertNull(singleOrderPlace.getBarcode());
        assertNotNull(singleOrderPlace.getOrderYandexId());
        assertEquals(singleOrderPlace.getOrderYandexId(), SINGLE_PLACE_ORDER_YANDEX_ID);
    }

    private OrderBatchesScDto.BoxBatchSc getSingleOrderPLace(OrderBatchesScDto batchesScDto) throws Exception {
        return batchesScDto.getBatches().stream()
                .map(OrderBatchesScDto.Batch::getBoxes)
                .flatMap(Collection::stream)
                .filter(boxBatchSc -> boxBatchSc.getBarcode() == null)
                .findFirst()
                .orElseThrow(Exception::new);
    }

    private void assertNotNullAllFields(OrderBatchesScDto batchesScDto) {
        batchesScDto.getBatches()
                .forEach(batch -> {
                    assertNotEmpty(batch.getBarcode());
                    assertNotNull(batch.getBoxes());
                    batch.getBoxes().forEach(boxBatchSc -> {
                        assertNotNull(boxBatchSc);
                        assertNotEmpty(boxBatchSc.getBarcode());
                        assertNotEmpty(boxBatchSc.getOrderYandexId());
                    });
                });
    }

    private void assertNotEmpty(String value) {
        Assertions.assertNotNull(value);
        Assertions.assertFalse(value.isEmpty());
    }
}
