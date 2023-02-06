package ru.yandex.market.clab.common.service.movement;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clab.common.service.ProcessingException;
import ru.yandex.market.clab.common.service.barcode.SsBarcodeRepository;
import ru.yandex.market.clab.common.service.cart.CartService;
import ru.yandex.market.clab.common.service.good.ActionSource;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.good.GoodServiceImpl;
import ru.yandex.market.clab.common.service.mapping.MbocDeliveryService;
import ru.yandex.market.clab.common.service.mapping.MbocService;
import ru.yandex.market.clab.common.service.requested.good.GoodToCartAndMovementStateObserver;
import ru.yandex.market.clab.common.service.requested.good.GoodToRequestedGoodStateObserver;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepository;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepositoryStub;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodService;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodServiceImpl;
import ru.yandex.market.clab.common.service.warehouse.WarehouseRepository;
import ru.yandex.market.clab.common.service.warehouse.WarehouseService;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author abutusov
 * @since 9/8/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MovementServiceTest {

    private static final long WAREHOUSE_ID = 1;
    private static final String WAREHOUSE_NAME = "warehouse_name";
    private static final long GOOD_ID = 1;
    private static final String BARCODE = "barcode";
    private static final long GOOD_MSKU_ID = 12345678L;

    private static final long SEED = 9005089642L;

    private GoodRepository goodRepository;
    private RequestedGoodRepository requestedGoodRepository;
    private MovementRepository movementRepository;
    private MovementService movementService;

    private EnhancedRandom random;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();

        WarehouseRepository warehouseRepository = Mockito.mock(WarehouseRepository.class);
        Warehouse warehouse = new Warehouse().setName(WAREHOUSE_NAME);
        when(warehouseRepository.getById(WAREHOUSE_ID)).thenReturn(warehouse);

        requestedGoodRepository = new RequestedGoodRepositoryStub();
        RequestedGoodService requestedGoodService = new RequestedGoodServiceImpl(requestedGoodRepository,
            Mockito.mock(MbocService.class),
            Mockito.mock(MbocDeliveryService.class),
            Mockito.mock(WarehouseService.class));

        goodRepository = new GoodRepositoryStub();
        movementRepository = new MovementRepositoryStub(goodRepository, warehouseRepository);
        goodRepository.addObserver(new GoodToRequestedGoodStateObserver(
            requestedGoodService,
            movementRepository
        ));

        SsBarcodeRepository ssBarcodeRepository = Mockito.mock(SsBarcodeRepository.class);
        when(ssBarcodeRepository.getGoodIds(anyString())).thenReturn(Collections.singletonList(GOOD_ID));
        GoodService goodService = new GoodServiceImpl(goodRepository, ssBarcodeRepository);
        WarehouseService warehouseService = Mockito.mock(WarehouseService.class);
        when(warehouseService.getWarehouseById(WAREHOUSE_ID)).thenReturn(warehouse);
        movementService = new MovementServiceImpl(movementRepository, goodService, warehouseService);

        goodRepository.addObserver(new GoodToCartAndMovementStateObserver(
            Mockito.mock(CartService.class),
            movementService,
            goodRepository
        ));
    }

    @Test
    public void testGetMovement() {
        Movement expected = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);

        Movement actual = movementService.getMovement(expected.getId());

        assertThat(actual).isEqualTo(expected);
    }

    @Test(expected = ProcessingException.class)
    public void testGetNotExistedMovement() {
        movementService.getMovement(1L);
    }

    @Test
    public void testGetMovementWithStats() {
        Movement expectedMovement = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        long userId = random.nextLong();
        saveGood(GOOD_ID, expectedMovement.getId(), null, userId, GoodState.NEW, false);

        MovementWithStats actual = movementService.getMovementWithStats(expectedMovement.getId());

        assertThat(actual).extracting(MovementWithStats::getMovement).isEqualTo(expectedMovement);
        assertThat(actual).extracting(MovementWithStats::getWarehouseName).isEqualTo(WAREHOUSE_NAME);
        assertThat(actual).extracting(MovementWithStats::getItemCount).isEqualTo(1);
        assertThat(actual).extracting(MovementWithStats::getLastItemUpdateDate).isNull();
        assertThat(actual).extracting(MovementWithStats::getLastItemUpdateUid).isEqualTo(userId);
    }

    @Test(expected = ProcessingException.class)
    public void testGetNotExistedMovementWithStats() {
        movementService.getMovementWithStats(1L);
    }

    @Test
    public void testGetMovementsWithDefaultSort() {
        Movement expectedIncomingMovement = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        Movement expectedOutgoingMovement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID, expectedIncomingMovement.getId(), null, random.nextLong(), GoodState.NEW, false);
        saveGood(GOOD_ID + 1, null, expectedOutgoingMovement.getId(), random.nextLong(), GoodState.NEW, false);

        List<MovementWithStats> result = movementService.getMovements(
            new MovementFilter().addType(SupplierType.THIRD_PARTY));

        assertThat(result).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getId).reversed())
            .hasSize(2);
    }

    @Test
    public void testCreateMovement() {
        Movement movement = new Movement()
            .setDirection(MovementDirection.INCOMING);

        Movement result = movementService.createMovement(movement);

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.NEW);
        assertThat(result).extracting(Movement::getCreatedDate).isNotNull();
    }

    @Test(expected = ProcessingException.class)
    public void testCreateOutgoingMovementWithoutWarehouse() {
        Movement movement = new Movement()
            .setDirection(MovementDirection.OUTGOING);

        movementService.createMovement(movement);
    }

    @Test
    public void testSetMovementState() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);

        movementService.setMovementState(movement.getId(), MovementState.ACCEPTED);
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.ACCEPTED);
    }

    @Test(expected = ProcessingException.class)
    public void testSetInvalidMovementState() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);

        movementService.setMovementState(movement.getId(), MovementState.DONE);
    }

    @Test
    public void testGetGoodsNoData() {
        Good expected = saveGood(GOOD_ID, 1L, null, random.nextLong(), GoodState.NEW, false);

        List<Good> result = movementService.getGoodsNoData(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expected);
    }

    @Test
    public void testAcceptMovementWithNewGoods() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.NEW, false);

        movementService.acceptMovement(movement.getId());
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.NOTHING_RECEIVED);
    }

    @Test
    public void testAcceptMovementWithSortedToCartGoods() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.SORTED_TO_CART, false);

        movementService.acceptMovement(movement.getId());
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testAcceptMovementWithAcceptedGoods() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.ACCEPTED, false);

        movementService.acceptMovement(movement.getId());
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.ACCEPTED);
    }

    @Test(expected = ProcessingException.class)
    public void testAcceptOutgoingMovement() {
        Movement movement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID);

        movementService.acceptMovement(movement.getId());
    }

    @Test(expected = ProcessingException.class)
    public void testCancelAcceptMovementWithInvalidState() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);

        movementService.cancelAcceptMovement(movement.getId());
    }

    @Test
    public void testCancelAcceptAcceptedMovement() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.ACCEPTED, false);

        movementService.cancelAcceptMovement(movement.getId());
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.ACCEPTING);
    }

    @Test
    public void testCancelAcceptMovementWithNothingReceivedState() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NOTHING_RECEIVED, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.NOT_RECEIVED, false);

        movementService.cancelAcceptMovement(movement.getId());
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.NEW);
    }

    @Test(expected = ProcessingException.class)
    public void testCancelAcceptMovementWithGoodsOnCart() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.SORTED_TO_CART, false);

        movementService.cancelAcceptMovement(movement.getId());
    }

    @Test(expected = ProcessingException.class)
    public void testCancelAcceptOutgoingMovement() {
        Movement movement = saveMovement(MovementDirection.OUTGOING, MovementState.ACCEPTED, WAREHOUSE_ID);

        movementService.cancelAcceptMovement(movement.getId());
    }

    @Test
    public void testCancelSendMovement() {
        Movement movement = saveMovement(MovementDirection.OUTGOING, MovementState.SENDING, WAREHOUSE_ID);

        movementService.cancelSendMovement(movement.getId(), false);
        Movement result = movementService.getMovement(movement.getId());

        assertThat(result).extracting(Movement::getState).isEqualTo(MovementState.CANCELLED);
    }

    @Test(expected = ProcessingException.class)
    public void testCancelSendMovementWithInvalidState() {
        Movement movement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID);

        movementService.cancelSendMovement(movement.getId(), false);
    }

    @Test(expected = ProcessingException.class)
    public void testCancelSendIncomingMovement() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);

        movementService.cancelSendMovement(movement.getId(), false);
    }

    @Test(expected = ProcessingException.class)
    public void testAcceptGoodNotFromMovement() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);
        saveGood(GOOD_ID, null, null, random.nextLong(), GoodState.NEW, false);

        movementService.acceptGood(movement.getId(), BARCODE);
    }

    @Test(expected = ProcessingException.class)
    public void testAcceptGoodThatAlreadyHasBarcode() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.NEW, true);

        movementService.acceptGood(movement.getId(), BARCODE);
    }

    @Test(expected = ProcessingException.class)
    public void testAcceptAlreadyAcceptedGood() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTED, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.ACCEPTED, false);

        movementService.acceptGood(movement.getId(), BARCODE);
    }

    @Test
    public void testAcceptAllGoodInMovement() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.NEW, false);

        Good actualGood = movementService.acceptGood(movement.getId(), BARCODE);
        Movement actualMovement = movementService.getMovement(movement.getId());

        assertThat(actualGood.getState()).isEqualTo(GoodState.ACCEPTED);
        assertThat(actualMovement.getState()).isEqualTo(MovementState.ACCEPTED);
    }

    @Test
    public void testAcceptGoodManually() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.NEW, false);

        movementService.acceptGoodManually(GOOD_ID, BARCODE);
        List<Good> actualGoods = movementService.getGoodsNoData(movement.getId());
        Movement actualMovement = movementService.getMovement(movement.getId());

        assertThat(actualGoods).hasSize(1);
        assertThat(actualGoods.get(0).getState()).isEqualTo(GoodState.ACCEPTED);
        assertThat(actualMovement.getState()).isEqualTo(MovementState.ACCEPTED);
    }

    @Test
    public void testUndoAcceptGood() {
        Movement movement = saveMovement(MovementDirection.INCOMING, MovementState.ACCEPTING, WAREHOUSE_ID);
        saveGood(GOOD_ID, movement.getId(), null, random.nextLong(), GoodState.ACCEPTED, true);

        movementService.undoAcceptGood(GOOD_ID);
        List<Good> actualGoods = movementService.getGoodsNoData(movement.getId());
        Movement actualMovement = movementService.getMovement(movement.getId());

        assertThat(actualGoods).hasSize(1);
        assertThat(actualGoods.get(0).getState()).isEqualTo(GoodState.NEW);
        assertThat(actualGoods.get(0).getWhBarcode()).isNull();
        assertThat(actualMovement.getState()).isEqualTo(MovementState.NEW);
    }

    @Test(expected = ProcessingException.class)
    public void testAddToOutgoingWithInvalidWarehouse() {
        Movement incomingMovement = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        Movement outgoingMovement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID + 1);
        saveGood(GOOD_ID,
            incomingMovement.getId(),
            outgoingMovement.getId(),
            random.nextLong(),
            GoodState.PREPARED_TO_OUT,
            true);

        movementService.addToOutgoing(outgoingMovement.getId(), BARCODE);
    }

    @Test(expected = ProcessingException.class)
    public void testAddAlreadyIncludedGoodToOutgoing() {
        Movement incomingMovement = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        Movement outgoingMovement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID,
            incomingMovement.getId(),
            outgoingMovement.getId(),
            random.nextLong(),
            GoodState.PREPARED_TO_OUT,
            true);

        movementService.addToOutgoing(outgoingMovement.getId(), BARCODE);
    }

    @Test
    public void testAddToOutgoingFromNotInOutgoing() {
        Movement incomingMovement =
            saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID, 1L);
        Movement outgoingMovement =
            saveMovement(MovementDirection.OUTGOING, MovementState.SENDING, WAREHOUSE_ID, 1L);
        Movement newOutgoingMovement =
            saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID, 1L);

        Good good = saveGood(GOOD_ID,
            incomingMovement.getId(),
            outgoingMovement.getId(),
            random.nextLong(),
            GoodState.NOT_IN_OUTGOING,
            true);

        RequestedGood requestedGood = createAndSaveRequestedGood(good, RequestedGoodState.PLANNED_OUTGOING);

        Good addedGood = movementService.addToOutgoing(newOutgoingMovement.getId(), BARCODE);
        requestedGood = requestedGoodRepository.getById(requestedGood.getId());

        assertThat(addedGood.getState()).isEqualTo(GoodState.OUT);
        assertThat(requestedGood.getState()).isEqualTo(RequestedGoodState.PROCESSED);
        assertThat(requestedGood.getRequestedMovementId()).isNull();
    }

    @Test(expected = ProcessingException.class)
    public void testAddToOutgoingWithInvalidSupplier() {
        Movement incomingMovement = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        Movement outgoingMovement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW, WAREHOUSE_ID);
        saveGood(GOOD_ID,
            incomingMovement.getId(),
            outgoingMovement.getId(),
            random.nextLong(),
            GoodState.PREPARED_TO_OUT,
            true);

        movementService.addToOutgoing(outgoingMovement.getId(), BARCODE);
    }

    @Test
    public void testGetMovementByAcceptedGoodWh() {
        Movement expectedMovement = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        saveGood(GOOD_ID, expectedMovement.getId(), null, random.nextLong(), GoodState.ACCEPTED, true);

        List<MovementWithStats> result = movementService.searchMovementsByGood(BARCODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).extracting(MovementWithStats::getMovement).isEqualTo(expectedMovement);
        assertThat(result.get(0)).extracting(MovementWithStats::getItemCount).isEqualTo(1);
    }

    @Test
    public void testGetMovementByGoodMskuId() {
        Movement expectedMovement1 = saveMovement(MovementDirection.INCOMING, MovementState.DONE, WAREHOUSE_ID);
        Movement expectedMovement2 = saveMovement(MovementDirection.OUTGOING, MovementState.SENDING, WAREHOUSE_ID);

        saveGood(GOOD_ID, expectedMovement1.getId(), expectedMovement2.getId(),
            random.nextLong(), GoodState.ACCEPTED, false);

        List<MovementWithStats> result = movementService.searchMovementsByGood(String.valueOf(GOOD_MSKU_ID));

        assertThat(result).hasSize(2);
        List<Movement> movementList = result.stream().map(MovementWithStats::getMovement).collect(Collectors.toList());
        assertThat(movementList).containsOnly(expectedMovement1, expectedMovement2);
    }

    @Test(expected = ProcessingException.class)
    public void testAddToOutgoingWithSameBarcode() {
        Movement incomingMovement = saveMovement(MovementDirection.INCOMING, MovementState.PROCESSED, WAREHOUSE_ID);
        Movement outgoingMovement = saveMovement(MovementDirection.OUTGOING, MovementState.NEW,
            WAREHOUSE_ID + 1, 1L);
        saveGood(GOOD_ID,
            incomingMovement.getId(),
            outgoingMovement.getId(),
            random.nextLong(),
            GoodState.OUT,
            true);
        Good goodWithDuplicatedBarcode = saveGood(GOOD_ID + 11,
            incomingMovement.getId(),
            null,
            random.nextLong(),
            GoodState.PREPARED_TO_OUT,
            true);
        goodRepository.save(goodWithDuplicatedBarcode.setSupplierSkuId("qwerty11"));

        movementService.addToOutgoing(outgoingMovement.getId(), BARCODE);
    }

    private Movement saveMovement(MovementDirection direction,
                                  MovementState state,
                                  Long warehouseId) {
        Movement toSave = random.nextObject(Movement.class, "id", "modifiedDate")
            .setDirection(direction)
            .setState(state)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setWarehouseId(warehouseId);
        return movementRepository.save(toSave);
    }

    private Movement saveMovement(MovementDirection direction,
                                  MovementState state,
                                  Long warehouseId,
                                  Long supplierId) {
        Movement toSave = random.nextObject(Movement.class, "id", "modifiedDate")
            .setDirection(direction)
            .setState(state)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setWarehouseId(warehouseId)
            .setSupplierId(supplierId);
        return movementRepository.save(toSave);
    }

    private Good saveGood(Long id,
                          Long incomingId,
                          Long outgoingId,
                          long userId,
                          GoodState state,
                          boolean hasBarcode) {
        Good good = RandomTestUtils.randomObject(Good.class, "id", "modifiedDate")
            .setId(id)
            .setSupplierId(1L)
            .setSupplierSkuId("qwerty")
            .setIncomingMovementId(incomingId)
            .setOutgoingMovementId(outgoingId)
            .setModifiedUserId(userId)
            .setState(state)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setWhBarcode(hasBarcode ? BARCODE : null)
            .setMskuId(GOOD_MSKU_ID);
        return goodRepository.save(good, ActionSource.USER);
    }

    private RequestedGood createAndSaveRequestedGood(Good good,
                                                     RequestedGoodState state) {
        RequestedGood requestedGood = RandomTestUtils
            .randomObject(RequestedGood.class, "id", "modifiedDate")
            .setGoodId(good.getId())
            .setState(state)
            .setSupplierId(good.getSupplierId())
            .setSupplierSkuId(good.getSupplierSkuId());
        return requestedGoodRepository.save(requestedGood);
    }
}
