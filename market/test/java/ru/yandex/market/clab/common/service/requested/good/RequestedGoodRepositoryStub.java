package ru.yandex.market.clab.common.service.requested.good;

import ru.yandex.market.clab.common.db.good.RequestedGoodUtils;
import ru.yandex.market.clab.common.service.ObservableRepositoryImpl;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodMovement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodLogistics;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RequestedGoodRepositoryStub extends ObservableRepositoryImpl<RequestedGood>
    implements RequestedGoodRepository {

    private final AtomicLong idGenerator = new AtomicLong();
    private final List<RequestedGood> goods = new ArrayList<>();
    private final List<RequestedGoodMovement> goodMovements = new ArrayList<>();

    @Nullable
    @Override
    public RequestedGood getById(long goodId) {
        return findGoods(new RequestedGoodFilter().addId(goodId)).stream().findFirst().orElse(null);
    }

    @Override
    public List<RequestedGood> save(Collection<RequestedGood> toSave) {
        return toSave.stream()
            .map(good -> {
            RequestedGood updated = new RequestedGood(good);
            if (RequestedGoodUtils.isNew(good)) {
                updated.setId(idGenerator.getAndIncrement());
            } else {
                goods.removeIf(g -> good.getId().equals(g.getId()));
            }
            goods.add(updated);
            return updated;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RequestedGoodMovement> createGoodMovements(Collection<RequestedGoodMovement> goods) {
        goodMovements.addAll(goods);
        return new ArrayList<>(goods);
    }

    @Override
    public List<RequestedGoodMovement> updateGoodMovements(Collection<RequestedGoodMovement> goods) {
        goods.forEach(good -> goodMovements.removeIf(
            g -> g.getRequestedGoodId().equals(good.getRequestedGoodId()) &&
            g.getRequestedMovementId().equals(good.getRequestedMovementId())));
        return createGoodMovements(goods);
    }

    @Override
    public List<RequestedGoodMovement> getGoodMovements(long movementId, List<Long> goodIds) {
        return goodMovements.stream()
            .filter(gm -> gm.getRequestedMovementId().equals(movementId) && goodIds.contains(gm.getRequestedGoodId()))
            .collect(Collectors.toList());
    }

    @Override
    public void removeGoodMovements(long movementId, List<Long> goodIds) {
        goodMovements.removeIf(
            g -> g.getRequestedGoodId().equals(movementId) &&
                goodIds.contains(g.getRequestedMovementId()));
    }

    @Override
    public void removeGoodMovements(long movementId) {
        goodMovements.removeIf(
            g -> g.getRequestedGoodId().equals(movementId));
    }

    @Override
    public List<RequestedGood> getByIds(Collection<Long> goodIds) {
        return goods.stream()
            .filter(g -> goodIds.contains(g.getId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<RequestedGood> findGoods(RequestedGoodFilter filter) {
        return goods.stream()
            .map(this::toInfo)
            .filter(filter::test)
            .map(RequestedGoodInfo::getGood)
            .collect(Collectors.toList());
    }

    @Override
    public List<RequestedGoodInfo> findInfos(RequestedGoodFilter filter, Sorting<RequestedGoodSortBy> sort, PageFilter page) {
        return goods.stream()
            .map(this::toInfo)
            .filter(filter::test)
            .collect(Collectors.toList());
    }

    @Override
    public int count(RequestedGoodFilter filter) {
        return (int) goods.stream()
            .map(this::toInfo)
            .filter(filter::test)
            .count();
    }

    @Override
    public List<RequestedGoodLogistics> getLogistics(long warehouseId, List<Long> goodIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void upsertInboundAvailability(List<RequestedGoodLogistics> requestedGoodStocks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void upsertStocks(List<RequestedGoodLogistics> requestedGoodStocks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<RequestedGood> findForUpdateLogisticsAvailability(Long warehouseId, RequestedGoodState state,
                                                                 LocalDateTime lastUpdateTs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockRequestedGoodTable() {
        // Do nothing
    }

    private RequestedGoodInfo toInfo(RequestedGood good) {
        return new RequestedGoodInfo(good, null, "category");
    }
}
