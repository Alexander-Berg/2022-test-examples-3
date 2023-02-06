package ru.yandex.market.global.index.domain.category;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.index.BaseLocalTest;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CategoryIndexSuppliyerLocalTest extends BaseLocalTest {
    private final CategoryIndexSupplier categoryIndexSupplier;
    private final IndexingService indexingService;

    @Test
    public void testReindexAll() {
        indexingService.reindex(categoryIndexSupplier);
    }
}
