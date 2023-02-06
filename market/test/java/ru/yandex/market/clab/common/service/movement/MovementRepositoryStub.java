package ru.yandex.market.clab.common.service.movement;

import ru.yandex.market.clab.common.db.good.MovementUtils;
import ru.yandex.market.clab.common.service.ObservableRepositoryImpl;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.SortOrder;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.warehouse.WarehouseRepository;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.MovementStateHistory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 12.10.2018
 */
public class MovementRepositoryStub extends ObservableRepositoryImpl<Movement> implements MovementRepository {
    private final GoodRepository goodRepository;
    private final WarehouseRepository warehouseRepository;
    private final AtomicLong idGenerator = new AtomicLong();
    private final List<Movement> movements = new ArrayList<>();
    private final Map<Long, List<MovementStateHistory>> states = new HashMap<>();

    public MovementRepositoryStub(GoodRepository goodRepository, WarehouseRepository warehouseRepository) {
        this.goodRepository = goodRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public Movement getById(long id) {
        return movements.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Movement> getByIds(Collection<Long> ids) {
        return movements.stream()
            .filter(m -> ids.contains(m.getId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Movement> find(MovementFilter filter) {
        return movements.stream()
            .filter(filter::test)
            .collect(Collectors.toList());
    }

    @Override
    public Movement save(Movement movement) {
        Movement saved = new Movement(movement);
        if (MovementUtils.isNew(movement)) {
            saved.setId(idGenerator.getAndIncrement());
        } else {
            movements.removeIf(m -> m.getId().equals(movement.getId()));
        }
        movements.add(saved);
        return saved;
    }

    @Override
    public List<MovementWithStats> findWithStats(MovementFilter filter, Sorting<MovementSortBy> sort,
                                                 PageFilter pageFilter) {
        Stream<MovementWithStats> stream = movements.stream()
            .filter(filter::test)
            .map(movement -> new MovementWithStats(movement, null, 0, null, null))
            .peek(mws -> {
                GoodFilter goodFilter = new GoodFilter().setMovementId(mws.getMovement().getId());
                List<Good> goods = goodRepository.find(goodFilter);
                int size = goods.size();
                mws.setItemCount(size);
                goods.stream()
                    .max(Comparator.comparing(Good::getModifiedDate))
                    .ifPresent(good -> {
                        mws.setLastItemUpdateUid(good.getModifiedUserId());
                        mws.setLastItemUpdateDate(good.getModifiedDate());
                    });

                Optional.ofNullable(warehouseRepository.getById(mws.getMovement().getWarehouseId()))
                    .ifPresent(warehouse -> mws.setWarehouseName(warehouse.getName()));
            });

        if (sort != null) {
            stream = stream.sorted(sortBy(sort));
        }

        if (pageFilter != null) {
            stream = stream.skip(pageFilter.getOffset()).limit(pageFilter.getLimit());
        }

        return stream
            .collect(Collectors.toList());
    }

    @Override
    public long count(MovementFilter filter) {
        return find(filter).size();
    }

    @Override
    public List<MovementStateHistory> getStateHistory(long movementId) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private static Comparator<? super MovementWithStats> sortBy(Sorting<MovementSortBy> sort) {
        switch (sort.getField()) {
            case ID:
                Comparator<MovementWithStats> comparator = Comparator.comparing(m -> m.getMovement().getId());
                if (sort.getOrder() == SortOrder.DESC) {
                    comparator = comparator.reversed();
                }
                return comparator;
            default:
                throw new IllegalArgumentException();
        }
    }
}
