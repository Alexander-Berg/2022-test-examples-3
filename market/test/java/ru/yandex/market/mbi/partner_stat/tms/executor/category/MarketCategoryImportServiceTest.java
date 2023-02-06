package ru.yandex.market.mbi.partner_stat.tms.executor.category;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.entity.MarketCategoryEntity;
import ru.yandex.market.mbi.partner_stat.tms.executor.category.service.MarketCategoryImportService;
import ru.yandex.market.mbi.partner_stat.yt.TestYQLReaderDataSupplier;

/**
 * Тесты для {@link MarketCategoryImportService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class MarketCategoryImportServiceTest extends FunctionalTest {

    @Autowired
    private TestYQLReaderDataSupplier testYQLReaderDataSupplier;

    @Autowired
    private MarketCategoryImportService marketCategoryImportService;

    @Test
    @DisplayName("Импортирование маркетных категорий из MBO")
    @DbUnitDataSet(
            before = "MarketCategoryImportService/testImport/before.csv",
            after = "MarketCategoryImportService/testImport/after.csv"
    )
    void testImport() {
        final var categories = ImmutableList.of(
                createCategory(1L, -1L, "Все категории", true, false),
                createCategory(2L, 1L, "Подкатегория 1", true, true),
                createCategory(3L, 1L, "Подкатегория 2", false, true),
                createCategory(4L, 5L, "Подкатегория 5.1", true, false),
                createCategory(5L, 2L, "Подкатегория 2.1", true, true)
        );
        Mockito.doReturn(categories).when(testYQLReaderDataSupplier).get();

        marketCategoryImportService.importCategories();
    }

    private static Pair<Long, MarketCategoryEntity> createCategory(
            final long hid,
            final long parentHid,
            final String name,
            final boolean published,
            final boolean leaf
    ) {
        final MarketCategoryEntity entity = new MarketCategoryEntity();
        entity.setHid(hid);
        entity.setName(name);
        entity.setPublished(published);
        entity.setLeaf(leaf);

        return Pair.of(parentHid, entity);
    }
}
