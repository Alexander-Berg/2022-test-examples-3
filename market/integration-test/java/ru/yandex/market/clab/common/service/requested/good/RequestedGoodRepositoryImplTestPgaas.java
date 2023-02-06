package ru.yandex.market.clab.common.service.requested.good;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.common.service.ConcurrentModificationException;
import ru.yandex.market.clab.common.service.category.CategoryRepository;
import ru.yandex.market.clab.common.service.requested.movement.RequestedMovementRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodMbocState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodMovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodMovement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodLogistics;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedMovement;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RequestedGoodRepositoryImplTestPgaas extends BasePgaasIntegrationTest {
    @Autowired
    private RequestedGoodRepository goodRepository;

    @Autowired
    private RequestedMovementRepository movementRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ControlledClock clock;

    @Test
    public void simpleInsertGood() {
        RequestedGood good = createGood();

        RequestedGood saved = goodRepository.save(good);
        assertThat(good.getId()).withFailMessage("should not affect original object").isNull();
        assertThat(good.getModifiedDate()).withFailMessage("should not affect original object").isNull();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getModifiedDate()).isNotNull();
        assertThat(saved.setId(null).setModifiedDate(null)).isEqualTo(good);
    }

    @Test
    public void simpleUpdateGood() {
        RequestedGood good = createGood();

        RequestedGood saved = goodRepository.save(good);

        Long id = saved.getId();

        saved.setComment(RandomTestUtils.randomString());
        clock.tickMinute();
        RequestedGood updated = goodRepository.save(saved);
        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getModifiedDate()).isAfter(saved.getModifiedDate());

        RequestedGood fetched = goodRepository.getById(id);
        assertThat(fetched).isEqualTo(updated);
    }

    @Test
    public void batchUpdateGoodUpdatesModifiedDate() {
        RequestedGood saved = goodRepository.save(createGood());
        saved.setComment(RandomTestUtils.randomString());

        clock.tickMinute();
        goodRepository.save(Collections.singleton(saved));

        RequestedGood fetched = goodRepository.getById(saved.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getModifiedDate()).isAfter(saved.getModifiedDate());
    }

    @Test
    public void goodOptimisticLocking() {
        RequestedGood saved = goodRepository.save(createGood());

        goodRepository.save(saved);

        assertThatThrownBy(() -> {

            saved.setComment(RandomTestUtils.randomString());
            goodRepository.save(saved);

        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void goodOptimisticLockBatchUpdate() {
        RequestedGood good1 = goodRepository.save(createGood().setComment("comment1"));
        RequestedGood good2 = goodRepository.save(createGood().setComment("comment2"));
        goodRepository.save(good1);

        good1.setComment("newcomment1");
        good2.setComment("newcomment2");

        assertThatThrownBy(() -> {
            goodRepository.save(Arrays.asList(good1, good2));
        }).isInstanceOf(ConcurrentModificationException.class);

        RequestedGood actualGood1 = goodRepository.getById(good1.getId());
        RequestedGood actualGood2 = goodRepository.getById(good2.getId());

        assertThat(actualGood1).isNotNull();
        assertThat(actualGood2).isNotNull();
        assertThat(actualGood1.getComment()).isEqualTo("comment1");
        // Unfortunaetly spring test creates transaction on test method and rollbacks it after exit from tst method.
        // We can't create nested transaction, so we can only assert on state inside the transaction.
        assertThat(actualGood2.getComment()).isEqualTo("newcomment2");
    }

    @Test
    public void saveAndGetLogistics() {
        RequestedGood good = createAndSaveGood();
        RequestedGood good2 = createAndSaveGood();

        List<Long> goodIds = Stream.of(good, good2)
            .map(RequestedGood::getId)
            .collect(Collectors.toList());

        RequestedGoodLogistics g1w1 = createStockLogistics(good, 145L).setInboundAllowed(false);
        RequestedGoodLogistics g1w2 = createStockLogistics(good, 163L).setInboundAllowed(false);
        RequestedGoodLogistics g2w1 = createStockLogistics(good2, 145L).setInboundAllowed(false);
        RequestedGoodLogistics g2w2 = createStockLogistics(good2, 163L).setInboundAllowed(false);

        List<RequestedGoodLogistics> requestedGoodLogistics = Arrays.asList(g1w1, g1w2, g2w1, g2w2);

        goodRepository.upsertStocks(requestedGoodLogistics);

        List<RequestedGoodLogistics> loadedStocks = goodRepository.getLogistics(145L, goodIds);

        assertThat(loadedStocks).containsExactlyInAnyOrder(g1w1, g2w1);

        loadedStocks = goodRepository.getLogistics(163L, Collections.singletonList(good.getId()));

        assertThat(loadedStocks).containsExactlyInAnyOrder(g1w2);
    }

    @Test
    public void createAndGetGoodMovements() {
        RequestedMovement movement = createAndSaveMovement();
        RequestedMovement movement2 = createAndSaveMovement();

        RequestedGood good = createAndSaveGood(movement);
        RequestedGood good2 = createAndSaveGood(movement);

        RequestedGoodMovement good1Movement1 = createGoodMovement(movement, good);
        RequestedGoodMovement good2Movement1 = createGoodMovement(movement, good2);
        RequestedGoodMovement good1Movement2 = createGoodMovement(movement2, good);
        RequestedGoodMovement good2Movement2 = createGoodMovement(movement2, good2);

        List<RequestedGoodMovement> goodMovements =
            Arrays.asList(good1Movement1, good1Movement2, good2Movement1, good2Movement2);
        List<RequestedGoodMovement> created = goodRepository.createGoodMovements(goodMovements);

        assertThat(created).allSatisfy(c -> {
            assertThat(c.getModifiedDate()).isNotNull();
        });

        List<RequestedGoodMovement> filtered =
            goodRepository.getGoodMovements(movement2.getId(), Arrays.asList(good.getId(), good2.getId()));

        assertThat(filtered)
            .extracting(f -> f.setModifiedDate(null))
            .containsExactlyInAnyOrder(good1Movement2, good2Movement2);
    }

    @Test
    public void updateGoodMovements() {
        RequestedMovement movement = createAndSaveMovement();

        RequestedGood good = createAndSaveGood(movement);

        RequestedGoodMovement goodMovement = createGoodMovement(movement, good);

        RequestedGoodMovement created = goodRepository.createGoodMovement(goodMovement);

        created.setState(RandomTestUtils.randomObject(RequestedGoodMovementState.class));
        created.setStateMessage(RandomTestUtils.randomString());

        clock.tickMinute();
        RequestedGoodMovement updated = goodRepository.updateGoodMovement(created);

        assertThat(updated.getModifiedDate()).isAfter(created.getModifiedDate());

        assertThatThrownBy(() -> {
            goodRepository.updateGoodMovement(created);
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void removeGoodMovements() {
        RequestedMovement movement = createAndSaveMovement();

        RequestedGood good = createAndSaveGood(movement);
        RequestedGood good2 = createAndSaveGood(movement);

        RequestedGoodMovement goodMovement = createGoodMovement(movement, good);
        RequestedGoodMovement good2Movement = createGoodMovement(movement, good2);

        goodRepository.createGoodMovements(Arrays.asList(goodMovement, good2Movement));

        goodRepository.removeGoodMovements(movement.getId(), Collections.singletonList(good.getId()));

        List<RequestedGoodMovement> good1Movements =
            goodRepository.getGoodMovements(movement.getId(), Collections.singletonList(good.getId()));
        List<RequestedGoodMovement> good2Movements =
            goodRepository.getGoodMovements(movement.getId(), Collections.singletonList(good2.getId()));

        assertThat(good1Movements).isEmpty();
        assertThat(good2Movements).isNotEmpty();

        goodRepository.removeGoodMovements(movement.getId());

        good2Movements =
            goodRepository.getGoodMovements(movement.getId(), Collections.singletonList(good2.getId()));

        assertThat(good2Movements).isEmpty();
    }

    @Test
    public void findGoods() {
        RequestedMovement movement = createAndSaveMovement();

        RequestedGood good = goodRepository.save(
            new RequestedGood()
                .setRequestedMovementId(movement.getId())
                .setMbocState(RequestedGoodMbocState.CL_PLANNING)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_READY)
                .setGoodId(1L)
                .setCategoryId(2L)
                .setMskuId(3L)
                .setSupplierId(2L)
                .setSupplierSkuId("qweqwe"));

        RequestedGood good2 = goodRepository.save(
            new RequestedGood()
                .setMbocState(RequestedGoodMbocState.CL_FAILED)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_FAILED)
                .setGoodId(2L)
                .setCategoryId(5L)
                .setMskuId(6L)
                .setSupplierId(3L)
                .setSupplierSkuId("qweqwe2"));

        goodRepository.createGoodMovement(createGoodMovement(movement, good)
            .setState(RequestedGoodMovementState.PLANNED));

        List<RequestedGood> foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addGoodId(1L));
        assertThat(foundGoods).containsExactly(good);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().setMovementId(movement.getId()));
        assertThat(foundGoods).containsExactly(good);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addId(good2.getId()));
        assertThat(foundGoods).containsExactly(good2);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addMbocState(RequestedGoodMbocState.CL_FAILED));
        assertThat(foundGoods).containsExactly(good2);

        foundGoods = goodRepository.findGoods(new RequestedGoodFilter()
            .addMovementState(RequestedGoodMovementState.PLANNED));
        assertThat(foundGoods).containsExactly(good);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addMskuId(3L));
        assertThat(foundGoods).containsExactly(good);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().setHasMovement(false));
        assertThat(foundGoods).containsExactly(good2);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addSskuIds("qweqwe"));
        assertThat(foundGoods).containsExactly(good);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addCategoryId(2L));
        assertThat(foundGoods).containsExactly(good);

        foundGoods = goodRepository.findGoods(
            new RequestedGoodFilter().addSskuIds("qwewqeqwewrfd"));
        assertThat(foundGoods).isEmpty();
    }

    @Test
    public void findGoodInfos() {

        Category category = RandomTestUtils.randomObject(Category.class);
        categoryRepository.create(category);
        Category category2 = RandomTestUtils.randomObject(Category.class);
        categoryRepository.create(category2);

        RequestedMovement movement = createAndSaveMovement();

        RequestedGood good = goodRepository.save(
            new RequestedGood()
                .setRequestedMovementId(movement.getId())
                .setMbocState(RequestedGoodMbocState.CL_PLANNING)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_READY)
                .setGoodId(1L)
                .setCategoryId(category.getId())
                .setMskuId(3L)
                .setSupplierId(2L)
                .setSupplierSkuId("qweqwe"));

        RequestedGoodMovement goodMovement = goodRepository
            .createGoodMovement(createGoodMovement(movement, good)
                .setState(RequestedGoodMovementState.PLANNED));

        RequestedGood good2 = goodRepository.save(
            new RequestedGood()
                .setMbocState(RequestedGoodMbocState.CL_FAILED)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_FAILED)
                .setGoodId(2L)
                .setCategoryId(category2.getId())
                .setMskuId(6L)
                .setSupplierId(3L)
                .setSupplierSkuId("qweqwe2"));

        long warehouseId = 145L;
        goodRepository.upsertStocks(Arrays.asList(
            createStockLogistics(good, warehouseId),
            createStockLogistics(good2, warehouseId)
        ));

        RequestedGoodInfo goodInfo = new RequestedGoodInfo(good, goodMovement, category.getName());
        List<RequestedGoodLogisticsInfo> stockInfos = goodRepository
            .getLogistics(warehouseId, Collections.singletonList(good.getId()))
            .stream()
            .map(s -> new RequestedGoodLogisticsInfo(s, "Маршрут ФФ"))
            .collect(Collectors.toList());
        stockInfos.forEach(goodInfo::addStocks);

        RequestedGoodInfo good2Info = new RequestedGoodInfo(good2, null, category2.getName());
        stockInfos = goodRepository
            .getLogistics(warehouseId, Collections.singletonList(good2.getId()))
            .stream()
            .map(s -> new RequestedGoodLogisticsInfo(s, "Маршрут ФФ"))
            .collect(Collectors.toList());
        stockInfos.forEach(good2Info::addStocks);

        List<RequestedGoodInfo> foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addGoodId(1L));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().setMovementId(movement.getId()));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addId(good2.getId()));
        assertThat(foundGoods).containsExactly(good2Info);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addMbocState(RequestedGoodMbocState.CL_FAILED));
        assertThat(foundGoods).containsExactly(good2Info);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addMovementState(RequestedGoodMovementState.PLANNED));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addMskuId(3L));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().setHasMovement(false));
        assertThat(foundGoods).containsExactly(good2Info);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addSskuIds("qweqwe"));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addCategoryId(category.getId()));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().addCategoryId(12323L));
        assertThat(foundGoods).isEmpty();
    }

    @Test
    public void findGoodInfoMultipleMovements() {
        Category category = RandomTestUtils.randomObject(Category.class);
        categoryRepository.create(category);

        RequestedMovement movement = createAndSaveMovement();

        RequestedMovement movement2 = createAndSaveMovement();

        RequestedGood good = goodRepository.save(
            new RequestedGood()
                .setRequestedMovementId(movement.getId())
                .setMbocState(RequestedGoodMbocState.CL_PLANNING)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_READY)
                .setGoodId(1L)
                .setCategoryId(category.getId())
                .setMskuId(3L)
                .setSupplierId(2L)
                .setSupplierSkuId("qweqwe"));

        RequestedGoodMovement goodMovement = goodRepository
            .createGoodMovement(createGoodMovement(movement, good)
                .setState(RequestedGoodMovementState.PLANNED));

        RequestedGoodMovement goodMovement2 = goodRepository
            .createGoodMovement(createGoodMovement(movement2, good)
                .setState(RequestedGoodMovementState.REJECTED));

        RequestedGoodInfo goodInfoMovement1 = new RequestedGoodInfo(good, goodMovement, category.getName());
        RequestedGoodInfo goodInfoMovement2 = new RequestedGoodInfo(good, goodMovement2, category.getName());

        List<RequestedGoodInfo> foundGoods = goodRepository.findInfos(new RequestedGoodFilter());
        assertThat(foundGoods).containsExactly(goodInfoMovement1);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().setMovementId(movement.getId()));
        assertThat(foundGoods).containsExactly(goodInfoMovement1);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().setMovementId(movement2.getId()));
        assertThat(foundGoods).containsExactly(goodInfoMovement2);
    }

    @Test
    public void findGoodsByStocks() {
        Category category = RandomTestUtils.randomObject(Category.class);
        categoryRepository.create(category);

        RequestedMovement movement = createAndSaveMovement();

        RequestedGood good = goodRepository.save(
            new RequestedGood()
                .setRequestedMovementId(movement.getId())
                .setMbocState(RequestedGoodMbocState.CL_PLANNING)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_READY)
                .setGoodId(1L)
                .setCategoryId(category.getId())
                .setMskuId(3L)
                .setSupplierId(2L)
                .setSupplierSkuId("qweqwe"));

        RequestedGoodMovement goodMovement = goodRepository
            .createGoodMovement(createGoodMovement(movement, good)
                .setState(RequestedGoodMovementState.PLANNED));

        long warehouseId145 = 145L;
        goodRepository
            .upsertStocks(Collections.singletonList(createStockLogistics(good, warehouseId145).setFitCount(1)));
        long warehouseId163 = 163L;
        goodRepository
            .upsertStocks(Collections.singletonList(createStockLogistics(good, warehouseId163).setFitCount(0)));

        RequestedGoodInfo goodInfo = new RequestedGoodInfo(good, goodMovement, category.getName());
        List<RequestedGoodLogisticsInfo> stockInfos = goodRepository
            .getLogistics(warehouseId145, Collections.singletonList(good.getId()))
            .stream()
            .map(s -> new RequestedGoodLogisticsInfo(s, "Маршрут ФФ"))
            .collect(Collectors.toList());
        stockInfos.forEach(goodInfo::addStocks);

        stockInfos = goodRepository
            .getLogistics(warehouseId163, Collections.singletonList(good.getId()))
            .stream()
            .map(s -> new RequestedGoodLogisticsInfo(s, "Лаборатория Контента"))
            .collect(Collectors.toList());
        stockInfos.forEach(goodInfo::addStocks);

        List<RequestedGoodInfo> foundGoods = goodRepository.findInfos(new RequestedGoodFilter());
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().setWarehouseWithStocks(warehouseId145));
        assertThat(foundGoods).containsExactly(goodInfo);

        foundGoods = goodRepository.findInfos(
            new RequestedGoodFilter().setWarehouseWithStocks(warehouseId163));
        assertThat(foundGoods).isEmpty();
    }

    @Test
    public void countGoods() {
        RequestedMovement movement = createAndSaveMovement();

        RequestedGood good = goodRepository.save(
            new RequestedGood()
                .setRequestedMovementId(movement.getId())
                .setMbocState(RequestedGoodMbocState.CL_PLANNING)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_READY)
                .setSupplierId(2L)
                .setSupplierSkuId("qweqwe"));

        RequestedGood good2 = goodRepository.save(
            new RequestedGood()
                .setMbocState(RequestedGoodMbocState.CL_PLANNING)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_FAILED)
                .setSupplierId(3L)
                .setSupplierSkuId("qweqwe2"));

        RequestedGood good3 = goodRepository.save(
            new RequestedGood()
                .setMbocState(RequestedGoodMbocState.CL_FAILED)
                .setLastPushedMbocState(RequestedGoodMbocState.CL_FAILED)
                .setSupplierId(3L)
                .setSupplierSkuId("qweqwe2"));

        goodRepository.createGoodMovement(createGoodMovement(movement, good)
            .setState(RequestedGoodMovementState.PLANNED));

        assertThat(goodRepository.count(new RequestedGoodFilter()
            .addMbocState(RequestedGoodMbocState.CL_PLANNING)))
            .isEqualTo(2);

        assertThat(goodRepository.count(new RequestedGoodFilter()
            .addMovementState(RequestedGoodMovementState.PLANNED)))
            .isEqualTo(1);

        assertThat(goodRepository.count(new RequestedGoodFilter()
            .addMovementState(RequestedGoodMovementState.IN_PROCESS)))
            .isEqualTo(0);
    }

    @Test
    public void findForUpdateLogisticsAvailability() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdateTs = now.minusDays(1);

        RequestedGood newGood = goodRepository.save(createGood().setState(RequestedGoodState.NEW));
        RequestedGood processedGood = goodRepository.save(createGood().setState(RequestedGoodState.PROCESSED));
        RequestedGood otherGood = goodRepository.save(createGood().setState(RequestedGoodState.PROCESSING));

        RequestedGoodLogistics ngW1 = createStockLogistics(newGood, 145L)
            .setInboundAllowed(true).setCheckAvailabilityTs(now.minusHours(1));
        RequestedGoodLogistics ngW2 = createStockLogistics(newGood, 163L);

        RequestedGoodLogistics pgW1 = createStockLogistics(processedGood, 145L)
            .setInboundAllowed(true).setCheckAvailabilityTs(now.minusDays(2));
        RequestedGoodLogistics pgW2 = createStockLogistics(processedGood, 163L)
            .setInboundAllowed(true).setCheckAvailabilityTs(now.minusDays(1));

        RequestedGoodLogistics ogW1 = createStockLogistics(otherGood, 145L);

        List<RequestedGoodLogistics> requestedGoodLogistics = Arrays.asList(ngW1, ngW2, pgW1, pgW2, ogW1);
        goodRepository.upsertInboundAvailability(requestedGoodLogistics);

        List<RequestedGood> forUpdateW1Processed = goodRepository
            .findForUpdateLogisticsAvailability(145L, RequestedGoodState.PROCESSED, lastUpdateTs);

        assertThat(forUpdateW1Processed).containsOnly(processedGood);

        List<RequestedGood> forUpdateW2New = goodRepository
            .findForUpdateLogisticsAvailability(163L, RequestedGoodState.NEW, lastUpdateTs);

        assertThat(forUpdateW2New).containsOnly(newGood);
    }

    @Test
    public void upsertLogistics() {
        long warehouseId = 145L;
        RequestedGood good = createAndSaveGood();
        RequestedGoodLogistics forUpsert = createStockLogistics(good, warehouseId);
        RequestedGoodLogistics expectedFromDb = new RequestedGoodLogistics()
            .setWarehouseId(forUpsert.getWarehouseId())
            .setRequestedGoodId(forUpsert.getRequestedGoodId())
            .setFitCount(forUpsert.getFitCount())
            .setInboundAllowed(false);

        goodRepository.upsertStocks(Collections.singletonList(forUpsert));
        List<RequestedGoodLogistics> result = goodRepository
            .getLogistics(warehouseId, Collections.singletonList(good.getId()));

        //save new record
        assertThat(result).containsOnly(expectedFromDb);

        forUpsert.setInboundAllowed(true).setCheckAvailabilityTs(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        goodRepository.upsertStocks(Collections.singletonList(forUpsert));
        result = goodRepository.getLogistics(warehouseId, Collections.singletonList(good.getId()));

        //logistics availability should not updated
        assertThat(result).containsOnly(expectedFromDb);

        expectedFromDb.setInboundAllowed(true).setCheckAvailabilityTs(forUpsert.getCheckAvailabilityTs());

        goodRepository.upsertInboundAvailability(Collections.singletonList(forUpsert));
        result = goodRepository.getLogistics(warehouseId, Collections.singletonList(good.getId()));

        //logistics availability should updated
        assertThat(result).containsOnly(expectedFromDb);

        forUpsert.setFitCount(forUpsert.getFitCount() + 1);

        goodRepository.upsertInboundAvailability(Collections.singletonList(forUpsert));
        result = goodRepository.getLogistics(warehouseId, Collections.singletonList(good.getId()));

        //fit count should not updated
        assertThat(result).containsOnly(expectedFromDb);
    }

    private RequestedGood createGood() {
        return RandomTestUtils.randomObject(RequestedGood.class, "id", "modifiedDate");
    }

    private RequestedGood createAndSaveGood() {
        return goodRepository.save(createGood());
    }

    private RequestedGood createAndSaveGood(RequestedMovement movement) {
        return goodRepository.save(createGood()
            .setRequestedMovementId(movement.getId()));
    }

    private RequestedMovement createAndSaveMovement() {
        RequestedMovement movement = RandomTestUtils.randomObject(RequestedMovement.class, "id", "modifiedDate");
        return movementRepository.save(movement);
    }

    private RequestedGoodMovement createGoodMovement(RequestedMovement movement, RequestedGood good) {
        return RandomTestUtils.randomObject(RequestedGoodMovement.class, "modifiedDate")
            .setRequestedGoodId(good.getId())
            .setRequestedMovementId(movement.getId());
    }

    private RequestedGoodLogistics createStockLogistics(RequestedGood good, long warehouesId) {
        return RandomTestUtils.randomObject(RequestedGoodLogistics.class,
            "inboundAllowed", "reasonOfUnavailability", "checkAvailabilityTs")
            .setWarehouseId(warehouesId)
            .setRequestedGoodId(good.getId());
    }
}
