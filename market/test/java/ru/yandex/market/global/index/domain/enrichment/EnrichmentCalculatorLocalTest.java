package ru.yandex.market.global.index.domain.enrichment;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.index.BaseLocalTest;
import ru.yandex.market.global.index.domain.category.CategoryIndexSupplier;
import ru.yandex.market.global.index.domain.offer.CategoryEnrichmentCacheDtoIndexSupplier;
import ru.yandex.market.global.index.domain.offer.ShopEnrichmentCacheDtoIndexSupplier;
import ru.yandex.market.global.index.domain.shop.ShopIndexSupplier;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EnrichmentCalculatorLocalTest extends BaseLocalTest {

    private final EnrichmentCalculator enrichmentCalculator;
    private final IndexingService indexingService;
    private final ShopIndexSupplier shopIndexSupplier;
    private final CategoryIndexSupplier categoryIndexSupplier;

    @Test
    public void testRun() {
        EnrichmentContext run = enrichmentCalculator.calculate();
        System.out.println(run.getShopCache());
    }

    @Test
    public void testRun2() {
        EnrichmentContext cache = enrichmentCalculator.calculate();

        indexingService.reindex(new ShopEnrichmentCacheDtoIndexSupplier(cache.getShopCache()));
        indexingService.reindex(shopIndexSupplier);
    }

    @Test
    public void testRun3() {
        EnrichmentContext cache = enrichmentCalculator.calculate();

        indexingService.reindex(new CategoryEnrichmentCacheDtoIndexSupplier(cache.getCategoryCache()));
        indexingService.reindex(categoryIndexSupplier);
    }
}
