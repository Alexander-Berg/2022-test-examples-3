package ru.yandex.market.psku.postprocessor.service.migration;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampContentStatus.MappingConfidence;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampUnitedOffer;
import Market.UltraControllerServiceData.UltraController;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.MigratedPskusChangeOwnershipInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.MigratedPskusDeleteInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuChangeOwnershipProcessStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuDeleteProcessStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MigratedPskusChangeOwnershipInfo;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.MigratedPskusDeleteInfo;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SaveAsyncMigrationServiceTest extends BaseDBTest {
    private static final int BUSINESS_ID_1 = 1;
    private static final int BUSINESS_ID_2 = 2;
    private static final int BUSINESS_ID_3 = 3;
    private static final int BUSINESS_ID_4 = 4;
    private static final String OFFER_ID_1 = "offer1";
    private static final String OFFER_ID_2 = "offer2";
    private static final String OFFER_ID_3 = "offer3";
    private static final String OFFER_ID_4 = "offer4";
    private static final int SKU_ID_1 = 11;
    private static final int SKU_ID_2 = 22;
    private static final int SKU_ID_3 = 33;
    private static final int SKU_ID_4 = 44;
    private static final int OTHER_SKU_ID = 99;

    @Autowired
    MigratedPskusChangeOwnershipInfoDao migratedPskusChangeOwnershipInfoDao;
    @Autowired
    MigratedPskusDeleteInfoDao migratedPskusDeleteInfoDao;
    SaveAsyncMigrationService saveAsyncMigrationService;


    @Before
    public void setup() {
        saveAsyncMigrationService = new SaveAsyncMigrationService(
            Mockito.spy(migratedPskusDeleteInfoDao),
            Mockito.spy(migratedPskusChangeOwnershipInfoDao),
            true,
            true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorIfConflictResolutionIsEmpty() {
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            null
        );
    }

    @Test
    public void testTheSameMappingDoNothing() {
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET
        );

        assertTrue(migratedPskusChangeOwnershipInfoDao.findAll().isEmpty());
        assertTrue(migratedPskusDeleteInfoDao.findAll().isEmpty());
    }

    @Test
    public void testSaveTransitionsIfAcceptTarget() {
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET
        );

        assertTrue(migratedPskusChangeOwnershipInfoDao.findAll().isEmpty());

        List<MigratedPskusDeleteInfo> deleteInfos = migratedPskusDeleteInfoDao.findAll();
        assertEquals(1, deleteInfos.size());

        MigratedPskusDeleteInfo deleteInfo = deleteInfos.get(0);
        assertEquals(BUSINESS_ID_1, deleteInfo.getDeletingBusinessId().longValue());
        assertEquals(BUSINESS_ID_2, deleteInfo.getAcceptingBusinessId().longValue());
        assertEquals(OFFER_ID_1, deleteInfo.getDeletingOfferId());
        assertEquals(OFFER_ID_2, deleteInfo.getAcceptingOfferId());
        assertEquals(SKU_ID_1, deleteInfo.getDeletingPskuId().intValue());
        assertEquals(SKU_ID_2, deleteInfo.getAcceptingPskuId().intValue());
        assertEquals(PskuDeleteProcessStatus.NEW, deleteInfo.getProcessingStatus());
    }

    @Test
    public void testSaveTransitionsIfAcceptSource() {
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE
        );

        assertEquals(1, migratedPskusChangeOwnershipInfoDao.findAll().size());

        List<MigratedPskusDeleteInfo> deleteInfos = migratedPskusDeleteInfoDao.findAll();
        assertEquals(1, deleteInfos.size());

        MigratedPskusDeleteInfo deleteInfo = deleteInfos.get(0);
        assertEquals(BUSINESS_ID_2, deleteInfo.getDeletingBusinessId().longValue());
        assertEquals(BUSINESS_ID_1, deleteInfo.getAcceptingBusinessId().longValue());
        assertEquals(OFFER_ID_1, deleteInfo.getAcceptingOfferId());
        assertEquals(OFFER_ID_2, deleteInfo.getDeletingOfferId());
        assertEquals(SKU_ID_2, deleteInfo.getDeletingPskuId().intValue());
        assertEquals(SKU_ID_1, deleteInfo.getAcceptingPskuId().intValue());
        assertEquals(PskuDeleteProcessStatus.NEW, deleteInfo.getProcessingStatus());
    }

    @Test
    public void testOnlySourceOfferExists() {
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            createEmptyUnitedOffer(),
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE
        );

        assertTrue(migratedPskusDeleteInfoDao.findAll().isEmpty());

        List<MigratedPskusChangeOwnershipInfo> changeOwnershipInfos = migratedPskusChangeOwnershipInfoDao.findAll();
        assertEquals(1, changeOwnershipInfos.size());

        MigratedPskusChangeOwnershipInfo changeOwnershipInfo = changeOwnershipInfos.get(0);
        assertEquals(PskuChangeOwnershipProcessStatus.NEW, changeOwnershipInfo.getProcessingStatus());
        assertEquals(SKU_ID_1, changeOwnershipInfo.getPskuId().intValue());
        assertEquals(OFFER_ID_1, changeOwnershipInfo.getDeletingOfferId());
        assertEquals(BUSINESS_ID_1, changeOwnershipInfo.getDeletingBusinessId().intValue());
    }

    @Test
    public void testSaveTransitionsOnlyWhenOfferHasPartnerSelfConfidence() {
        // both source and result offers have MAPPING_CONFIDENCE_AUTO as confidence, so nothing should be stored
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_AUTO),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, MappingConfidence.MAPPING_CONFIDENCE_AUTO),
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE
        );

        assertEquals(0, migratedPskusChangeOwnershipInfoDao.findAll().size());

        List<MigratedPskusDeleteInfo> deleteInfos = migratedPskusDeleteInfoDao.findAll();
        assertEquals(0, deleteInfos.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testSaveTransitionsWhenNoConfidenceButHasMapping() {
        saveAsyncMigrationService.savePskuMigrationInfo(
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, null),
            createUnitedOffer(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, null),
            createUnitedOffer(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF),
            BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE
        );
    }

    @Test
    public void testAllowAsyncMigration() {
        addDeleteInfo(BUSINESS_ID_1, OFFER_ID_1, OTHER_SKU_ID, SKU_ID_1, PskuDeleteProcessStatus.NEW);
        addDeleteInfo(BUSINESS_ID_2, OFFER_ID_2, OTHER_SKU_ID, SKU_ID_2, PskuDeleteProcessStatus.PSKU_DELETED);
        addDeleteInfo(BUSINESS_ID_3, OFFER_ID_3, OTHER_SKU_ID, SKU_ID_3, PskuDeleteProcessStatus.PMODEL_DELETED);
        addDeleteInfo(BUSINESS_ID_4, OFFER_ID_4, OTHER_SKU_ID, SKU_ID_4, PskuDeleteProcessStatus.READY_FOR_PROCESS);

        addChangeOwnerInfo(BUSINESS_ID_1, OFFER_ID_1, SKU_ID_1, PskuChangeOwnershipProcessStatus.NEW);
        addChangeOwnerInfo(BUSINESS_ID_2, OFFER_ID_2, SKU_ID_2, PskuChangeOwnershipProcessStatus.NOT_NEED_TO_CHANGE);
        addChangeOwnerInfo(BUSINESS_ID_3, OFFER_ID_3, SKU_ID_3, PskuChangeOwnershipProcessStatus.CHANGED);
        addChangeOwnerInfo(BUSINESS_ID_4, OFFER_ID_4, SKU_ID_4, PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS);

        List<DataCampOfferIdentifiers.OfferIdentifiers> ids = new ArrayList<>();
        ids.add(createIdentifiers(BUSINESS_ID_1, OFFER_ID_1));

        saveAsyncMigrationService.allowPskuMigrationProcess(ids);

        List<MigratedPskusDeleteInfo> deleteInfos =
            migratedPskusDeleteInfoDao.fetchByProcessingStatus(PskuDeleteProcessStatus.READY_FOR_PROCESS);
        List<MigratedPskusChangeOwnershipInfo> changeOwnershipInfos =
            migratedPskusChangeOwnershipInfoDao.fetchByProcessingStatus(PskuChangeOwnershipProcessStatus.READY_FOR_PROCESS);

        assertEquals(2, deleteInfos.size());
        assertEquals(2, changeOwnershipInfos.size());

        Optional<MigratedPskusDeleteInfo> deleteInfo = deleteInfos.stream()
            .filter(info -> info.getDeletingBusinessId().equals((long) BUSINESS_ID_1) && info.getDeletingOfferId().equals(OFFER_ID_1))
            .findFirst();
        Optional<MigratedPskusChangeOwnershipInfo> changeOwnershipInfo = changeOwnershipInfos.stream()
            .filter(info -> info.getDeletingBusinessId().equals((long) BUSINESS_ID_1) && info.getDeletingOfferId().equals(OFFER_ID_1))
            .findFirst();

        assertTrue(changeOwnershipInfo.isPresent());
        assertTrue(deleteInfo.isPresent());

    }

    @NotNull
    private DataCampOfferIdentifiers.OfferIdentifiers createIdentifiers(int businessId, String offerId) {
        return DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setOfferId(offerId)
            .setBusinessId(businessId)
            .build();
    }

    private MigratedPskusChangeOwnershipInfo addChangeOwnerInfo(
        int businessId,
        String offerId,
        int pskuId,
        PskuChangeOwnershipProcessStatus status
    ) {
        MigratedPskusChangeOwnershipInfo changeOwnershipInfo = new MigratedPskusChangeOwnershipInfo();
        changeOwnershipInfo.setDeletingBusinessId((long) businessId);
        changeOwnershipInfo.setDeletingOfferId(offerId);
        changeOwnershipInfo.setPskuId((long) pskuId);
        changeOwnershipInfo.setProcessingStatus(status);
        changeOwnershipInfo.setProcessingStatusUpdateTs(Timestamp.from(Instant.now()));
        migratedPskusChangeOwnershipInfoDao.insert(changeOwnershipInfo);
        return changeOwnershipInfo;
    }

    private MigratedPskusDeleteInfo addDeleteInfo(
        int sourceBusinessId,
        String sourceOfferId,
        int deletingPskuId,
        int acceptingPskuId,
        PskuDeleteProcessStatus status
    ) {
        MigratedPskusDeleteInfo deleteInfo = new MigratedPskusDeleteInfo();
        deleteInfo.setDeletingOfferId(sourceOfferId);
        deleteInfo.setDeletingBusinessId((long) sourceBusinessId);
        deleteInfo.setDeletingPskuId((long) deletingPskuId);
        deleteInfo.setAcceptingPskuId((long) acceptingPskuId);
        deleteInfo.setProcessingStatus(status);
        deleteInfo.setProcessingStatusUpdateTs(Timestamp.from(Instant.now()));
        migratedPskusDeleteInfoDao.insert(deleteInfo);
        return deleteInfo;
    }

    private DataCampUnitedOffer.UnitedOffer createEmptyUnitedOffer() {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
            .build();
    }

    private DataCampUnitedOffer.UnitedOffer createUnitedOffer(
        int businessId,
        String offerId,
        int skuId,
        MappingConfidence mappingConfidence
    ) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(getBasic(businessId, offerId, skuId, mappingConfidence))
            .build();
    }

    @NotNull
    private DataCampOffer.Offer getBasic(int businessId, String offerId, int skuId,
                                         MappingConfidence mappingConfidence) {
        return DataCampOffer.Offer.newBuilder().setIdentifiers(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(businessId)
                    .setOfferId(offerId)
                    .build()
            )
            .setContent(getContentWithSkuMapping(skuId, mappingConfidence))
            .build();
    }

    @NotNull
    private DataCampOfferContent.OfferContent getContentWithSkuMapping(
        int skuId,
        MappingConfidence mappingConfidence
    ) {
        DataCampOfferContent.OfferContent.Builder builder = DataCampOfferContent.OfferContent.newBuilder().setBinding(
            DataCampOfferMapping.ContentBinding.newBuilder()
                .setApproved(DataCampOfferMapping.Mapping.newBuilder().setMarketSkuId(skuId).build()).build()
        )
            .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                .setEnrichedOffer(UltraController.EnrichedOffer.newBuilder()
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_APPROVED)
                    .build())
                .build());
        if (mappingConfidence != null) {
            builder.setStatus(DataCampContentStatus.ContentStatus.newBuilder()
                .setContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setSkuMappingConfidence(mappingConfidence)
                    .build())
                .build());
        }
        return builder.build();
    }
}
