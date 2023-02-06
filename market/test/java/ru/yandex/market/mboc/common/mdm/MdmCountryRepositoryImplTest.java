package ru.yandex.market.mboc.common.mdm;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author amaslak
 */
public class MdmCountryRepositoryImplTest extends BaseDbTestClass {

    private static final Logger log = LoggerFactory.getLogger(MdmCountryRepositoryImplTest.class);

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private MdmCountryRepositoryImpl mdmCountryRepository;

    @Autowired
    private OfferRepositoryImpl offerRepository;

    @Autowired
    private SupplierRepositoryImpl supplierRepository;

    private static final int SUPPLIER_ID = 1;

    @Before
    public void setUp() throws Exception {
        mdmCountryRepository = new MdmCountryRepositoryImpl(jdbcTemplate, transactionTemplate);
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));

    }

    @Test
    public void testMdmCountryInsertAnsSelect() {
        MdmCountryGeoIds mdmCountryDao = new MdmCountryGeoIds(
            SUPPLIER_ID, "Something-1123", List.of(1, 2, -57642), Instant.now()
        );

        List<MdmCountryGeoIds> before = mdmCountryRepository.findAll();
        Assertions.assertThat(before).isEmpty();

        mdmCountryRepository.insertOrUpdate(mdmCountryDao);

        List<MdmCountryGeoIds> after = mdmCountryRepository.findAll();
        Assertions.assertThat(after).containsExactly(mdmCountryDao);
    }


    /**
     * mbo_category.v_supplier_to_market_sku_snapshot should use geo_id from mdmCountryRepository
     */
    @Test
    public void testMdmCountryExportToVSupplierToMarketSkuSnapshot() {
        Offer testOffer = OfferTestUtils.simpleOffer()
            .setShopSku("Something-1123")
            .setUploadToYtStamp(1L)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(new Offer.Mapping(1L, DateTimeUtils.dateTimeNow()),
                Offer.MappingConfidence.CONTENT);

        Assertions.assertThat(offerRepository.insertOffer(testOffer)).isTrue();

        Integer supplierId = testOffer.getServiceOffersSuppliers().get(0);

        MdmCountryGeoIds mdmCountryDao = new MdmCountryGeoIds(
            supplierId, testOffer.getShopSku(), List.of(1, 2, -57642), DateTimeUtils.instantNow()
        );
        mdmCountryRepository.insertOrUpdate(mdmCountryDao);

        List<Map<String, Object>> offers = jdbcTemplate.getJdbcOperations().queryForList(
            "select * from mbo_category.v_supplier_to_market_sku_snapshot"
        );

        Assertions.assertThat(offers).hasSize(1);
        Map<String, Object> exportRow = offers.get(0);
        Assertions.assertThat(exportRow.get("shop_id")).isEqualTo(mdmCountryDao.getSupplierId());
        Assertions.assertThat(exportRow.get("shop_sku_id")).isEqualTo(mdmCountryDao.getShopSku());

        // expect jdbc int array concatenation
        String expectedMasterData = mdmCountryDao.getGeoIds().stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));
        Assertions.assertThat(exportRow.get("master_data")).isEqualTo(expectedMasterData);
    }
}
