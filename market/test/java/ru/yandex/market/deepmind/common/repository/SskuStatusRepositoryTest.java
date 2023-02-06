package ru.yandex.market.deepmind.common.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.ShopSkuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.ShopSkuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.mocks.SskuStatusAuditServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusDeletedRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepositoryImpl;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mboc.common.audit.OfferStatusAuditInfoRead;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

public class SskuStatusRepositoryTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final String[] FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS = new String[]{
        "modifiedAt",
        "statusStartAt",
        "statusFinishAt",
        "plannedStartAt",
        "plannedFinishAt",
        "hasNoPurchasePrice",
        "modifiedByUser",
        "hasNoValidContract"
    };

    @Resource
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private ShopSkuAvailabilityChangedHandler shopSkuAvailabilityChangedHandler;
    @Resource
    private SskuStatusDeletedRepository sskuStatusDeletedRepository;

    private SskuStatusAuditServiceMock sskuStatusAuditServiceMock;

    private SskuStatusRepositoryImpl sskuStatusRepository;

    @Before
    public void setUp() {
        sskuStatusAuditServiceMock = new SskuStatusAuditServiceMock();
        sskuStatusRepository = new SskuStatusRepositoryImpl(namedJdbcTemplate, transactionTemplate,
            sskuStatusDeletedRepository, sskuStatusAuditServiceMock);
        sskuStatusRepository.setChangedSskuHandler(shopSkuAvailabilityChangedHandler);
        SecurityContextAuthenticationUtils.setAuthenticationToken();
    }

    @After
    public void tearDown() {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
    }

    @Test
    public void testSave() {
        sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.ACTIVE, "comment #1"));

        List<SskuStatus> statuses = sskuStatusRepository.findAll();
        assertThat(statuses)
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability", "comment")
            .containsExactly(
                offerStatus(1, "sku1", OfferAvailability.ACTIVE, "comment #1")
            );
        assertThat(statuses.get(0).getModifiedAt()).isNotNull();
    }

    @Test
    public void testChangeAvailability() {
        var sku1 = sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.ACTIVE, "comment #1"));
        var afterCreate = sskuStatusRepository.findByKey(1, "sku1").orElseThrow();

        sskuStatusRepository.save(sku1.setAvailability(OfferAvailability.DELISTED));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        var afterUpdate = sskuStatusRepository.findByKey(1, "sku1").orElseThrow();

        assertThat(afterUpdate.getAvailability()).isEqualTo(OfferAvailability.DELISTED);
        assertThat(afterUpdate.getModifiedAt()).isAfter(afterCreate.getModifiedAt());
    }

    @Test
    public void testDontCreateEventsOnDoubleSave() {
        var sku1 = sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.DELISTED, "comment #1"));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        sskuStatusRepository.save(sku1.setComment("comment #2"));
        assertThatAvailabilityTaskQueueIsEmpty();
    }

    @Test
    public void testCreateEvents() {
        // create DELISTED
        var ssku1 = sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.DELISTED, ""));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        // DELISTED -> ACTIVE
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.ACTIVE));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        // ACTIVE -> INACTIVE
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.INACTIVE));
        assertThatAvailabilityTaskQueueIsEmpty();

        // INACTIVE -> ACTIVE
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.ACTIVE));
        assertThatAvailabilityTaskQueueIsEmpty();

        // ACTIVE -> DELISTED
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.DELISTED));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        // DELISTED -> DELISTED
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.DELISTED));
        assertThatAvailabilityTaskQueueIsEmpty();

        // DELISTED -> INACTIVE
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.INACTIVE));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        // INACTIVE -> INACTIVE_TMP
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.INACTIVE_TMP));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        // INACTIVE_TMP -> PENDING
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.PENDING));
        assertThatAvailabilityTaskQueueContains(new ServiceOfferKey(1, "sku1"));
        clearQueue();

        // PENDING -> ACTIVE
        ssku1 = sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.ACTIVE));
        assertThatAvailabilityTaskQueueIsEmpty();
    }

    @Test(expected = SqlConcurrentModificationException.class)
    public void testOptimisticLockException() {
        var saved = sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.ACTIVE, "comment #1"));
        saved
            .setAvailability(OfferAvailability.DELISTED)
            .setModifiedAt(saved.getModifiedAt().minusSeconds(1)); // make outdated

        sskuStatusRepository.save(saved);
    }

    @Test(expected = SqlConcurrentModificationException.class)
    public void testOptimisticLockExceptionIfModifiedAtNull() {
        var saved = sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.ACTIVE, "comment #1"));
        saved
            .setAvailability(OfferAvailability.DELISTED)
            .setModifiedAt(null);

        sskuStatusRepository.save(List.of(saved));
    }

    @Test
    public void testAuditInfo() {
        var ssku1 = sskuStatusRepository.save(
            offerStatus(1, "sku1", OfferAvailability.ACTIVE, "created status")
        );
        assertThat(sskuStatusAuditServiceMock.getInfo())
            .usingElementComparatorIgnoringFields("modifiedTs", "newFinishStatusTime")
            .containsExactly(
                new OfferStatusAuditInfoRead()
                    .setAuthor("test-user")
                    .setShopSkuKey(new ShopSkuKey(1, "sku1"))
                    .setOldValue(null, null)
                    .setNewValue("ACTIVE", "created status", null)
            );

        sskuStatusRepository.save(List.of(
            ssku1.setAvailability(OfferAvailability.INACTIVE).setComment("modified status"),
            offerStatus(2, "sku2", OfferAvailability.DELISTED, "created new status")
        ));
        assertThat(sskuStatusAuditServiceMock.getInfo())
            .usingElementComparatorIgnoringFields("modifiedTs", "newFinishStatusTime")
            .containsExactlyInAnyOrder(
                new OfferStatusAuditInfoRead()
                    .setAuthor("test-user")
                    .setShopSkuKey(new ShopSkuKey(1, "sku1"))
                    .setOldValue("ACTIVE", "created status")
                    .setNewValue("INACTIVE", "modified status", null),
                new OfferStatusAuditInfoRead()
                    .setAuthor("test-user")
                    .setShopSkuKey(new ShopSkuKey(2, "sku2"))
                    .setOldValue(null, null)
                    .setNewValue("DELISTED", "created new status", null)
            );
    }

    @Test
    public void testWriteFinishStatusAtInAudit() {
        var statusFinishAt = Instant.now();
        sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.ACTIVE, "created status")
                .setStatusFinishAt(statusFinishAt));
        assertThat(sskuStatusAuditServiceMock.getInfo())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactly(
                new OfferStatusAuditInfoRead()
                    .setAuthor("test-user")
                    .setShopSkuKey(new ShopSkuKey(1, "sku1"))
                    .setOldValue(null, null)
                    .setNewValue("ACTIVE", "created status", statusFinishAt)
            );
    }

    @Test
    public void testFindWithManyKeys() {
        sskuStatusRepository.save(
            offerStatus(1, "sku1", OfferAvailability.ACTIVE, ""),
            offerStatus(2, "sku2", OfferAvailability.INACTIVE, "")
        );
        var keys = new ArrayList<ServiceOfferKey>();
        for (int i = 1; i <= 100_000; i++) {
            keys.add(new ServiceOfferKey(i, "sku" + i));
        }

        var result = sskuStatusRepository.find(keys);

        assertThat(result)
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability", "comment")
            .containsExactlyInAnyOrder(
                offerStatus(1, "sku1", OfferAvailability.ACTIVE, ""),
                offerStatus(2, "sku2", OfferAvailability.INACTIVE, "")
            );
    }

    @Test
    public void testCountByStatus() {
        sskuStatusRepository.save(
            offerStatus(1, "sku1", OfferAvailability.ACTIVE, ""),
            offerStatus(1, "sku2", OfferAvailability.DELISTED, ""),
            offerStatus(1, "sku3", OfferAvailability.DELISTED, ""),
            offerStatus(1, "sku4", OfferAvailability.INACTIVE, ""),
            offerStatus(1, "sku5", OfferAvailability.INACTIVE, ""),
            offerStatus(1, "sku6", OfferAvailability.INACTIVE, "")
        );

        var result = sskuStatusRepository.countByStatus();

        assertThat(result).containsOnly(
            Map.entry(OfferAvailability.ACTIVE, 1),
            Map.entry(OfferAvailability.DELISTED, 2),
            Map.entry(OfferAvailability.INACTIVE, 3)
        );
    }

    @Test
    public void testSaveStatusFinishAt() {
        var finishAt = Instant.parse("2022-12-03T10:15:30Z");
        var sku1 = sskuStatusRepository.save(offerStatus(1, "sku1", OfferAvailability.INACTIVE, "")
            .setStatusFinishAt(finishAt));

        assertThat(sku1.getStatusFinishAt()).isEqualTo(finishAt);
    }

    @Test
    public void statusAreBeingDeleted() {
        //arrange
        var statusToSave = offerStatus(1, "sku1", OfferAvailability.ACTIVE, "");
        var statusToDelete =  offerStatus(2, "sku2", OfferAvailability.ACTIVE, "");
        sskuStatusRepository.save(statusToSave, statusToDelete);

        //act
        sskuStatusRepository.delete(statusToDelete);

        //assert that status has been deleted
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(statusToSave);

        //assert that status has been stored as deleted
        var deletedKey = extractShopSkuKey(statusToDelete);
        Assertions.assertThat(sskuStatusDeletedRepository.findAllKeys())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(deletedKey);
    }

    @Test
    public void statusAreBeingRestored() {
        //arrange
        var statusToRestore = offerStatus(1, "sku1", OfferAvailability.ACTIVE, "");
        sskuStatusDeletedRepository.add(statusToRestore);

        //act
        sskuStatusRepository.save(statusToRestore);

        //assert that status has been restored
        Assertions.assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorIgnoringFields(FIELDS_TO_IGNORE_WHEN_COMPARING_STATUS)
            .containsExactly(statusToRestore);

        //assert that status has been removed from deleted
        Assertions.assertThat(sskuStatusDeletedRepository.findAllKeys())
            .isEmpty();
    }

    private ServiceOfferKey extractShopSkuKey(SskuStatus sskuStatus) {
        return new ServiceOfferKey(sskuStatus.getSupplierId(), sskuStatus.getShopSku());
    }

    private SskuStatus offerStatus(int supplierId, String shopSku, OfferAvailability availability, String comment) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(
                ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.valueOf(
                    availability.name())
            )
            .setComment(comment);
    }

    private void assertThatAvailabilityTaskQueueContains(ServiceOfferKey... shopSkuKeys) {
        var tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        var sskuKeysToRefresh = tasks.stream().map(ShopSkuAvailabilityChangedTask::getShopSkuKeys)
            .flatMap(Collection::stream).collect(Collectors.toList());
        assertThat(sskuKeysToRefresh)
            .containsExactlyInAnyOrder(shopSkuKeys);
    }

    private void assertThatAvailabilityTaskQueueIsEmpty() {
        var tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        assertThat(tasks).isEmpty();
    }
}
