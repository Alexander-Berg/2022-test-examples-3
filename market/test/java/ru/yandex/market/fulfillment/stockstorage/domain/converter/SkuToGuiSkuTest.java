package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SkuDetailDto;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SkuGridDto;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.EnrichedSku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.SkuSyncAudit;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaEnrichedSkuRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaSkuRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaStockFreezeRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaStockRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaUnfreezeJobRepository;
import ru.yandex.market.fulfillment.stockstorage.service.frontend.SkuFrontService;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;

import static org.junit.Assert.assertEquals;

public class SkuToGuiSkuTest {

    private final SkuFrontService skuFrontService = new SkuFrontService(
            Mockito.mock(ReplicaSkuRepository.class),
            Mockito.mock(ReplicaStockFreezeRepository.class),
            Mockito.mock(ReplicaUnfreezeJobRepository.class),
            Mockito.mock(ReplicaStockRepository.class),
            Mockito.mock(SystemPropertyService.class),
            Mockito.mock(ReplicaEnrichedSkuRepository.class));

    private EnrichedSku getEnrichedSkuWithDifferentSizes() {
        EnrichedSku sku = new EnrichedSku();
        sku.setUnitId(new UnitId("Sku", 1L, 1));
        sku.setEnabled(true);
        sku.setUpdatable(true);
        sku.setLifetime(123);
        sku.setAdditionalContent("AdditionalContent");
        sku.setSkuSyncAudit(new SkuSyncAudit().setSkuId(1L).setSynced(LocalDateTime.of(2020, 1, 1, 1, 1, 1)));
        return sku;
    }

    private Sku getSkuWithDifferentSizes() {
        Sku sku = new Sku();
        sku.setUnitId(new UnitId("Sku", 1L, 1));
        sku.setEnabled(true);
        sku.setUpdatable(true);
        sku.setLifetime(123);
        sku.setAdditionalContent("AdditionalContent");

        return sku;
    }


    @Test
    public void testConvertToSkuDetail() {

        EnrichedSku sku = getEnrichedSkuWithDifferentSizes();
        UnitId unitId = sku.getUnitId();
        SkuDetailDto detailDto = skuFrontService.toSkuDetailDto(sku);

        assertEquals(unitId.getSku(), detailDto.getSku());
        assertEquals(unitId.getVendorId().longValue(), detailDto.getVendorId());
        assertEquals(unitId.getWarehouseId().intValue(), detailDto.getWarehouseId());

        assertEquals(sku.isEnabled(), detailDto.isEnabled());
        assertEquals(sku.isUpdatable(), detailDto.isUpdatable());
        assertEquals(sku.getAdditionalContent(), detailDto.getAdditionalContent());
        assertEquals(sku.getSkuSyncAudit().getSynced(), LocalDateTime.of(2020, 1, 1, 1, 1, 1));
    }


    @Test
    public void testConvertToSkuGrid() {
        Sku sku = getSkuWithDifferentSizes();
        UnitId unitId = sku.getUnitId();
        SkuGridDto gridDto = skuFrontService.toSkuGridDto(sku, Mockito.mock(Stock.class));

        assertEquals(unitId.getSku(), gridDto.getSku());
        assertEquals(unitId.getVendorId().longValue(), gridDto.getVendorId());
        assertEquals(unitId.getWarehouseId().intValue(), gridDto.getWarehouseId());

    }

}
