package ru.yandex.market.mboc.common.services.mstat.yt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mboc.common.config.mstat.MstatOfferTableConfig;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer.AcceptanceStatus;
import ru.yandex.market.mboc.common.offers.model.Offer.ServiceOffer;
import ru.yandex.market.mboc.common.services.mstat.ExportContext;
import ru.yandex.market.mboc.common.services.mstat.MstatOfferState;
import ru.yandex.market.mboc.common.services.mstat.SnapShotContext;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author apluhin
 * @created 2/19/21
 */
public class MstatOffersExporterTest {


    MstatOffersExporter expandedExporter;

    @Before
    public void setUp() throws Exception {
        MstatOfferTableConfig mstatOfferTableConfig = new MstatOfferTableConfig();
        ReflectionTestUtils.setField(mstatOfferTableConfig, "offerPath", "//test/path");
        expandedExporter = new MstatOffersExporter(null, null,
            mstatOfferTableConfig, new StorageKeyValueServiceMock());
    }

    @Test
    public void testExtractKeys() {
        MstatOfferState offerState = MstatOfferState.builder()
            .shopSku("shop-sku-name")
            .businessId(1)
            .serviceOffers(List.of(
                new ServiceOffer(1),
                new ServiceOffer(2)
            ))
            .build();
        Assertions.assertThat(expandedExporter.extractKeys(offerState)).isEqualTo(
            Arrays.asList(
                Map.of(
                    "shop_sku", "shop-sku-name",
                    "supplier_id", 1
                ),
                Map.of(
                    "shop_sku", "shop-sku-name",
                    "supplier_id", 2
                )
            )
        );
    }

    @Test
    public void testExportContextCreation() {
        SnapShotContext context = expandedExporter.prepareContext();
        LocalDate prevDay = LocalDate.now().minusDays(1);
        Assertions.assertThat(context.destinationTable).isEqualTo("//test/" + prevDay);
        Assertions.assertThat(context.sql).isEqualTo(
            "INSERT INTO `//test/tmp_table` (        " +
                "id,         " +
                "supplier_id,         " +
                "shop_sku,         " +
                "approved_sku_mapping_id,         " +
                "vendor_id,         " +
                "category_id,         " +
                "tracker_ticket,         " +
                "is_base_offer,         " +
                "offer_destination,         " +
                "business_id,         " +
                "approved_sku_mapping_confidence,         " +
                "mapped_model_id,         " +
                "mapped_model_confidence,         " +
                "mapped_category_confidence,         " +
                "availability,         " +
                "processing_status,         " +
                "`date`     ) SELECT id, supplier_id, shop_sku, approved_sku_mapping_id, vendor_id, " +
                "category_id, tracker_ticket, is_base_offer, offer_destination, business_id, " +
                "approved_sku_mapping_confidence, mapped_model_id, mapped_model_confidence, " +
                "mapped_category_confidence, availability, processing_status, '" + prevDay +
                "' as `date`from `//test/path`;"
        );
    }

    @Test
    public void testBaseOfferSelect() {
        var suppliers = List.of(
            new Supplier().setId(0).setFulfillment(true),
            new Supplier().setId(1).setCrossdock(true),
            new Supplier().setId(2).setDropship(true),
            new Supplier().setId(3).setDropshipBySeller(true),
            new Supplier().setId(4),
            new Supplier().setId(5).setFulfillment(true),
            new Supplier().setId(6).setCrossdock(true),
            new Supplier().setId(7).setDropship(true),
            new Supplier().setId(8).setDropshipBySeller(true),
            new Supplier().setId(9)
        );
        // Present with fulfillment
        testBaseOfferSelectCase(suppliers, 0);
        // Present with crossdock
        suppliers.get(0).setFulfillment(false);
        suppliers.get(5).setFulfillment(false);
        testBaseOfferSelectCase(suppliers, 1);
        // Present with dropship
        suppliers.get(1).setCrossdock(false);
        suppliers.get(6).setCrossdock(false);
        testBaseOfferSelectCase(suppliers, 2);
        // Present with dropship by seller
        suppliers.get(2).setDropship(false);
        suppliers.get(7).setDropship(false);
        testBaseOfferSelectCase(suppliers, 3);
        // No specials
        suppliers.get(3).setDropshipBySeller(false);
        suppliers.get(8).setDropshipBySeller(false);
        testBaseOfferSelectCase(suppliers, 0);
    }

    private void testBaseOfferSelectCase(List<Supplier> suppliers, int expectedSupplierIndex) {
        Supplier expectedSupplier = suppliers.get(expectedSupplierIndex);
        var offers = List.of(
            OfferTestUtils.simpleOffer(1)
                .setShopSku("ssku")
                .setServiceOffers(List.of(new ServiceOffer(
                    expectedSupplier.getId(),
                    MbocSupplierType.BUSINESS,
                    AcceptanceStatus.TRASH
                )))
                .addNewServiceOfferIfNotExistsForTests(suppliers)
        );
        var context = ExportContext.builder()
            .offers(offers)
            .suppliers(suppliers.stream().collect(Collectors.toMap(Supplier::getId, it -> it, (i1, i2) -> i1)))
            .mskus(Map.of()).build();

        var result = new ArrayList<Map<String, Object>>();
        expandedExporter.convertEntityToYt(offers, context, result::add);

        Assertions.assertThat(result).hasSize(suppliers.size());

        Map<String, Object> expected = result.stream()
            .filter(m -> m.get(MstatOfferTableConfig.SUPPLIER_ID.getName()).equals((long) expectedSupplier.getId()))
            .findAny().orElse(null);
        Assertions.assertThat(expected).isNotNull();

        result.forEach(state -> {
            if (state != expected) {
                Assertions.assertThat(state.get(MstatOfferTableConfig.IS_BASE_OFFER.getName())).isEqualTo(false);
            }
            Assertions.assertThat(state.get(MstatOfferTableConfig.BASE_ACCEPTANCE_STATUS.getName()))
                .isEqualTo(AcceptanceStatus.TRASH.name());
        });
        Assertions.assertThat(expected.get(MstatOfferTableConfig.IS_BASE_OFFER.getName())).isEqualTo(true);
    }

    @Test
    public void testNotFailsWhenNoServiceOffers() {
        var offers = List.of(
            OfferTestUtils.simpleOffer(1)
                .setServiceOffers(List.of())
                .setShopSku("ssku")
        );
        var context = ExportContext.builder()
            .offers(offers)
            .suppliers(Map.of(324, new Supplier().setId(324)))
            .mskus(Map.of()).build();

        expandedExporter.convertEntityToYt(offers, context, ignored -> {
        });
    }
}
