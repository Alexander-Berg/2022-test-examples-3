package ru.yandex.market.mdm.integration.test.logbroker;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerEvent;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestWithLogbrokerClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@DirtiesContext
public class DataCampToMdmMappingsLogbrokerIntegrationTest extends MdmBaseIntegrationTestWithLogbrokerClass {
    private static final int LB_TIMEOUT_SEC = 10;
    // Checking updates only for biz_ids
    // All business logic with services has been already checked in DataCampToMdmLogbrokerDataProcessorImplTest
    private static final int BUSINESS = 1;
    private static final long MSKU = 123456L;
    private static final int CATEGORY = 777;
    private static final long VERSION = 3453534L;
    private static final int UPDATED_CATEGORY = 888;
    private static final String BUSINES_SHOP_SKU = "xxx";

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    @Qualifier("testingDataCampToMdmMappingsProducer")
    private MdmLogbrokerService datacampToMdmLogbrokerService;
    @Autowired
    private SilverSskuRepository silverSskuRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        mappingsCacheRepository.deleteAll();
        mdmSupplierRepository.deleteAll();
        MdmSupplier existingBusiness = business(BUSINESS);
        mdmSupplierRepository.insertBatch(existingBusiness);

        // Set master data version lower than in the test, to get real update.
        Optional<SilverCommonSsku> commonSsku = silverSskuRepository.findSsku(
            new ShopSkuKey(BUSINESS, BUSINES_SHOP_SKU))
            .stream()
            .findFirst();

        if (commonSsku.isPresent()) {
            SilverCommonSsku commonSskuForUpdate = commonSsku.get();
            SilverServiceSsku baseSsku = commonSskuForUpdate.getBaseSsku();
            MdmParamValue baseVersion = baseSsku.getParamValue(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                .orElseThrow()
                .setNumeric(BigDecimal.valueOf(VERSION - 1));
            baseSsku.addParamValue(baseVersion);

            Map<Integer, SilverServiceSsku> serviceSskus = commonSskuForUpdate.getServiceSskus();
            serviceSskus.forEach((supplierId, ssku) -> {
                MdmParamValue serviceVersion = baseSsku.getParamValue(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                    .orElseThrow()
                    .setNumeric(BigDecimal.valueOf(VERSION - 1));
                ssku.addParamValue(serviceVersion);
            });

            commonSskuForUpdate.setBaseSsku(baseSsku);
            commonSskuForUpdate.putServiceSskus(serviceSskus.values());
            silverSskuRepository.insertOrUpdateSsku(commonSskuForUpdate);
        }

        var gg = silverSskuRepository.findSsku(new ShopSkuKey(BUSINESS, BUSINES_SHOP_SKU));
        commit();
    }

    @Test
    public void testNewMappingsReceived() {
        commit();
        datacampToMdmLogbrokerService.publishEvent(message(unitedOffer(CATEGORY)));
        MappingCacheDao expectedBiz = mappingCacheDao(BUSINESS, BUSINES_SHOP_SKU, CATEGORY, MSKU);
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mappingsCacheRepository.findAll())
                .usingElementComparatorIgnoringFields("updateStamp", "modifiedTimestamp", "versionTimestamp",
                    "mappingSource", "mbocTimestamp", "eoxTimestamp")
                .containsExactlyInAnyOrder(
                    expectedBiz
                );
        });
    }

    @Test
    public void testOldMappingsUpdated() {
        commit();
        MappingCacheDao insertedBiz = mappingCacheDao(BUSINESS, BUSINES_SHOP_SKU, CATEGORY, MSKU);
        mappingsCacheRepository.insertBatch(insertedBiz);
        commit();
        datacampToMdmLogbrokerService.publishEvent(message(unitedOffer(UPDATED_CATEGORY)));
        MappingCacheDao expectedBiz = mappingCacheDao(BUSINESS, BUSINES_SHOP_SKU, UPDATED_CATEGORY, MSKU);
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mappingsCacheRepository.findAll())
                .usingElementComparatorIgnoringFields("updateStamp", "modifiedTimestamp", "versionTimestamp",
                    "mappingSource", "mbocTimestamp", "eoxTimestamp")
                .containsExactlyInAnyOrder(
                    expectedBiz
                );
        });
    }

    @After
    public void cleanup() {
        mappingsCacheRepository.deleteAll();
        mdmSupplierRepository.deleteAll();
        commit();
    }

    private MdmSupplier business(int id) {
        MdmSupplier s = new MdmSupplier();
        s.setId(id);
        s.setType(MdmSupplierType.BUSINESS);
        s.setBusinessEnabled(true);
        s.setDeleted(false);
        return s;
    }

    private MdmLogbrokerEvent<DatacampMessageOuterClass.DatacampMessage> message(
        DataCampUnitedOffer.UnitedOffer... messages) {
        return new MdmLogbrokerEvent<>(DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .addAllUnitedOffers(
                List.of(DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                    .addAllOffer(List.of(messages))
                    .build()))
            .build());
    }

    private DataCampUnitedOffer.UnitedOffer unitedOffer(int mappingCategoryId) {
        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basic(mappingCategoryId))
            .build();
        return unitedOffer;
    }

    private DataCampOffer.Offer basic(int mappingCategoryId) {
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS)
                .setOfferId(BUSINES_SHOP_SKU)
                .build())
            .setContent(offerContent(mappingCategoryId))
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setMasterDataVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(VERSION)
                        .build()).build()).build())
            .build();
    }

    private DataCampOfferContent.OfferContent offerContent(int mappingCategoryId) {
        return DataCampOfferContent.OfferContent.newBuilder()
            .setBinding(mapping(mappingCategoryId))
            .setStatus(status())
            .build();
    }

    private DataCampOfferMapping.ContentBinding mapping(int mappingCategoryId) {
        return DataCampOfferMapping.ContentBinding.newBuilder()
            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                .setMarketSkuId(MSKU)
                .setMarketCategoryId(mappingCategoryId)
                .setMeta(meta()))
            .build();
    }

    private DataCampContentStatus.ContentStatus status() {
        return DataCampContentStatus.ContentStatus.newBuilder()
            .setContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder()
                .setMeta(meta())
                .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT))
            .build();
    }

    private DataCampOfferMeta.UpdateMeta meta() {
        return DataCampOfferMeta.UpdateMeta.newBuilder()
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(100)
                .setNanos(90))
            .build();
    }

    private void commit() {
        jdbcTemplate.getJdbcTemplate().execute("commit");
    }

    private MappingCacheDao mappingCacheDao(int supplierId, String shopSku, int categoryId, long mskuId) {
        return new MappingCacheDao()
            .setCategoryId(categoryId)
            .setMskuId(mskuId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setMskuId(MSKU);
    }

}
