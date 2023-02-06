package ru.yandex.market.mbisfintegration.importer.yt;

import java.time.LocalDate;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.market.mbisfintegration.converters.impl.yt.SupplierStateB2bPartnerYtConverter;
import ru.yandex.market.mbisfintegration.datapreparation.impl.YtAccountPreparationService;
import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.RecordType;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;
import ru.yandex.market.mbisfintegration.salesforce.SfDate;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType.SUPPLIER;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 24.03.2022
 */
public class SupplierStateB2bPartnerYtImportTest extends AbstractYtImportTest {

    private static final long IMPORTED_SUPPLIER_ID = 637936L;

    @Autowired
    YtAccountPreparationService ytAccountPreparationService;

    @Autowired
    YtClient ytClientMock;

    @Autowired
    YtMockHelper ytMockHelper;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        entityClass = Account.class;
        entityType = SUPPLIER;
        converter = new SupplierStateB2bPartnerYtConverter();
        importConfig = new ImportConfiguration(entityClass, "uri", "supplier_id", entityType);

        ytMockHelper.mockSelectSingleRow(
                "where supplier_type != '0'",
                "supplier_id", new YTreeIntegerNodeImpl(true, IMPORTED_SUPPLIER_ID, Map.of()),
                "supplier_advertising_strategies_flag", new YTreeBooleanNodeImpl(true, Map.of()),
                "supplier_loyalty_program_flag", new YTreeBooleanNodeImpl(true, Map.of()),
                "participation_in_promotions_status", new YTreeBooleanNodeImpl(true, Map.of()),
                "business_direct_bonuses_flag", new YTreeBooleanNodeImpl(true, Map.of()),
                "channel_name", new YTreeStringNodeImpl("Agencies only", Map.of()),
                "agency_id", new YTreeIntegerNodeImpl(true, 5L, Map.of()),
//                "agency_name", new YTreeStringNodeImpl("Q W", Map.of()),
//                "agency_manager_name", new YTreeStringNodeImpl("F A", Map.of()),
                "supplier_total_delivered_orders_cnt", new YTreeIntegerNodeImpl(true, 11L, Map.of()),
                "supplier_total_delivered_gmv_rub_numeric", new YTreeIntegerNodeImpl(true, 12L, Map.of()),
                "last_30_days_cpa_auction_promotion_incl_vat_rub_numeric", new YTreeIntegerNodeImpl(true, 13L
                        , Map.of()),
                "cpa_auction_promotion_incl_vat_rub_numeric", new YTreeIntegerNodeImpl(true, 14L, Map.of()),
                "supplier_active_flag", new YTreeBooleanNodeImpl(true, Map.of()),
                "supplier_active_assortment_cnt", new YTreeIntegerNodeImpl(true, 15L, Map.of()),
                "supplier_hidden_offers_cnt", new YTreeIntegerNodeImpl(true, 16L, Map.of()),
                "warehouse_federal_district_name", new YTreeStringNodeImpl("Центральный федеральный округ", Map.of()),
                "warehouse_city_name", new YTreeStringNodeImpl("Москва", Map.of()),
                "survey_csi_score_code", new YTreeIntegerNodeImpl(true, 17L, Map.of()),
                "last_offer_date", new YTreeStringNodeImpl("2021-04-18", Map.of()),
                "first_order_delivered_date", new YTreeStringNodeImpl("2021-04-19", Map.of()),
                "supplier_last_30_days_delivered_gmv_rub_numeric", new YTreeDoubleNodeImpl(12.34, Map.of()),
                "first_express_offer_date", new YTreeStringNodeImpl("2022-05-22", Map.of()),
                "first_offer_date", new YTreeStringNodeImpl("2022-05-23", Map.of()),
                "wizard_org_type", new YTreeStringNodeImpl("СМЗ", Map.of())
        );

        ytImporter = new SupplierStateB2bPartnerYtImport(ytClientMock, ytAccountPreparationService,
                configurationService);
    }

    @Test
    void readAndConvertOneRow() {
        entityService.add(
                new Entity(IMPORTED_SUPPLIER_ID,
                        entityType,
                        null,
                        new Account().withSupplierIDC((double) IMPORTED_SUPPLIER_ID)
                )
        );
        ytImporter.doImport(converter);
        Assertions.assertThat(this.<Account>findEntityData(IMPORTED_SUPPLIER_ID))
                .isEqualTo(
                        new Account()
                                .withSupplierIDC((double) IMPORTED_SUPPLIER_ID)
                                .withLoyaltyProgramC(true)
                                .withSalesC(true)
                                .withDirectBonusesC(true)
                                .withChannelTypeC("Агент")
                                .withAgencyIDC(5.0)
                                .withDeliveredOrdersAllTimeC(11.0)
                                .withGMVC(12.0)
                                .withCPAAuctionPromotionSpend30DaysC(13.0)
                                .withCPAAuctionPromotionSpendingsOverallC(14.0)
                                .withActiveC(true)
                                .withDisplayedOffersCountC(15.0)
                                .withHiddenOffersCountC(16.0)
                                .withPSATC(17.0)
                                .withRecordType(new RecordType().withName("Supplier"))
                                .withWarehouseFederalDistrictC("Центральный федеральный округ")
                                .withWarehouseCityC("Москва")
                                .withLastOfferDateC(SfDate.parse("2021-04-18", ISO_LOCAL_DATE, LocalDate::from))
                                .withFirstOrderDeliveredDateC(SfDate.parse("2021-04-19", ISO_LOCAL_DATE,
                                        LocalDate::from))
                                .withGMV30DayC(12.34)
                                .withExpressFirstDateOfferC(SfDate.parse("2022-05-22", ISO_LOCAL_DATE, LocalDate::from))
                                .withFirstOfferDateC(SfDate.parse("2022-05-23", ISO_LOCAL_DATE, LocalDate::from))
                                .withSelfC(true)
                );
    }
}
