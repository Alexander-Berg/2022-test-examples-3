package ru.yandex.market.ff4shops.partner;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet
class PartnerOffersImporterServiceTest extends FunctionalTest {
    private static final long SUPPLIER_ID = 777;

    @Autowired
    private PartnerOffersImporterService importer;

    @Test
    @DisplayName("Проверка удаления, что должно так же вызвать update rownum")
    @DbUnitDataSet(before = "importSupplierOffers.before.csv", after =
            "importSupplierOffers.delete.after.csv")
    void testDelete() {
        QueryCountHolder.clear();

        importer.importOffers(SUPPLIER_ID, getSkus(List.of("sku2", "sku4")));

        assertEquals(0, QueryCountHolder.getGrandTotal().getInsert());
        assertEquals(1, QueryCountHolder.getGrandTotal().getUpdate());
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());
        assertEquals(0, QueryCountHolder.getGrandTotal().getDelete());
    }

    @Test
    @DisplayName("Проверка добавления и удаления")
    @DbUnitDataSet(before = "importSupplierOffers.before.csv", after =
            "importSupplierOffers.add-delete.after.csv")
    void testAddDelete() {
        QueryCountHolder.clear();

        importer.importOffers(SUPPLIER_ID, getSkus(List.of(
                "sku-new1",
                "sku-new2",
                "sku-new3",
                "sku-new4",
                "sku2",
                "sku4")
        ));

        assertEquals(1, QueryCountHolder.getGrandTotal().getInsert());
        assertEquals(2, QueryCountHolder.getGrandTotal().getUpdate());
        assertEquals(2, QueryCountHolder.getGrandTotal().getSelect());
        assertEquals(0, QueryCountHolder.getGrandTotal().getDelete());
    }

    @Test
    @Disabled
    @DisplayName("Проверка восстановления при пропуске rowNum в базе")
    @DbUnitDataSet(before = "importSupplierOffers.recover_gap.before.csv",
            after = "importSupplierOffers.recover_gap.after.csv")
    void testRecoverFromRowNumGap() {
        QueryCountHolder.clear();

        importer.importOffers(SUPPLIER_ID, getSkus(List.of("sku1", "sku2", "sku3", "sku4", "sku5")));

        assertEquals(0, QueryCountHolder.getGrandTotal().getInsert());
        assertEquals(1, QueryCountHolder.getGrandTotal().getUpdate());
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());
        assertEquals(0, QueryCountHolder.getGrandTotal().getDelete());
    }

    @Test
    @Disabled
    @DisplayName("Проверка восстановления при дубликате rowNum в базе")
    @DbUnitDataSet(before = "importSupplierOffers.recover_duplicate.before.csv",
            after = "importSupplierOffers.recover_duplicate.after.csv")
    void testRecoverFromRowNumDuplicate() {
        QueryCountHolder.clear();

        importer.importOffers(SUPPLIER_ID, getSkus(List.of("sku1", "sku2", "sku3", "sku4", "sku5")));

        assertEquals(0, QueryCountHolder.getGrandTotal().getInsert());
        assertEquals(1, QueryCountHolder.getGrandTotal().getUpdate());
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());
        assertEquals(0, QueryCountHolder.getGrandTotal().getDelete());
    }

    private Set<String> getSkus(List<String> shopSkus) {
        return new LinkedHashSet<>(shopSkus);
    }
}
