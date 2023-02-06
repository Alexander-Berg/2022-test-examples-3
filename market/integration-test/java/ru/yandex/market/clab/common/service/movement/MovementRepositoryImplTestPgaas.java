package ru.yandex.market.clab.common.service.movement;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.good.ActionSource;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 16.10.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MovementRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    private static final long WAREHOUSE_ID_145 = 145;
    private static final long WAREHOUSE_ID_171 = 171;
    private static final long WAREHOUSE_ID_172 = 172;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private GoodRepository goodRepository;

    private Set<Movement> savedMovements;

    private static final long USER_ID = 5667428349L;

    @Before
    public void before() {
        savedMovements = new HashSet<>();
    }

    @Test
    public void saveMovements() {
        Movement movement = new Movement();
        movement.setSupplierType(SupplierType.FIRST_PARTY);
        movement.setDirection(MovementDirection.OUTGOING);
        Movement saved = movementRepository.save(movement);

        assertThat(saved.getId()).isNotNull().isGreaterThan(-1);
        assertThat(saved.getModifiedDate()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(saved.getDirection()).isEqualTo(MovementDirection.OUTGOING);

        Movement fetched = movementRepository.getById(saved.getId());

        assertThat(fetched).isEqualToComparingFieldByField(saved);
    }

    @Test
    public void findMovements() {
        Movement incNewFp = saveMovement(
            MovementDirection.INCOMING, MovementState.NEW, SupplierType.FIRST_PARTY, null);
        Movement incDoneTp = saveMovement(
            MovementDirection.INCOMING, MovementState.DONE, SupplierType.THIRD_PARTY, null);
        Movement incAccFp = saveMovement(
            MovementDirection.INCOMING, MovementState.ACCEPTED, SupplierType.FIRST_PARTY, null);
        Movement incNewTp = saveMovement(
            MovementDirection.INCOMING, MovementState.NEW, SupplierType.THIRD_PARTY, null);
        Movement outDoneFp = saveMovement(
            MovementDirection.OUTGOING, MovementState.DONE, SupplierType.FIRST_PARTY, null);
        Movement outProTp = saveMovement(
            MovementDirection.OUTGOING, MovementState.PROCESSED, SupplierType.THIRD_PARTY, null);
        List<Movement> all = Arrays.asList(incNewFp, incDoneTp, incAccFp, incNewTp, outDoneFp, outProTp);

        assertThat(movementRepository.find(new MovementFilter().addState(MovementState.DONE)))
            .extracting(Movement::getId)
            .contains(
                incDoneTp.getId(),
                outDoneFp.getId()
            ).doesNotContain(savedMovementsExcept(incDoneTp, outDoneFp));

        assertThat(movementRepository.find(new MovementFilter().addType(SupplierType.THIRD_PARTY)))
            .extracting(Movement::getId)
            .contains(
                incDoneTp.getId(),
                incNewTp.getId(),
                outProTp.getId()
            ).doesNotContain(savedMovementsExcept(incDoneTp, incNewTp, outProTp));

        assertThat(movementRepository.find(new MovementFilter().addDirection(MovementDirection.INCOMING)))
            .extracting(Movement::getId)
            .contains(
                incNewFp.getId(),
                incDoneTp.getId(),
                incAccFp.getId(),
                incNewTp.getId()
            ).doesNotContain(savedMovementsExcept(incNewFp, incDoneTp, incAccFp, incNewTp));

        assertThat(
            movementRepository.find(
                new MovementFilter()
                    .addDirection(MovementDirection.INCOMING)
                    .addState(MovementState.NEW)
                    .addType(SupplierType.THIRD_PARTY)))
            .extracting(Movement::getId)
            .contains(incNewTp.getId())
            .doesNotContain(savedMovementsExcept(incNewTp));
    }

    private Long[] savedMovementsExcept(Movement... except) {
        Set<Movement> exceptMovements = new HashSet<>(Arrays.asList(except));
        return savedMovements.stream()
            .filter(m -> !exceptMovements.contains(m))
            .map(Movement::getId)
            .toArray(Long[]::new);
    }

    @Test
    public void fetchWithStats() {
        Movement movement1 = saveMovement(
            MovementDirection.INCOMING, MovementState.NEW, SupplierType.FIRST_PARTY, null);
        Movement movement2 = saveMovement(
            MovementDirection.INCOMING, MovementState.DONE, SupplierType.THIRD_PARTY, null);
        Movement movement3 = saveMovement(
            MovementDirection.INCOMING, MovementState.ACCEPTED, SupplierType.FIRST_PARTY, null);
        Movement movement4 = saveMovement(
            MovementDirection.OUTGOING, MovementState.NEW, SupplierType.THIRD_PARTY, null);
        Movement movement5 = saveMovement(
            MovementDirection.OUTGOING, MovementState.DONE, SupplierType.FIRST_PARTY, null);
        Movement movement6 = saveMovement(
            MovementDirection.OUTGOING, MovementState.ACCEPTED, SupplierType.THIRD_PARTY, null);
        Movement movement7 = saveMovement(
            MovementDirection.OUTGOING, MovementState.ACCEPTED, SupplierType.THIRD_PARTY, null);

        LocalDateTime lastChangeDate = RandomTestUtils.randomObject(LocalDateTime.class);
        Good good14 = saveGood(movement1, movement4, RandomTestUtils.randomLong(), lastChangeDate.plusSeconds(1));
        Good good15 = saveGood(movement1, movement5, RandomTestUtils.randomLong(), lastChangeDate.plusSeconds(2));
        Good good16 = saveGood(movement1, movement6, RandomTestUtils.randomLong(), lastChangeDate.plusSeconds(3));
        Good good24 = saveGood(movement2, movement4, RandomTestUtils.randomLong(), lastChangeDate.plusSeconds(4));
        Good good26 = saveGood(movement2, movement6, RandomTestUtils.randomLong(), lastChangeDate.plusSeconds(5));
        Good good36 = saveGood(movement3, movement6, RandomTestUtils.randomLong(), lastChangeDate.plusSeconds(6));

        List<MovementWithStats> movementsWithId1 = movementRepository.findWithStats(
            new MovementFilter().addId(movement1.getId()));

        assertThat(movementsWithId1).extracting(MovementWithStats::getMovement)
            .extracting(Movement::getId)
            .containsExactly(movement1.getId());

        assertThat(movementsWithId1).extracting(MovementWithStats::getItemCount).containsExactly(3);
        assertThat(movementsWithId1).extracting(MovementWithStats::getLastItemUpdateDate).containsExactly(
            good16.getLastChangeDate());
        assertThat(movementsWithId1).extracting(MovementWithStats::getLastItemUpdateUid).containsExactly(
            good16.getModifiedUserId());

        List<MovementWithStats> movementsThirdParty = movementRepository.findWithStats(
            new MovementFilter()
                .addType(SupplierType.THIRD_PARTY));

        assertThat(movementsThirdParty)
            .extracting(MovementWithStats::getMovement)
            .extracting(Movement::getId)
            .contains(
                movement2.getId(),
                movement4.getId(),
                movement6.getId(),
                movement7.getId()
            ).doesNotContain(
                movement1.getId(),
                movement3.getId(),
                movement5.getId()
            );

        Map<Long, MovementWithStats> byMovementId =
            movementsThirdParty.stream().collect(Collectors.toMap(m -> m.getMovement().getId(), Function.identity()));

        MovementWithStats withMov2 = byMovementId.get(movement2.getId());
        assertThat(withMov2).extracting(MovementWithStats::getItemCount).isEqualTo(2);
        assertThat(withMov2).extracting(MovementWithStats::getLastItemUpdateDate).isEqualTo(good26.getLastChangeDate());
        assertThat(withMov2).extracting(MovementWithStats::getLastItemUpdateUid).isEqualTo(good26.getModifiedUserId());

        MovementWithStats withMov6 = byMovementId.get(movement6.getId());
        assertThat(withMov6).extracting(MovementWithStats::getItemCount).isEqualTo(3);
        assertThat(withMov6).extracting(MovementWithStats::getLastItemUpdateDate).isEqualTo(good36.getLastChangeDate());
        assertThat(withMov6).extracting(MovementWithStats::getLastItemUpdateUid).isEqualTo(good36.getModifiedUserId());

        MovementWithStats withMov7 = byMovementId.get(movement7.getId());
        assertThat(withMov7).extracting(MovementWithStats::getItemCount).isEqualTo(0);
        assertThat(withMov7).extracting(MovementWithStats::getLastItemUpdateDate).isNull();
        assertThat(withMov7).extracting(MovementWithStats::getLastItemUpdateUid).isNull();
    }

    @Test
    public void findWithPaging() {
        Movement new1 = saveMovement(
            MovementDirection.INCOMING, MovementState.NEW, SupplierType.FIRST_PARTY, WAREHOUSE_ID_171);
        Movement accepted1 = saveMovement(
            MovementDirection.INCOMING, MovementState.ACCEPTED, SupplierType.THIRD_PARTY, null);
        Movement accepted2 = saveMovement(
            MovementDirection.INCOMING, MovementState.ACCEPTED, SupplierType.FIRST_PARTY, WAREHOUSE_ID_171);
        Movement new2 = saveMovement(
            MovementDirection.OUTGOING, MovementState.NEW, SupplierType.THIRD_PARTY, null);
        Movement done = saveMovement(
            MovementDirection.OUTGOING, MovementState.DONE, SupplierType.FIRST_PARTY, WAREHOUSE_ID_172);
        Movement accepted3 = saveMovement(
            MovementDirection.OUTGOING, MovementState.ACCEPTED, SupplierType.THIRD_PARTY, null);
        Movement accepted4 = saveMovement(
            MovementDirection.OUTGOING, MovementState.ACCEPTED, SupplierType.THIRD_PARTY, WAREHOUSE_ID_172);

        LocalDateTime lastChangeDate = RandomTestUtils.randomObject(LocalDateTime.class);
        saveGood(accepted2, accepted4, USER_ID, lastChangeDate.plusSeconds(1));
        saveGood(accepted1, accepted3, USER_ID, lastChangeDate.plusSeconds(2));
        saveGood(accepted1, accepted3, USER_ID, lastChangeDate.plusSeconds(3));
        saveGood(accepted2, accepted4, USER_ID, lastChangeDate.plusSeconds(4));
        saveGood(accepted1, done, USER_ID, lastChangeDate.plusSeconds(5));
        saveGood(new1, accepted4, USER_ID, lastChangeDate.plusSeconds(6));
        saveGood(new2, accepted3, USER_ID, lastChangeDate.plusSeconds(7));

        final int pageSize = 3;
        MovementFilter filter = new MovementFilter().addState(MovementState.ACCEPTED);
        PageFilter pageFilter = PageFilter.page(0, pageSize);
        List<MovementWithStats> movements;
        movements = movementRepository.findWithStats(filter, MovementSortBy.ID.asc(), pageFilter);

        assertThat(movements).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getId))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.EXTERNAL_ID.asc(), pageFilter);

        assertThat(movements).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getExternalId))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.CREATION_DATE.asc(), pageFilter);

        assertThat(movements).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getCreatedDate))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.DIRECTION.asc(), pageFilter);

        assertThat(movements).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getDirection))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.STATUS.asc(), pageFilter);

        assertThat(movements).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getState))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.SUPPLIER_TYPE.asc(), pageFilter);

        assertThat(movements).extracting(MovementWithStats::getMovement)
            .isSortedAccordingTo(Comparator.comparing(Movement::getSupplierType))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.GOOD_COUNT.asc(), pageFilter);

        assertThat(movements)
            .isSortedAccordingTo(Comparator.comparing(MovementWithStats::getItemCount))
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.GOOD_LAST_CHANGE_DATE.desc(),
            pageFilter);

        assertThat(movements)
            .isSortedAccordingTo(Comparator.comparing(MovementWithStats::getLastItemUpdateDate).reversed())
            .hasSize(pageSize);

        movements = movementRepository.findWithStats(filter, MovementSortBy.WAREHOUSE_NAME.desc(),
            pageFilter);

        assertThat(movements)
            .isSortedAccordingTo(Comparator.comparing(MovementWithStats::getWarehouseName).reversed())
            .hasSize(pageSize);
    }

    public Good saveGood(Movement incoming, Movement outgoing, long userId, LocalDateTime lastChangeDate) {
        Good good =  RandomTestUtils.randomObject(Good.class, "id", "modifiedDate", "lastChangeDate")
            .setLastChangeDate(lastChangeDate)
            .setSupplierId(1L)
            .setSupplierSkuId("qwerty")
            .setIncomingMovementId(incoming.getId())
            .setOutgoingMovementId(outgoing.getId())
            .setModifiedUserId(userId);
        return goodRepository.save(good, ActionSource.USER);
    }

    public Movement saveMovement(MovementDirection direction, MovementState state, SupplierType type, Long whId) {
        Movement toSave = RandomTestUtils.randomObject(Movement.class, "id", "modifiedDate")
            .setDirection(direction)
            .setState(state)
            .setSupplierType(type)
            .setWarehouseId(whId != null ? whId : WAREHOUSE_ID_145);
        Movement saved = movementRepository.save(toSave);
        savedMovements.add(saved);
        return saved;
    }
}
