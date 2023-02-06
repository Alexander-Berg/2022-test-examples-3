package ru.yandex.market.global.index.domain.offer;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.index.BaseLocalTest;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OffersSupplierLocalTest extends BaseLocalTest {
    private final OfferIndexSupplier offerIndexSupplier;
    private final IndexingService indexingService;

    @Test
    public void testReindexAll() {
        indexingService.reindex(offerIndexSupplier);
    }

    @Test
    public void testReindexShop() {
        indexingService.index(offerIndexSupplier.getShopOffersSupplier(11379949L));
    }
}
