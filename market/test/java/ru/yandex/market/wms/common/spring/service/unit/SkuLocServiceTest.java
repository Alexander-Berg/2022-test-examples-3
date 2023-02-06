package ru.yandex.market.wms.common.spring.service.unit;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.enums.LocationType;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuLoc;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuLocDao;
import ru.yandex.market.wms.common.spring.service.SkuLocService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.createSku;

public class SkuLocServiceTest extends BaseTest {

    private static final String USER = "TEST";

    private SkuLocService skuLocService;
    private SkuLocDao skuLocDao;

    @BeforeEach
    public void setup() {
        super.setup();
        skuLocDao = mock(SkuLocDao.class);
        skuLocService = new SkuLocService(skuLocDao);
    }

    @Test
    public void insertOrAddWhenExists() {
        Sku sku = createSku();
        String loc = "STAGE";
        SkuId skuId = createSkuId(sku);
        when(skuLocDao.getExistingSkuId(skuId, loc)).thenReturn(Optional.of(skuId));
        skuLocService.insertOrAdd(skuId, loc, USER, BigDecimal.ONE);
        verify(skuLocDao).getExistingSkuId(skuId, loc);
        verify(skuLocDao).addToQty(skuId, loc, BigDecimal.ONE, USER);
        verifyNoMoreInteractions(skuLocDao);
    }

    @Test
    public void insertOrAddWhenNotExists() {
        Sku sku = createSku();
        String loc = "STAGE";
        SkuId skuId = createSkuId(sku);
        when(skuLocDao.getExistingSkuId(skuId, loc)).thenReturn(Optional.empty());
        skuLocService.insertOrAdd(skuId, loc, USER, BigDecimal.ONE);
        verify(skuLocDao).getExistingSkuId(skuId, loc);
        verify(skuLocDao).insert(Collections.singletonList(createSkuLoc(sku, loc)));
        verifyNoMoreInteractions(skuLocDao);
    }

    private SkuId createSkuId(Sku sku) {
        return new SkuId(sku.getStorerKey(), sku.getSku());
    }

    private SkuLoc createSkuLoc(Sku sku, String loc) {
        return SkuLoc.builder()
            .storerKey(sku.getStorerKey())
            .sku(sku.getSku())
            .loc(loc)
            .qty(BigDecimal.ONE)
            .qtyPicked(BigDecimal.ZERO)
            .locationType(LocationType.OTHER)
            .addWho(USER)
            .editWho(USER)
            .build();
    }
}
