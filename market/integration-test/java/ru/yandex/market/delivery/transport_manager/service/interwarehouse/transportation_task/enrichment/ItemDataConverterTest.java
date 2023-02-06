package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.enrichment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.service.external.mdm.ItemDataReceiver;
import ru.yandex.market.delivery.transport_manager.service.external.mdm.dto.ItemEnrichment;
import ru.yandex.market.delivery.transport_manager.service.external.mdm.dto.ItemRequest;
import ru.yandex.market.mdm.http.tm.TmEnrichmentData;
import ru.yandex.market.mdm.http.tm.TmWeightDimensionsInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemDataConverterTest {

    private static final Set<ItemRequest> ITEM_REQUEST = Set.of(new ItemRequest(1, "10"));

    private final ItemDataReceiver itemDataReceiver = mock(ItemDataReceiver.class);
    private final ItemMdmDataConverter itemMdmDataConverter = new ItemMdmDataConverter(itemDataReceiver);

    @BeforeEach
    void initMock() {
        var mockEnrichment = List.of(TmEnrichmentData.newBuilder()
            .setShopSku("10")
            .setSupplierId(1)
            .setWeightDimensionsInfo(TmWeightDimensionsInfo.newBuilder()
                    // микрометры 100см х 50см х 33см
                    .setBoxHeightUm(1_000_000)
                    .setBoxLengthUm(500_000)
                    .setBoxWidthUm(330_000)
                    // миллиграммы, 6.625 кг
                    .setWeightGrossMg(6_625_000)
                    .setWeightNetMg(5_000_000)
                    .setWeightTareMg(1_625_000)
                    .build())
            .build())
            .stream().collect(Collectors.toMap(
                elem -> new ItemRequest(1, "10"),
                Function.identity()
            ));

        when(itemDataReceiver.searchSskuMasterData(ITEM_REQUEST)).thenReturn(mockEnrichment);
    }

    @Test
    void testKorobyteConversion() {
        Map<ItemRequest, ItemEnrichment> search = itemMdmDataConverter.getMdmData(ITEM_REQUEST);
        ItemEnrichment itemData = search.values()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Mock failed to return data"));
        Assertions.assertThat(itemData.getHeight()).isEqualTo(100);
        Assertions.assertThat(itemData.getLength()).isEqualTo(50);
        Assertions.assertThat(itemData.getWidth()).isEqualTo(33);
        Assertions.assertThat(itemData.getWeightNet()).isEqualTo(new BigDecimal("5.000"));
        Assertions.assertThat(itemData.getWeightTare()).isEqualTo(new BigDecimal("1.625"));
        Assertions.assertThat(itemData.getWeightGross()).isEqualTo(new BigDecimal("6.625"));
    }
}
