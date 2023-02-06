package ru.yandex.market.deepmind.common.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatusDeleted;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusDeletedRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges;
import ru.yandex.market.mboc.http.SupplierOffer;

public class ImportMskuOfferHelperServiceTest extends DeepmindBaseDbTestClass {
    private static final String[] FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS = new String[]{
        "modifiedAt",
        "modifiedLogin",
        "statusStartAt",
        "statusFinishAt",
        "plannedStartAt",
        "plannedFinishAt",
        "hasNoPurchasePrice",
        "hasNoValidContract"
    };

    private static final String[] FIELDS_TO_IGNORE_WHEN_COMPARING_DELETED_STATUS = new String[]{
        "deletedTs"
    };

    private static final String METRIC_SUFFIX = "";

    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Autowired
    private SskuStatusRepository sskuStatusRepository;
    @Autowired
    private SskuStatusDeletedRepository sskuStatusDeletedRepository;
    @Autowired
    private OffersConverter offersConverter;
    @Autowired
    private BeruId beruId;
    @Autowired
    private TransactionHelper transactionHelper;

    private ImportMskuOfferHelperService importMskuOfferHelperService;

    @Before
    public void setup() {
        var solomonPushService = Mockito.mock(DeepmindSolomonPushService.class);

        importMskuOfferHelperService = new ImportMskuOfferHelperService(
            serviceOfferReplicaRepository,
            sskuStatusRepository,
            offersConverter,
            solomonPushService,
            beruId,
            transactionHelper
        );
    }

    @Test
    public void creatingOfferLeadsToTheCreationOfStatus() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);

        //act
        importMskuOfferHelperService.processChangedOffers(List.of(offer), METRIC_SUFFIX, false);

        //assert
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));
    }

    @Test
    public void creatingOfferByBusinessIdLeadsToTheCreationOfStatus() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);

        //act
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //assert
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));
    }

    @Test
    public void removingOfferLeadsToTheDeletingOfStatus() {
        //arrange
        var offerToStay = protoOffer(1, 1, "sku1", 1L, 1L);
        var offerToDelete = protoOffer(2, 2, "sku2", 2L, 2L);
        importMskuOfferHelperService.processChangedOffers(List.of(offerToStay, offerToDelete), METRIC_SUFFIX, false);

        var deletedOffer = makeOfferDeleted(offerToDelete);

        //act
        importMskuOfferHelperService.processChangedOffers(List.of(deletedOffer), METRIC_SUFFIX, false);

        //assert that status has been stored as deleted
        Assertions.assertThat(sskuStatusDeletedRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_DELETED_STATUS)
            .containsExactly(deletedStatus(2, "sku2"));

        //assert that other status remained
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));
    }

    @Test
    public void removingOfferByBusinessIdLeadsToTheDeletingOfStatus() {
        //arrange
        var offerToStay = protoOffer(1, 1, "sku1", 1L, 1L);
        var offerToDelete = protoOffer(1, 2, "sku2", 2L, 2L);
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offerToStay, offerToDelete),
            1,
            "sku0",
            "sku2",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //act
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offerToStay),
            1,
            "sku0",
            "sku2",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //assert that status has been stored as deleted
        Assertions.assertThat(sskuStatusDeletedRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_DELETED_STATUS)
            .containsExactly(deletedStatus(2, "sku2"));

        //assert that other status remained
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));
    }

    @Test
    public void addingDeletedOfferLeadsToTheRestoringOfStatus() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);
        var deletedOffer = makeOfferDeleted(offer);
        importMskuOfferHelperService.processChangedOffers(List.of(deletedOffer), METRIC_SUFFIX, false);

        //act
        importMskuOfferHelperService.processChangedOffers(List.of(offer), METRIC_SUFFIX, false);

        //assert that status has been created
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));

        //assert that deleted status has been removed
        Assertions.assertThat(sskuStatusDeletedRepository.findAll())
            .isEmpty();
    }

    @Test
    public void addingDeletedOfferByBusinessIdLeadsToTheRestoringOfStatus() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);
        var deletedOffer = makeOfferDeleted(offer);
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(deletedOffer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //act
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //assert that active status has been created
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));

        //assert that deleted status has been removed
        Assertions.assertThat(sskuStatusDeletedRepository.findAll())
            .isEmpty();
    }

    @Test
    public void repeatedDeletingDoesNotCauseDuplicates() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);
        importMskuOfferHelperService.processChangedOffers(List.of(offer), METRIC_SUFFIX, false);

        var deletedOffer = makeOfferDeleted(offer);
        importMskuOfferHelperService.processChangedOffers(List.of(deletedOffer), METRIC_SUFFIX, false);

        //act
        importMskuOfferHelperService.processChangedOffers(List.of(deletedOffer), METRIC_SUFFIX, false);

        //assert that exactly one deleted status has been created
        Assertions.assertThat(sskuStatusDeletedRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_DELETED_STATUS)
            .containsExactly(deletedStatus(1, "sku1"));
    }

    @Test
    public void repeatedDeletingByBusinessIdDoesNotCauseDuplicates() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //act
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //assert that exactly one deleted status has been created
        Assertions.assertThat(sskuStatusDeletedRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_DELETED_STATUS)
            .containsExactly(deletedStatus(1, "sku1"));
    }

    @Test
    public void repeatedRestoringDoesNotCauseDuplicates() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //act
        importMskuOfferHelperService.processOffersByBusinessId(
            List.of(offer),
            1,
            "sku0",
            "sku1",
            SupplierType.THIRD_PARTY,
            METRIC_SUFFIX,
            false
        );

        //assert that exactly one status has been created
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));
    }

    @Test
    public void repeatedRestoringByBusinessIdDoesNotCauseDuplicates() {
        //arrange
        var offer = protoOffer(1, 1, "sku1", 1L, 1L);
        importMskuOfferHelperService.processChangedOffers(List.of(offer), METRIC_SUFFIX, false);

        var deletedOffer = makeOfferDeleted(offer);
        importMskuOfferHelperService.processChangedOffers(List.of(deletedOffer), METRIC_SUFFIX, false);

        importMskuOfferHelperService.processChangedOffers(List.of(offer), METRIC_SUFFIX, false);

        //act
        importMskuOfferHelperService.processChangedOffers(List.of(offer), METRIC_SUFFIX, false);

        //assert that exactly one status has been created
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(status(1, "sku1"));
    }

    private MboCategoryOfferChanges.SimpleBaseOffer makeOfferDeleted(
        MboCategoryOfferChanges.SimpleBaseOffer protoOffer) {
        var serviceOffer = protoOffer.getServiceOffersList().get(0);
        return protoOffer(
            protoOffer.getBusinessId(),
            serviceOffer.getSupplierId(),
            protoOffer.getShopSku(),
            protoOffer.getModifiedSeqId(),
            protoOffer.getApprovedMappingMskuId(),
            true
        );
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY, false).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, boolean isDeleted) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY, isDeleted).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId,  SupplierOffer.SupplierType type) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId, type, false).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer.Builder protoOfferBuilder(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, SupplierOffer.SupplierType type,
        boolean isDeleted) {
        return MboCategoryOfferChanges.SimpleBaseOffer.newBuilder()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setModifiedSeqId(seqId)
            .setApprovedMappingMskuId(mskuId)
            .setIsDeleted(isDeleted)
            .setModifiedTs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
            .addServiceOffers(
                MboCategoryOfferChanges.SimpleServiceOffer.newBuilder()
                    .setSupplierId(supplierId)
                    .setSupplierType(type)
                    .setAcceptanceStatus(MboCategoryOfferChanges.SimpleServiceOffer.AcceptanceStatus.OK)
                    .build());
    }

    private SskuStatus status(MboCategoryOfferChanges.SimpleBaseOffer protoOffer) {
        return status(
            protoOffer.getServiceOffersList().get(0).getSupplierId(),
            protoOffer.getShopSku()
        );
    }

    private SskuStatus status(int supplierId, String shopSku) {
        return status(supplierId, shopSku, OfferAvailability.ACTIVE, null);
    }

    private SskuStatus status(int supplierId, String shopSku, OfferAvailability availability, String comment) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(
                ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.valueOf(
                    availability.name())
            )
            .setComment(comment)
            .setModifiedByUser(false);
    }

    private SskuStatusDeleted deletedStatus(MboCategoryOfferChanges.SimpleBaseOffer protoOffer) {
        return new SskuStatusDeleted(
            protoOffer.getServiceOffersList().get(0).getSupplierId(),
            protoOffer.getShopSku(),
            null,
            null
        );
    }

    private SskuStatusDeleted deletedStatus(int supplierId, String shopSku) {
        return new SskuStatusDeleted(supplierId, shopSku, null, null);
    }
}
