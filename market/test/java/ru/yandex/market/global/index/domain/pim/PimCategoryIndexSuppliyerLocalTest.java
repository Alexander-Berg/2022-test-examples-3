package ru.yandex.market.global.index.domain.pim;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.index.BaseLocalTest;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PimCategoryIndexSuppliyerLocalTest extends BaseLocalTest {
    private final PimCategoriesIndexSupplier pimCategoriesIndexSupplier;
    private final IndexingService indexingService;

    @Test
    public void testReindexAll() {
        indexingService.reindex(pimCategoriesIndexSupplier);
    }
}
