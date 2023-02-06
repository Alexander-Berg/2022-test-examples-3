package ru.yandex.market.mboc.common.services.offers.processing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.RemovedOffer;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoveOfferServiceTest extends BaseDbTestClass {

    private AtomicLong offerIdsGenerator;

    private RemoveOfferService removeOfferService;

    @SpyBean
    private RemovedOfferRepository removedOfferRepository;
    @SpyBean
    private OfferRepository offerRepository;
    @MockBean
    private DatacampImportService datacampImportService;
    @MockBean
    private MigrationService migrationService;
    @Autowired
    private TransactionHelper transactionHelper;

    @Before
    public void setUp() throws Exception {
        removeOfferService = new RemoveOfferService(
            removedOfferRepository,
            offerRepository,
            datacampImportService,
            migrationService,
            transactionHelper,
            offerDestinationCalculator
        );
        offerIdsGenerator = new AtomicLong();
    }

    @Test
    public void removeOffersWillMarkOnDeleteBasicOfferSuccessfully() {
        var businessSkuKey1 = new BusinessSkuKey(1111, "testShopSku1");
        var businessSkuKey2 = new BusinessSkuKey(2222, "testShopSku2");
        var businessSkuKeys = new HashSet<>(List.of(businessSkuKey1, businessSkuKey2));

        var offer1 = createOffer(businessSkuKey1.getBusinessId(), businessSkuKey1.getShopSku());
        var offer2 = createOffer(businessSkuKey2.getBusinessId(), businessSkuKey2.getShopSku());

        var offerList = List.of(offer1, offer2);

        when(offerRepository.findOffersByBusinessSkuKeys(businessSkuKeys))
            .thenReturn(List.of(offer1, offer2));
        doNothing().when(datacampImportService)
            .markAsImported(businessSkuKeys);

        removeOfferService.removeOffers(businessSkuKeys);

        final Map<Long, RemovedOffer> removedOfferMap = removedOfferRepository.findAll().stream()
            .collect(Collectors.toMap(RemovedOffer::getId, Function.identity()));

        offerList.forEach(offer -> {
            var removedOffer = removedOfferMap.get(offer.getId());
            assertNotNull(removedOffer);
            assertFalse(removedOffer.getIsRemoved());
            assertNull(removedOffer.getRemovedOffer());
            assertNull(removedOffer.getRemovedOfferContent());
        });

        verify(datacampImportService, times(1)).markAsImported(anySet());
    }

    @Test
    public void removeOffersAndCompleteMigrationIfExistsWillCompleteMigration() {
        removeOffersWillMarkOnDeleteBasicOfferSuccessfully();

        var businessSkuKeyWithMigrationWithoutSource1 = new BusinessSkuKey(1111, "testShopSku1");
        var businessSkuKeyWithMigrationOk2 = new BusinessSkuKey(2222, "testShopSku1");
        var businessSkuKeyWithMigrationOk22 = new BusinessSkuKey(2222, "testShopSku22");
        var businessSkuKeyWithoutMigration = new BusinessSkuKey(3333, "testShopSku3");
        var businessSkuKeys = new HashSet<>(List.of(
            businessSkuKeyWithoutMigration,
            businessSkuKeyWithMigrationOk2,
            businessSkuKeyWithMigrationOk22,
            businessSkuKeyWithMigrationWithoutSource1
        ));

        var migratingOfferKeys = new HashSet<>(List.of(
            businessSkuKeyWithMigrationOk2,
            businessSkuKeyWithMigrationOk22,
            businessSkuKeyWithMigrationWithoutSource1
        ));

        var sourceBusinessSkuKey1 = new BusinessSkuKey(4444, businessSkuKeyWithMigrationOk2.getShopSku());
        var sourceBusinessSkuKey2 = new BusinessSkuKey(4444, businessSkuKeyWithMigrationOk22.getShopSku());
        var sourceNotExistsBusinessSkuKey = new BusinessSkuKey(
            5555,
            businessSkuKeyWithMigrationWithoutSource1.getShopSku()
        );

        var offer1 = createOffer(
            businessSkuKeyWithoutMigration.getBusinessId(), businessSkuKeyWithoutMigration.getShopSku());
        var offer2 = createOffer(
            businessSkuKeyWithMigrationOk2.getBusinessId(), businessSkuKeyWithMigrationOk2.getShopSku());
        var offer3 = createOffer(
            businessSkuKeyWithMigrationOk22.getBusinessId(), businessSkuKeyWithMigrationOk22.getShopSku());

        int serviceOfferToRemoveId1 = 777;
        var sourceOffer1 = createOffer(
            businessSkuKeyWithMigrationOk2.getBusinessId(), businessSkuKeyWithMigrationOk2.getShopSku())
            .addNewServiceOfferIfNotExistsForTests(
                OfferTestUtils.simpleSupplier()
                    .setId(serviceOfferToRemoveId1)
            );

        var sourceOffer2 = createOffer(
            businessSkuKeyWithMigrationOk22.getBusinessId(), businessSkuKeyWithMigrationOk22.getShopSku())
            .addNewServiceOfferIfNotExistsForTests(
                OfferTestUtils.simpleSupplier()
                    .setId(serviceOfferToRemoveId1)
            );

        MigrationStatus migrationStatus1 = new MigrationStatus();
        migrationStatus1.setId(1L);
        migrationStatus1.setSourceBusinessId(sourceNotExistsBusinessSkuKey.getBusinessId());
        migrationStatus1.setSupplierId(666);
        MigrationStatus migrationStatus2 = new MigrationStatus();
        migrationStatus2.setId(2L);
        migrationStatus2.setSourceBusinessId(sourceBusinessSkuKey1.getBusinessId());
        migrationStatus2.setSupplierId(serviceOfferToRemoveId1);

        MigrationOffer migrationOffer1 = new MigrationOffer();
        migrationOffer1.setShopSku(businessSkuKeyWithMigrationWithoutSource1.getShopSku());
        MigrationOffer migrationOffer2 = new MigrationOffer();
        migrationOffer2.setShopSku(businessSkuKeyWithMigrationOk2.getShopSku());
        MigrationOffer migrationOffer3 = new MigrationOffer();
        migrationOffer3.setShopSku(businessSkuKeyWithMigrationOk22.getShopSku());

        when(offerRepository.findOffersByBusinessSkuKeys(List.of(
            businessSkuKeyWithMigrationOk2,
            businessSkuKeyWithMigrationOk22,
            businessSkuKeyWithMigrationWithoutSource1
        )))
            .thenReturn(List.of(
                offer1, offer2, offer3)
            );
        when(migrationService.isInMigrationCached(anyInt()))
            .thenAnswer((Answer<Boolean>) invocation ->
                !Objects.equals(invocation.getArgument(0), businessSkuKeyWithoutMigration.getBusinessId())
            );
        when(migrationService.getMigrationsForReceivingByTarget(
            migratingOfferKeys.stream()
                .map(BusinessSkuKey::getBusinessId)
                .collect(Collectors.toSet())
        ))
            .thenReturn(Map.of(
                businessSkuKeyWithMigrationWithoutSource1.getBusinessId(), migrationStatus1,
                businessSkuKeyWithMigrationOk2.getBusinessId(), migrationStatus2
            ));
        when(migrationService.getMigrationOffersAsMap(migrationStatus1.getId(),
            List.of(migrationOffer1.getShopSku())))
            .thenReturn(Map.of(migrationOffer1.getShopSku(), migrationOffer1));
        when(migrationService.getMigrationOffersAsMap(migrationStatus2.getId(),
            List.of(migrationOffer3.getShopSku(), migrationOffer2.getShopSku())))
            .thenReturn(Map.of(
                migrationOffer2.getShopSku(), migrationOffer2,
                migrationOffer3.getShopSku(), migrationOffer3
            ));
        when(offerRepository.findOffersByBusinessSkuKeysAsMap(anySet()))
            .thenReturn(Map.of(
                sourceBusinessSkuKey1, sourceOffer1,
                sourceBusinessSkuKey2, sourceOffer2
            ));
        when(offerRepository.updateOffers(anySet()))
            .thenReturn(2);

        removeOfferService.removeOffers(businessSkuKeys);

        verify(migrationService, times(1)).updateOfferWithState(
            argThat(m -> new ArrayList<>(m).equals(List.of(migrationOffer1, migrationOffer2, migrationOffer3))),
            eq(MigrationOfferState.WARNING));
        verify(offerRepository, times(1)).updateOffers(new HashSet<>(List.of(
            sourceOffer1.removeServiceOfferIfExistsForTests(serviceOfferToRemoveId1),
            sourceOffer2.removeServiceOfferIfExistsForTests(serviceOfferToRemoveId1)
        )));
    }

    @Test
    public void removeOffersWillSkipNotDCAnd1POffers() {
        var shopSkuNotDC = new BusinessSkuKey(1111, "shopSkuNotDC");
        var shopSku1P = new BusinessSkuKey(2222, "shopSku1P");
        var businessSkuKeys = new HashSet<>(List.of(shopSkuNotDC, shopSku1P));

        var offerNotDC = createOffer(shopSkuNotDC.getBusinessId(), shopSkuNotDC.getShopSku())
            .setDataCampOffer(false);
        var offer1p = createOffer(shopSku1P.getBusinessId(), shopSku1P.getShopSku())
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.realSupplier());

        when(offerRepository.findOffersByBusinessSkuKeys(businessSkuKeys))
            .thenReturn(List.of(
                offerNotDC, offer1p
            ));
        doNothing().when(datacampImportService)
            .markAsImported(businessSkuKeys);

        removeOfferService.removeOffers(businessSkuKeys);

        var allRemoved = removedOfferRepository.findAll();
        assertThat(allRemoved).isEmpty();

        verify(removedOfferRepository, never()).save(anyCollection());
        verify(datacampImportService, never()).markAsImported(anySet());
    }

    @Test
    public void removeOffersWillSkipAlreadyRemoved() {
        var notMarkedForRemoveBsk = new BusinessSkuKey(1111, "shopSku1");
        var markedForRemoveBsk = new BusinessSkuKey(2222, "shopSku2");
        var businessSkuKeys = new HashSet<>(List.of(notMarkedForRemoveBsk, markedForRemoveBsk));

        var notMarkedForRemoveOffer = createOffer(notMarkedForRemoveBsk.getBusinessId(),
            notMarkedForRemoveBsk.getShopSku());
        var markedForRemoveOffer = createOffer(markedForRemoveBsk.getBusinessId(), markedForRemoveBsk.getShopSku());

        when(offerRepository.findOffersByBusinessSkuKeys(businessSkuKeys))
            .thenReturn(List.of(
                notMarkedForRemoveOffer, markedForRemoveOffer
            ));
        doNothing().when(datacampImportService)
            .markAsImported(businessSkuKeys);

        var expectedRemovedOffer = removedOfferRepository.save(new RemovedOffer(markedForRemoveOffer.getId(),
            Instant.now(),
            null, null, null));

        removeOfferService.removeOffers(businessSkuKeys);

        var allRemoved = removedOfferRepository.findAll();
        assertThat(allRemoved).hasSize(2);

        var resultRemovedOffer = allRemoved.stream()
            .filter(removedOffer -> removedOffer.getId().equals(expectedRemovedOffer.getId()))
            .findFirst().orElseThrow();

        assertEquals(expectedRemovedOffer, resultRemovedOffer);
    }

    private Offer createOffer(Integer businessId, String shopSku) {
        return new Offer()
            .setId(offerIdsGenerator.incrementAndGet())
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setDataCampOffer(true);
    }
}
