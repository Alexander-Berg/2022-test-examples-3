package ru.yandex.market.clab.common.service.requested.movement;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodMovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedMovementState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodMovement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedMovement;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RequestedMovementRepositoryTest extends BasePgaasIntegrationTest {

    @Autowired
    private RequestedMovementRepository movementRepository;

    @Autowired
    private RequestedGoodRepository goodRepository;

    private Set<RequestedMovement> savedMovements;

    private static final long USER_ID = 234324324L;

    @Before
    public void before() {
        savedMovements = new HashSet<>();
    }

    @Test
    public void saveMovements() {
        RequestedMovement movement = new RequestedMovement();
        movement.setWarehouseFromId(1L);
        movement.setWarehouseToId(2L);
        movement.setState(RequestedMovementState.NEW);
        movement.setDirection(MovementDirection.OUTGOING);
        RequestedMovement saved = movementRepository.save(movement);

        assertThat(saved.getId()).isNotNull().isGreaterThan(-1);
        assertThat(saved.getModifiedDate()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getWarehouseFromId()).isEqualTo(1L);
        assertThat(saved.getWarehouseToId()).isEqualTo(2L);
        assertThat(saved.getState()).isEqualTo(RequestedMovementState.NEW);
        assertThat(saved.getDirection()).isEqualTo(MovementDirection.OUTGOING);

        RequestedMovement fetched = movementRepository.getById(saved.getId());

        assertThat(fetched).isEqualToComparingFieldByField(saved);
    }

    @Test
    public void findMovements() {
        RequestedMovement incNew = saveMovement(MovementDirection.INCOMING, RequestedMovementState.NEW);
        RequestedMovement incPlanned = saveMovement(MovementDirection.INCOMING, RequestedMovementState.PLANNED);
        RequestedMovement incProcessed = saveMovement(MovementDirection.INCOMING, RequestedMovementState.PROCESSED);
        RequestedMovement outNew = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.NEW);
        RequestedMovement outReq = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.REQUESTED);
        RequestedMovement outInPr = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.IN_PROCESS);

        assertThat(movementRepository.find(new RequestedMovementFilter().addState(RequestedMovementState.NEW)))
            .extracting(RequestedMovement::getId)
            .containsExactlyInAnyOrder(
                incNew.getId(),
                outNew.getId()
            );

        assertThat(movementRepository.find(new RequestedMovementFilter().setDirection(MovementDirection.OUTGOING)))
            .extracting(RequestedMovement::getId)
            .containsExactlyInAnyOrder(
                outNew.getId(),
                outReq.getId(),
                outInPr.getId()
            );

        assertThat(movementRepository.find(new RequestedMovementFilter()
            .addId(incProcessed.getId())
            .addId(outReq.getId())))
            .extracting(RequestedMovement::getId)
            .containsExactlyInAnyOrder(
                incProcessed.getId(),
                outReq.getId()
            );

        assertThat(
            movementRepository.find(
                new RequestedMovementFilter()
                    .setDirection(MovementDirection.INCOMING)
                    .addState(RequestedMovementState.PLANNED)
                    .addId(incPlanned.getId())))
            .extracting(RequestedMovement::getId)
            .containsExactlyInAnyOrder(incPlanned.getId());
    }

    @Test
    public void fetchWithStats() {
        RequestedMovement incNew = saveMovement(MovementDirection.INCOMING, RequestedMovementState.NEW);
        RequestedMovement incPlanned = saveMovement(MovementDirection.INCOMING, RequestedMovementState.PLANNED);
        RequestedMovement incProcessed = saveMovement(MovementDirection.INCOMING, RequestedMovementState.PROCESSED);
        RequestedMovement outNew = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.NEW);
        RequestedMovement outReq = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.REQUESTED);
        RequestedMovement outInPr = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.IN_PROCESS,
            163L, 145L);

        saveGoodMovement(incNew);
        saveGoodMovement(incNew);
        saveGoodMovement(incNew);
        saveGoodMovement(incPlanned);
        saveGoodMovement(incPlanned);
        saveGoodMovement(incProcessed);
        saveGoodMovement(incProcessed);
        saveGoodMovement(outNew);
        saveGoodMovement(outReq);

        List<RequestedMovementWithStats> movementsWithId1 = movementRepository.findStats(
            new RequestedMovementFilter().addId(incNew.getId()));

        assertThat(movementsWithId1)
            .extracting(RequestedMovementWithStats::getMovement)
            .extracting(RequestedMovement::getId)
            .containsExactly(incNew.getId());

        assertThat(movementsWithId1).extracting(RequestedMovementWithStats::getGoodCount).containsExactly(3);

        List<RequestedMovementWithStats> foundMovements = movementRepository.findStats(
            new RequestedMovementFilter()
                .setDirection(MovementDirection.OUTGOING)
                .addState(RequestedMovementState.NEW).addState(RequestedMovementState.IN_PROCESS));

        assertThat(foundMovements)
            .extracting(RequestedMovementWithStats::getMovement)
            .extracting(RequestedMovement::getId)
            .containsExactlyInAnyOrder(
                outNew.getId(),
                outInPr.getId()
            );

        Map<Long, RequestedMovementWithStats> byMovementId =
            foundMovements.stream().collect(Collectors.toMap(m -> m.getMovement().getId(), Function.identity()));

        RequestedMovementWithStats outNewStats = byMovementId.get(outNew.getId());
        assertThat(outNewStats).extracting(RequestedMovementWithStats::getGoodCount).isEqualTo(1);
        assertThat(outNewStats).extracting(RequestedMovementWithStats::getWarehouseFrom)
            .isEqualTo("Маршрут ФФ");
        assertThat(outNewStats).extracting(RequestedMovementWithStats::getWarehouseTo)
            .isEqualTo("Лаборатория Контента");

        RequestedMovementWithStats outInPrStats = byMovementId.get(outInPr.getId());
        assertThat(outInPrStats).extracting(RequestedMovementWithStats::getGoodCount).isEqualTo(0);
        assertThat(outInPrStats).extracting(RequestedMovementWithStats::getWarehouseFrom)
            .isEqualTo("Лаборатория Контента");
        assertThat(outInPrStats).extracting(RequestedMovementWithStats::getWarehouseTo)
            .isEqualTo("Маршрут ФФ");
    }

    @Test
    public void findWithPaging() {
        RequestedMovement incNew = saveMovement(MovementDirection.INCOMING, RequestedMovementState.NEW);
        RequestedMovement incPlanned = saveMovement(MovementDirection.INCOMING, RequestedMovementState.PLANNED);
        RequestedMovement outProcessed = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.PROCESSED);
        RequestedMovement outNew = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.NEW);
        RequestedMovement outReq = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.REQUESTED);
        RequestedMovement outInPr = saveMovement(MovementDirection.OUTGOING, RequestedMovementState.IN_PROCESS);

        saveGoodMovement(incNew);
        saveGoodMovement(incNew);
        saveGoodMovement(incNew);
        saveGoodMovement(outInPr);
        saveGoodMovement(outInPr);
        saveGoodMovement(outProcessed);
        saveGoodMovement(outProcessed);
        saveGoodMovement(outProcessed);
        saveGoodMovement(outNew);

        final int pageSize = 3;
        RequestedMovementFilter filter = new RequestedMovementFilter().setDirection(MovementDirection.OUTGOING);
        PageFilter pageFilter = PageFilter.page(0, pageSize);
        List<RequestedMovementWithStats> movements;
        movements = movementRepository.findStats(filter, RequestedMovementSortBy.ID.asc(), pageFilter);

        assertThat(movements).extracting(RequestedMovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(RequestedMovement::getId))
            .hasSize(pageSize);

        movements = movementRepository.findStats(filter, RequestedMovementSortBy.GOOD_COUNT.desc(),
            pageFilter);

        assertThat(movements)
            .isSortedAccordingTo(Comparator.comparing(RequestedMovementWithStats::getGoodCount).reversed())
            .hasSize(pageSize);
    }

    public void saveGoodMovement(RequestedMovement movement) {
        RequestedGood good = RandomTestUtils.randomObject(RequestedGood.class, "id", "modifiedDate");
        good = goodRepository.save(good);
        RequestedGoodMovement goodMovement = new RequestedGoodMovement()
            .setRequestedGoodId(good.getId())
            .setRequestedMovementId(movement.getId())
            .setState(RandomTestUtils.randomObject(RequestedGoodMovementState.class));
        goodRepository.createGoodMovements(Collections.singletonList(goodMovement));
    }

    public RequestedMovement saveMovement(MovementDirection direction, RequestedMovementState state) {
        return saveMovement(direction, state, 145L, 163L);
    }

    public RequestedMovement saveMovement(MovementDirection direction, RequestedMovementState state,
                                          Long warehouseFromId, Long warehouseToId) {
        RequestedMovement saved = movementRepository.save(
            new RequestedMovement()
                .setDirection(direction)
                .setState(state)
                .setWarehouseFromId(warehouseFromId)
                .setWarehouseToId(warehouseToId)
        );
        savedMovements.add(saved);
        return saved;
    }
}
