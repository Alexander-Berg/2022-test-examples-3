package ru.yandex.market.mboc.common.offers.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class AntiMappingRepositoryImplAuditTest extends BaseDbTestClass {

    private static final long OFFER_ID = 12345L;

    @Autowired
    private AntiMappingRepositoryImpl antiMappingRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MboAuditServiceMock mboAuditService;

    @Before
    public void setUp() {
        mboAuditService.clearActions();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        Offer offer = OfferTestUtils.nextOffer().setId(OFFER_ID);
        offerRepository.insertOffer(offer);
    }

    @Test
    public void insertNew() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping());

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), "offer_id"),
                action(inserted.getId(), "not_model_id"),
                action(inserted.getId(), "not_sku_id"),
                action(inserted.getId(), "updated_user"),
                action(inserted.getId(), "upload_request_ts"),
                action(inserted.getId(), "source_type")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.CREATE,
                    "not_model_id:not_sku_id",
                    null,
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID)
            );

        List<MboAudit.MboAction> skuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getNotSkuId(), MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(skuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getNotSkuId(), MboAudit.ActionType.CREATE,
                    "offer_id", null, String.valueOf(OFFER_ID))
            );
    }

    @Test
    public void markAsDeleted() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping());
        mboAuditService.clearActions();

        inserted
            .setDeletedTs(Instant.now())
            .setDeletedUser("deleted user");

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.CREATE, "deleted_ts"),
                action(inserted.getId(), MboAudit.ActionType.CREATE, "deleted_user")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    StringUtils.EMPTY)
            );

        List<MboAudit.MboAction> skuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getNotSkuId(), MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(skuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getNotSkuId(), MboAudit.ActionType.DELETE,
                    "offer_id",
                    String.valueOf(inserted.getOfferId()),
                    StringUtils.EMPTY)
            );
    }

    @Test
    public void restoreDeleted() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping(true));
        mboAuditService.clearActions();

        inserted
            .setDeletedTs(null)
            .setDeletedUser(null);

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.DELETE, "deleted_ts"),
                action(inserted.getId(), MboAudit.ActionType.DELETE, "deleted_user")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.CREATE,
                    "not_model_id:not_sku_id",
                    StringUtils.EMPTY,
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID)
            );

        List<MboAudit.MboAction> skuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getNotSkuId(), MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(skuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getNotSkuId(), MboAudit.ActionType.CREATE,
                    "offer_id", StringUtils.EMPTY, String.valueOf(inserted.getOfferId()))
            );
    }

    @Test
    public void updateExisting() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping());
        mboAuditService.clearActions();

        inserted.setNotSkuId(OfferTestUtils.TEST_SKU_ID + 1);

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.UPDATE, "not_sku_id")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    StringUtils.EMPTY),
                action(inserted.getOfferId(), MboAudit.ActionType.CREATE,
                    "not_model_id:not_sku_id",
                    StringUtils.EMPTY,
                    OfferTestUtils.TEST_MODEL_ID + ":" + (OfferTestUtils.TEST_SKU_ID + 1))
            );

        List<MboAudit.MboAction> oldSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(oldSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE, "offer_id")
            );

        List<MboAudit.MboAction> newSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID + 1, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(newSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID + 1, MboAudit.ActionType.CREATE, "offer_id")
            );
    }

    @Test
    public void updateAndRestoreMarkedAsDeleted() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping(true));
        mboAuditService.clearActions();

        inserted.setNotSkuId(OfferTestUtils.TEST_SKU_ID + 1)
            .setDeletedTs(null)
            .setDeletedUser(null);

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.DELETE, "deleted_ts"),
                action(inserted.getId(), MboAudit.ActionType.DELETE, "deleted_user"),
                action(inserted.getId(), MboAudit.ActionType.UPDATE, "not_sku_id")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    null),
                action(inserted.getOfferId(), MboAudit.ActionType.CREATE,
                    "not_model_id:not_sku_id",
                    null,
                    OfferTestUtils.TEST_MODEL_ID + ":" + (OfferTestUtils.TEST_SKU_ID + 1))
            );

        List<MboAudit.MboAction> oldSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(oldSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE, "offer_id")
            );

        List<MboAudit.MboAction> newSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID + 1, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(newSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID + 1, MboAudit.ActionType.CREATE, "offer_id")
            );
    }

    @Test
    public void updateToNullSkuAndRestoreMarkedAsDeleted() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping(true));
        mboAuditService.clearActions();

        inserted.setNotSkuId(null)
            .setDeletedTs(null)
            .setDeletedUser(null);

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.DELETE, "deleted_ts"),
                action(inserted.getId(), MboAudit.ActionType.DELETE, "deleted_user"),
                action(inserted.getId(), MboAudit.ActionType.DELETE, "not_sku_id")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    StringUtils.EMPTY),
                action(inserted.getOfferId(), MboAudit.ActionType.CREATE,
                    "not_model_id:not_sku_id",
                    StringUtils.EMPTY,
                    OfferTestUtils.TEST_MODEL_ID + ":" + null)
            );

        List<MboAudit.MboAction> oldSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(oldSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE, "offer_id")
            );

        List<MboAudit.MboAction> newSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(newSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE,
                    "offer_id", String.valueOf(inserted.getOfferId()), StringUtils.EMPTY)
            );
    }

    @Test
    public void updateToNullSku() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping());
        mboAuditService.clearActions();

        inserted.setNotSkuId(null);

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.DELETE, "not_sku_id")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    StringUtils.EMPTY),
                action(inserted.getOfferId(), MboAudit.ActionType.CREATE,
                    "not_model_id:not_sku_id",
                    StringUtils.EMPTY,
                    OfferTestUtils.TEST_MODEL_ID + ":" + null)
            );

        List<MboAudit.MboAction> oldSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(oldSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE, "offer_id")
            );

        List<MboAudit.MboAction> newSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(newSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE,
                    "offer_id", String.valueOf(inserted.getOfferId()), StringUtils.EMPTY)
            );
    }

    @Test
    public void updateExistingAndMarkDeleted() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping());
        mboAuditService.clearActions();

        inserted.setNotSkuId(OfferTestUtils.TEST_SKU_ID + 1)
            .setDeletedTs(Instant.now())
            .setDeletedUser("deleted user");

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.CREATE, "deleted_ts"),
                action(inserted.getId(), MboAudit.ActionType.CREATE, "deleted_user"),
                action(inserted.getId(), MboAudit.ActionType.UPDATE, "not_sku_id")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    StringUtils.EMPTY),
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + (OfferTestUtils.TEST_SKU_ID + 1),
                    StringUtils.EMPTY)
            );

        List<MboAudit.MboAction> oldSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(oldSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE, "offer_id")
            );

        List<MboAudit.MboAction> newSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID + 1, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(newSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID + 1, MboAudit.ActionType.DELETE, "offer_id")
            );
    }

    @Test
    public void updateToNullSkuAndMarkDelete() {
        AntiMapping inserted = antiMappingRepository.insert(antiMapping());
        mboAuditService.clearActions();

        inserted.setNotSkuId(null)
            .setDeletedTs(Instant.now())
            .setDeletedUser("deleted user");

        antiMappingRepository.update(inserted);

        List<MboAudit.MboAction> antiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getId(), MboAudit.EntityType.ANTI_MAPPING))
            .getActionsList();
        assertThat(antiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(inserted.getId(), MboAudit.ActionType.CREATE, "deleted_ts"),
                action(inserted.getId(), MboAudit.ActionType.CREATE, "deleted_user"),
                action(inserted.getId(), MboAudit.ActionType.DELETE, "not_sku_id")
            );

        List<MboAudit.MboAction> offerAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(inserted.getOfferId(), MboAudit.EntityType.OFFER_ANTI_MAPPING))
            .getActionsList();
        assertThat(offerAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + OfferTestUtils.TEST_SKU_ID,
                    StringUtils.EMPTY),
                action(inserted.getOfferId(), MboAudit.ActionType.DELETE,
                    "not_model_id:not_sku_id",
                    OfferTestUtils.TEST_MODEL_ID + ":" + null,
                    StringUtils.EMPTY)
            );

        List<MboAudit.MboAction> oldSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(oldSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE, "offer_id")
            );

        List<MboAudit.MboAction> newSkuAntiMappingAudit = mboAuditService
            .findActions(createFindAuditRequest(OfferTestUtils.TEST_SKU_ID, MboAudit.EntityType.SKU_ANTI_MAPPING))
            .getActionsList();
        assertThat(newSkuAntiMappingAudit)
            .usingElementComparatorOnFields("entityId", "propertyName", "actionType", "oldValue", "newValue")
            .containsExactlyInAnyOrder(
                action(OfferTestUtils.TEST_SKU_ID, MboAudit.ActionType.DELETE,
                    "offer_id", String.valueOf(inserted.getOfferId()), StringUtils.EMPTY)
            );
    }

    private AntiMapping antiMapping() {
        return antiMapping(false);
    }

    private AntiMapping antiMapping(boolean deleted) {
        AntiMapping antiMapping = new AntiMapping()
            .setOfferId(OFFER_ID)
            .setNotModelId(OfferTestUtils.TEST_MODEL_ID)
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID)
            .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
            .setCreatedTs(Instant.now().minus(3, ChronoUnit.DAYS))
            .setUpdatedTs(Instant.now().minus(2, ChronoUnit.DAYS))
            .setUpdatedUser("test user updated")
            .markNeedsUpload();
        if (deleted) {
            antiMapping
                .setDeletedUser("test user deleted")
                .setDeletedTs(Instant.now().minus(1, ChronoUnit.DAYS));
        }
        return antiMapping;
    }

    private MboAudit.FindActionsRequest createFindAuditRequest(long entityId,
                                                               MboAudit.EntityType entityType) {
        return MboAudit.FindActionsRequest.newBuilder()
            .setEntityId(entityId)
            .addEntityType(entityType)
            .setLength(100)
            .build();
    }

    private MboAudit.MboAction action(long entityId,
                                      String propertyName) {
        return action(entityId, MboAudit.ActionType.CREATE, propertyName, null, null);
    }

    private MboAudit.MboAction action(long entityId,
                                      MboAudit.ActionType actionType,
                                      String propertyName) {
        return action(entityId, actionType, propertyName, null, null);
    }

    private MboAudit.MboAction action(long entityId,
                                      MboAudit.ActionType actionType,
                                      String propertyName,
                                      String oldValue,
                                      String newValue) {
        MboAudit.MboAction.Builder builder = MboAudit.MboAction.newBuilder()
            .setPropertyName(propertyName)
            .setEntityId(entityId)
            .setActionType(actionType);
        if (oldValue != null) {
            builder.setOldValue(oldValue);
        }
        if (newValue != null) {
            builder.setNewValue(newValue);
        }
        return builder.build();
    }
}
