package ru.yandex.market.deepmind.common.datacamp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindOfferRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;

import static ru.yandex.market.deepmind.common.datacamp.DeepmindDatacampOfferHandler.DEEPMIND_DATACAMP_SAVE_ENABLED;

public class DeepmindDatacampOfferHandlerTest extends DeepmindBaseDbTestClass {

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private DeepmindOfferRepository deepmindOfferRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;

    private DeepmindDatacampOfferHandler handler;

    @Before
    public void setUp() throws Exception {
        var offersConverter = new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository);
        var storageKeyValueService = new StorageKeyValueServiceMock();

        handler = new DeepmindDatacampOfferHandler(
            BERU_ID,
            BERU_BUSINESS_ID,
            offersConverter,
            deepmindOfferRepository,
            storageKeyValueService
        );
        deepmindSupplierRepository.save(
            supplier(1, 11, SupplierType.THIRD_PARTY),
            supplier(2, BERU_BUSINESS_ID, SupplierType.REAL_SUPPLIER).setRealSupplierId("000222")
        );

        storageKeyValueService.putValue(DEEPMIND_DATACAMP_SAVE_ENABLED, true);
    }

    @Test
    public void process() {
        handler.process(List.of(
            message(11, 1, "sku-1", 1111, 123, "title-1", 123L, OfferAcceptanceStatus.NEW),
            message(BERU_BUSINESS_ID, 2, "000222.sku-2", 2222, 456, "title-2", 124L, OfferAcceptanceStatus.OK)
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK)
            );
    }

    @Test
    public void notSaveOldVersionedOffer() {
        deepmindOfferRepository.save(
            offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK)
        );

        handler.process(List.of(
            message(11, 1, "sku-1", 3333, 123, "title-1", 120L, OfferAcceptanceStatus.NEW),
            message(BERU_BUSINESS_ID, 2, "000222.sku-2", 4444, 456, "title-2", 121L, OfferAcceptanceStatus.OK)
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK)
            );
    }

    @Test
    public void deleteBaseOffersTest() {
        deepmindSupplierRepository.save(
            supplier(3, 33, SupplierType.THIRD_PARTY)
        );
        deepmindOfferRepository.save(
            offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
            offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
        );

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );

        handler.process(List.of(
            message(11, Map.of(1, false, 3, false), "sku-1", 1111, 123, "title-1", 126L,
                OfferAcceptanceStatus.NEW, true)
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK)
            );
    }

    @Test
    public void deleteServiceOffersTest() {
        deepmindSupplierRepository.save(
            supplier(3, 33, SupplierType.THIRD_PARTY)
        );
        deepmindOfferRepository.save(
            offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
            offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
        );

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );

        handler.process(List.of(
            message(11, Map.of(1, false, 3, true), "sku-1", 1111, 123,
                "title-1", 126L, OfferAcceptanceStatus.NEW)
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 126L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK)
            );
    }

    @Test
    public void offerChangedTwiceInBatchTest() {
        deepmindSupplierRepository.save(
            supplier(3, 33, SupplierType.THIRD_PARTY)
        );
        deepmindOfferRepository.save(
            offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
            offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
        );

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );

        handler.process(List.of(
            message(11, Map.of(1, false, 3, false), "sku-1", 1111, 123, "title-1", 125L,
                OfferAcceptanceStatus.NEW, true), // delete the offer
            message(11, 1, "sku-1", 1111, 123, "title-1", 126L, OfferAcceptanceStatus.NEW) // add the deleted offer
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 126L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );
    }

    @Test
    public void dontDeleteIfOldVersionTest() {
        deepmindSupplierRepository.save(
            supplier(3, 33, SupplierType.THIRD_PARTY)
        );
        deepmindOfferRepository.save(
            offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
            offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
        );

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );

        handler.process(List.of(
            message(11, Map.of(1, false, 3, false), "sku-1", 1111, 123, "title-1", 120L,
                OfferAcceptanceStatus.NEW, true) // delete the offer but version is old
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );
    }

    @Test
    public void dontDeleteIfOldVersionServiceOfferTest() {
        deepmindSupplierRepository.save(
            supplier(3, 33, SupplierType.THIRD_PARTY)
        );
        deepmindOfferRepository.save(
            offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
            offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
        );

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );

        handler.process(List.of(
            message(11, Map.of(1, true, 3, false), "sku-1", 1111, 123, "title-1", 120L,
                OfferAcceptanceStatus.NEW, false) // delete the offer but version is old
        ));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                offer(1, "sku-1", 11, "title-1", 123L, 1111L, 123L,
                    SupplierType.THIRD_PARTY, OfferAcceptanceStatus.NEW),
                offer(2, "sku-2", BERU_BUSINESS_ID, "title-2", 124L, 2222L, 456L,
                    SupplierType.REAL_SUPPLIER, OfferAcceptanceStatus.OK),
                offer(3, "sku-1", 33, "title-1", 125L, 3333, 456L, SupplierType.THIRD_PARTY, OfferAcceptanceStatus.OK)
            );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private DatacampMessageOuterClass.DatacampMessage message(int businessId,
                                                              int supplierId,
                                                              String shopSku,
                                                              long mskuId,
                                                              int categoryId,
                                                              String title,
                                                              long version,
                                                              OfferAcceptanceStatus acceptanceStatus) {
        return message(businessId, Map.of(supplierId, false), shopSku, mskuId, categoryId, title,
            version, acceptanceStatus);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private DatacampMessageOuterClass.DatacampMessage message(int businessId,
                                                              Map<Integer, Boolean> supplierIds,
                                                              String shopSku,
                                                              long mskuId,
                                                              int categoryId,
                                                              String title,
                                                              long version,
                                                              OfferAcceptanceStatus acceptanceStatus) {
        return message(businessId, supplierIds, shopSku, mskuId, categoryId, title, version, acceptanceStatus, false);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private DatacampMessageOuterClass.DatacampMessage message(int businessId,
                                                              Map<Integer, Boolean> supplierIdToDeleted,
                                                              String shopSku,
                                                              long mskuId,
                                                              int categoryId,
                                                              String title,
                                                              long version,
                                                              OfferAcceptanceStatus acceptanceStatus,
                                                              boolean isBaseOfferDeleted) {
        var offerBuilder = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(businessId)
                    .setOfferId(shopSku)
                    .setShopId(businessId)
                    .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketSkuId(mskuId)
                            .setMarketCategoryId(categoryId)
                            .build())
                        .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketCategoryId(categoryId)
                            .build())
                        .build())
                    .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                            .setTitle(DataCampOfferMeta.StringValue.newBuilder().setValue(title).build())
                            .build())
                        .build())
                    .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                        .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                            .getDefaultInstanceForType())
                        .build())
                    .setStatus(DataCampContentStatus.ContentStatus.newBuilder()
                        .setContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setOfferAcceptanceStatus(
                                DataCampContentStatus.OfferAcceptanceStatus.forNumber(
                                    acceptanceStatusOrdinal(acceptanceStatus)))
                            .build())
                        .build())
                    .build())
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                    .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                        .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                            .setCounter(version)
                            .build())
                        .build())
                    .setRemoved(DataCampOfferMeta.Flag.newBuilder()
                        .setFlag(isBaseOfferDeleted)
                        .build())
                    .build())
                .build());
        supplierIdToDeleted.forEach((key, value) -> offerBuilder.putService(key, DataCampOffer.Offer.newBuilder()
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                    .setRemoved(DataCampOfferMeta.Flag.newBuilder()
                        .setFlag(value)
                        .build())
                    .build())
                .build())
            .build());

        return DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .addUnitedOffers(DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                .addOffer(offerBuilder))
            .build();
    }

    private Integer acceptanceStatusOrdinal(OfferAcceptanceStatus acceptanceStatus) {
        switch (acceptanceStatus) {
            case NEW:
                return 0;
            case OK:
                return 1;
            case TRASH:
                return 2;
            default:
                return null;
        }
    }

    private Supplier supplier(int id, Integer businessId, SupplierType supplierType) {
        return new Supplier()
            .setId(id)
            .setBusinessId(businessId)
            .setName("id_" + id)
            .setSupplierType(supplierType);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private ServiceOfferReplica offer(int supplierId, String shopSku, int businessId, String title, long version,
                                      long mskuId, long categoryId, SupplierType supplierType,
                                      OfferAcceptanceStatus acceptanceStatus) {
        return new ServiceOfferReplica()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setBusinessId(businessId)
            .setSeqId(version)
            .setTitle(title)
            .setMskuId(mskuId)
            .setCategoryId(categoryId)
            .setSupplierType(supplierType)
            .setAcceptanceStatus(acceptanceStatus)
            .setModifiedTs(Instant.now());
    }

}
