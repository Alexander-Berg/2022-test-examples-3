package ru.yandex.market.core.supplier.category;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierCategoryServiceTest extends FunctionalTest {
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SupplierCategoryDao supplierCategoryDao;
    @Value("${yt.mbo.mboc_offers_expanded_sku.latest.table://home/dummy}")
    private String table;
    @Mock
    private Yt yt;
    @Mock
    private Cypress cypress;
    @Mock
    private YtTables ytTables;

    @DisplayName("Ежедневный импорт данных о связках поставщик - категории, в которых у него заведены товары в каталоге Парнерского Интерфейса")
    @Test
    @DbUnitDataSet(before = "SupplierCategoryServiceTest.before.csv", after = "SupplierCategoryServiceTest.after.csv")
    public void shouldUpsert() {
        when(yt.cypress()).thenReturn(cypress);
        YPath yPath = YPath.simple(table);
        when(cypress.exists(yPath)).thenReturn(true);
        when(yt.tables()).thenReturn(ytTables);

        Iterator<YTreeMapNode> ytRows = getYtRows();

        doAnswer((Answer<Void>) invocation -> {
            Consumer<YTreeMapNode> mapper = invocation.getArgument(2);
            while (ytRows.hasNext()) {
                mapper.accept(ytRows.next());
            }
            return null;
        }).when(ytTables).read(argThat(yPath::equals), argThat(arg -> arg.equals(YTableEntryTypes.YSON)), any(Consumer.class));

        var service = new SupplierCategoryService(yt, table, supplierCategoryDao, transactionTemplate);

        service.doImport();

        verify(yt).cypress();
        verify(cypress).exists(yPath);
        verify(yt).tables();
        verify(ytTables).read(argThat(yPath::equals), argThat(arg -> arg.equals(YTableEntryTypes.YSON)), any(Consumer.class));
    }

    private Iterator<YTreeMapNode> getYtRows() {
        YTreeMapNode row1 = mock(YTreeMapNode.class);
        YTreeMapNode row2 = mock(YTreeMapNode.class);
        YTreeMapNode row3 = mock(YTreeMapNode.class);
        YTreeMapNode row4 = mock(YTreeMapNode.class);
        YTreeMapNode row5 = mock(YTreeMapNode.class);
        when(row1.getLongO("supplier_id")).thenReturn(Optional.of(111111111111L));
        when(row1.getLongO("category_id")).thenReturn(Optional.of(111111111111L));
        when(row1.getLongO("approved_market_sku_id")).thenReturn(Optional.of(111111111111L));
        when(row1.getStringO("supplier_type")).thenReturn(Optional.of("THIRD_PARTY"));
        when(row2.getLongO("supplier_id")).thenReturn(Optional.of(222222222222L));
        when(row2.getLongO("category_id")).thenReturn(Optional.of(222222222222L));
        when(row2.getLongO("approved_market_sku_id")).thenReturn(Optional.empty());
        when(row2.getStringO("supplier_type")).thenReturn(Optional.of("THIRD_PARTY"));
        when(row3.getLongO("supplier_id")).thenReturn(Optional.of(333333333333L));
        when(row3.getLongO("category_id")).thenReturn(Optional.of(333333333333L));
        when(row3.getLongO("approved_market_sku_id")).thenReturn(Optional.of(333333333333L));
        when(row3.getStringO("supplier_type")).thenReturn(Optional.of("THIRD_PARTY"));
        when(row4.getLongO("supplier_id")).thenReturn(Optional.of(444444444444L));
        when(row4.getLongO("category_id")).thenReturn(Optional.of(444444444444L));
        when(row4.getLongO("approved_market_sku_id")).thenReturn(Optional.of(444444444444L));
        when(row4.getStringO("supplier_type")).thenReturn(Optional.of("MARKET_SHOP"));
        when(row5.getLongO("supplier_id")).thenReturn(Optional.of(555555555555L));
        when(row5.getLongO("category_id")).thenReturn(Optional.of(555555555555L));
        when(row5.getLongO("approved_market_sku_id")).thenReturn(Optional.of(555555555555L));
        when(row5.getStringO("supplier_type")).thenReturn(Optional.of("THIRD_PARTY"));
        return Stream.of(row1, row2, row3, row4, row5).collect(Collectors.toList()).iterator();
    }

}
