package ru.yandex.market.mbo.mdm.common.datacamp;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 08/04/2021
 */
public class ServiceOfferMigrationServiceImplTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_ID_1 = 100;
    private static final int BUSINESS_ID_2 = 101;
    private static final int SUPPLIER_ID_1 = 200;
    private static final int SUPPLIER_ID_2 = 201;
    private static final int SUPPLIER_ID_3 = 202;

    private static final String SHOP_SKU = "тонна_шоколада";

    @Autowired
    private ServiceOfferMigrationService serviceOfferMigrationService;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    @Autowired
    private ServiceOfferMigrationRepository serviceOfferMigrationRepository;

    @Autowired
    private SilverSskuRepository silverSskuRepository;

    @Autowired
    private MdmParamCache paramCache;

    @Before
    public void before() {
        mdmSupplierRepository.insertBatch(
            createBusinessSupplier(BUSINESS_ID_1),
            createBusinessSupplier(BUSINESS_ID_2),
            createServiceSupplier(SUPPLIER_ID_1, BUSINESS_ID_1),
            createServiceSupplier(SUPPLIER_ID_2, BUSINESS_ID_1),
            createServiceSupplier(SUPPLIER_ID_3, BUSINESS_ID_2)
        );
    }

    @Test
    public void testNoMigrationRequest() {
        List<CommonSsku> sskus = List.of(createTestDatacampSsku(BUSINESS_ID_1, List.of(SUPPLIER_ID_1)));

        serviceOfferMigrationService.processOffers(sskus);

        Assertions.assertThat(serviceOfferMigrationRepository.findAll()).isEmpty();
        Assertions.assertThat(silverSskuRepository.findAll()).isEmpty();
    }

    @Test
    public void testMigrateServiceToEmptyBusinessWhileSourceBusinessHasOtherServices() {
        var migrationInfo = new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_ID_1)
            .setShopSku(SHOP_SKU)
            .setSrcBusinessId(BUSINESS_ID_1)
            .setDstBusinessId(BUSINESS_ID_2)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insert(migrationInfo);

        // create silver param values for service offer in source business
        CommonSsku origSsku = createTestDatacampSsku(BUSINESS_ID_1, List.of(SUPPLIER_ID_1, SUPPLIER_ID_2));
        SilverCommonSsku orgSilverSsku = commonSskuToSilverCommonSsku(origSsku);
        silverSskuRepository.insertOrUpdateSsku(orgSilverSsku);
        List<SskuSilverParamValue> sskuSilverParamValuesBefore = silverSskuRepository.findAll();

        // create incoming offer for target business
        List<CommonSsku> sskusFromDatacamp = List.of(createTestDatacampSsku(BUSINESS_ID_2, List.of(SUPPLIER_ID_1)));

        serviceOfferMigrationService.processOffers(sskusFromDatacamp);

        List<ServiceOfferMigrationInfo> updatedMigrationInfos = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(updatedMigrationInfos).hasSize(1);
        Assertions.assertThat(updatedMigrationInfos.get(0)).isEqualToIgnoringGivenFields(migrationInfo,
            "isProcessed", "processedTimestamp");
        Assertions.assertThat(updatedMigrationInfos.get(0).isProcessed()).isTrue();

        Assertions.assertThat(silverSskuRepository.findAll()).isEqualTo(sskuSilverParamValuesBefore);
    }

    @Test
    public void testMigrateServiceToNonEmptyBusinessWhileSourceBusinessHasOtherServices() {
        var migrationInfo = new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_ID_1)
            .setShopSku(SHOP_SKU)
            .setSrcBusinessId(BUSINESS_ID_1)
            .setDstBusinessId(BUSINESS_ID_2)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insert(migrationInfo);

        // create silver param values for service offer in source business
        CommonSsku origSsku1 = createTestDatacampSsku(BUSINESS_ID_1, List.of(SUPPLIER_ID_1, SUPPLIER_ID_2));
        CommonSsku origSsku2 = createTestDatacampSsku(BUSINESS_ID_2, List.of(SUPPLIER_ID_3));
        var silverCommonSskus = List.of(origSsku1, origSsku2).stream()
            .map(this::commonSskuToSilverCommonSsku)
            .collect(Collectors.toList());
        silverSskuRepository.insertOrUpdateSskus(silverCommonSskus);
        List<SskuSilverParamValue> sskuSilverParamValuesBefore = silverSskuRepository.findAll();

        // create incoming offer for target business
        List<CommonSsku> sskusFromDatacamp = List.of(createTestDatacampSsku(BUSINESS_ID_2, List.of(SUPPLIER_ID_1)));

        serviceOfferMigrationService.processOffers(sskusFromDatacamp);

        List<ServiceOfferMigrationInfo> updatedMigrationInfos = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(updatedMigrationInfos).hasSize(1);
        Assertions.assertThat(updatedMigrationInfos.get(0)).isEqualToIgnoringGivenFields(migrationInfo,
            "isProcessed", "processedTimestamp");
        Assertions.assertThat(updatedMigrationInfos.get(0).isProcessed()).isTrue();

        Assertions.assertThat(silverSskuRepository.findAll()).isEqualTo(sskuSilverParamValuesBefore);
    }

    @Test
    public void testMigrateServiceToNonEmptyBusinessWhileSourceBusinessDoesNotHaveOtherServices() {
        var migrationInfo = new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_ID_1)
            .setShopSku(SHOP_SKU)
            .setSrcBusinessId(BUSINESS_ID_1)
            .setDstBusinessId(BUSINESS_ID_2)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insert(migrationInfo);

        // create silver param values for service offer in source business
        CommonSsku origSsku = createTestDatacampSsku(BUSINESS_ID_1, List.of(SUPPLIER_ID_1));
        SilverCommonSsku orgSilverSsku = commonSskuToSilverCommonSsku(origSsku);
        silverSskuRepository.insertOrUpdateSsku(orgSilverSsku);
        List<SskuSilverParamValue> sskuSilverParamValuesBefore = silverSskuRepository.findAll();

        // create incoming offer for target business
        List<CommonSsku> sskusFromDatacamp = List.of(createTestDatacampSsku(BUSINESS_ID_2, List.of(SUPPLIER_ID_1)));

        serviceOfferMigrationService.processOffers(sskusFromDatacamp);

        List<ServiceOfferMigrationInfo> updatedMigrationInfos = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(updatedMigrationInfos).hasSize(1);
        Assertions.assertThat(updatedMigrationInfos.get(0)).isEqualToIgnoringGivenFields(migrationInfo,
            "isProcessed", "processedTimestamp");
        Assertions.assertThat(updatedMigrationInfos.get(0).isProcessed()).isTrue();

        Assertions.assertThat(silverSskuRepository.findAll())
            .containsExactlyInAnyOrderElementsOf(sskuSilverParamValuesBefore);
    }

    private static MdmSupplier createBusinessSupplier(int id) {
        return new MdmSupplier().setId(id).setType(MdmSupplierType.BUSINESS);
    }

    private static MdmSupplier createServiceSupplier(int id, int businessId) {
        return new MdmSupplier().setId(id).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(businessId).setBusinessEnabled(true);
    }

    private CommonSsku createTestDatacampSsku(int businessId, List<Integer> serviceIds) {
        var builder = new CommonSskuBuilder(paramCache, new ShopSkuKey(businessId, SHOP_SKU))
            .with(KnownMdmParams.MANUFACTURER_COUNTRY, "Кирибати");
        for (Integer serviceId : serviceIds) {
            builder.startServiceValues(serviceId)
                .with(KnownMdmParams.MIN_SHIPMENT, 11L)
                .with(KnownMdmParams.SERVICE_EXISTS, true)
                .endServiceValues();
        }
        return builder.build();
    }

    private SilverCommonSsku commonSskuToSilverCommonSsku(CommonSsku commonSsku) {
        MasterDataSource source = new MasterDataSource(
            MasterDataSourceType.SUPPLIER,
            String.valueOf(commonSsku.getKey().getSupplierId())
        );
        return SilverCommonSsku.fromCommonSsku(commonSsku, source);
    }
}
