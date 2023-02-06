package ru.yandex.market.mboc.common.erp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.erp.model.ErpMapping;
import ru.yandex.market.mboc.common.erp.model.ErpObject;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author yuramalinov
 * @created 05.06.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ErpMappingExporterServiceTest extends BaseIntegrationTestClass {
    @Value("${mboc.beru.supplierId}")
    int beruId;
    @Autowired
    private ErpMappingExporterService erpMappingExporterService;
    @Qualifier("erpJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Qualifier("erpTransactionTemplate")
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SupplierRepository supplierRepository;
    private TransactionStatus transaction;

    @Before
    public void begin() {
        transaction = transactionTemplate.getTransactionManager().getTransaction(transactionTemplate);
    }

    @After
    public void rollback() {
        transactionTemplate.getTransactionManager().rollback(transaction);
    }

    @Test
    public void testCorrectExport() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/erp-mappings.yml");
        Supplier supplier = new Supplier(42, "Some 1P Supplier", "1p.ru", "!!")
            .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("ERP2");
        supplierRepository.insert(supplier);
        Map<Integer, Supplier> suppliersMap = ImmutableMap.of(supplier.getId(), supplier);

        erpMappingExporterService.exportMappingsToErp(offers, suppliersMap);

        List<Map<String, Object>> data = jdbcTemplate.queryForList("select * from RSSKU_TO_MSKU order by MSKU");
        assertThat(data).hasSize(2);
        Map<String, Object> firstOffer = data.get(0);

        assertEquals(map(
            "RS_ID", "ERP2",
            "SSKU", "ERP2.sku1",
            "RSSKU", "sku1",
            "MSKU", 101010L,
            "TITLE", "Напильники для PGaaS! (searcH)",
            "BARCODES", "12344",
            "DELETED", false,
            "MOD_TS", new Timestamp(LocalDateTime.of(2017, 10, 28, 10, 15, 20)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
            "EXPORT_TS", firstOffer.get("EXPORT_TS"),
            "IMPORT_TS", null,
            "ID", firstOffer.get("ID"), // No matter what id
            "IMPORT_STATUS", 0,
            "SUPPLIER_ID", (long) beruId,
            "STATUS", "ACTIVE",
            "CATEGORY_ID", 12,
            "VENDOR_ID", 13,
            "VENDOR_NAME", "vendor"
        ), firstOffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApprovedMappingIsRequired() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/erp-mappings.yml");
        offers.get(1).updateApprovedSkuMapping(null, null);
        Supplier supplier = new Supplier(42, "Some 1P Supplier", "1p.ru", "!!")
            .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("ERP2");
        supplierRepository.insert(supplier);
        Map<Integer, Supplier> suppliersMap = ImmutableMap.of(supplier.getId(), supplier);

        erpMappingExporterService.exportMappingsToErp(offers, suppliersMap);
    }

    @Test
    public void testDeletedFlagSetToTrue() {
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/erp-mappings.yml");
        Offer offer = offers.get(0);
        offer.updateApprovedSkuMapping(Offer.Mapping.updateIfChanged(offer.getApprovedSkuMapping(), 0), null);

        Supplier supplier = new Supplier(42, "Some 1P Supplier", "1p.ru", "!!")
            .setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("ERP2");
        supplierRepository.insert(supplier);
        Map<Integer, Supplier> supplierMap = ImmutableMap.of(supplier.getId(), supplier);

        List<ErpMapping> erpMappings = erpMappingExporterService.exportMappingsToErp(offers, supplierMap);
        Assertions.assertThat(erpMappings)
            .extracting(ErpObject::isDeleted)
            .containsExactly(true, false);
    }

    private Map<String, Object> map(Object... items) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < items.length; i += 2) {
            result.put((String) items[i], items[i + 1]);
        }
        return result;
    }
}
