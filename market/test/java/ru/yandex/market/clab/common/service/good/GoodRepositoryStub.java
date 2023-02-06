package ru.yandex.market.clab.common.service.good;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.math.NumberUtils;
import ru.yandex.market.clab.common.db.good.GoodUtils;
import ru.yandex.market.clab.common.service.ObservableRepositoryImpl;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.ShopSkuKey;
import ru.yandex.market.clab.common.service.SortOrder;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.common.service.user.User;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.GoodStateHistory;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.GoodType;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 12.10.2018
 */
public class GoodRepositoryStub extends ObservableRepositoryImpl<Good> implements GoodRepository {

    private static final long CLOTHES_ID = 1L;
    private static final long TOYS_ID = 2L;
    private final AtomicLong idGenerator = new AtomicLong();
    private final List<Good> goods = new ArrayList<>();
    private final Map<Long, GoodStateHistory> states = new HashMap<>();

    @Nullable
    @Override
    public Good getById(long goodId) {
        return find(new GoodFilter().addId(goodId)).stream().findFirst().orElse(null);
    }

    @Override
    public Good save(Good good, ActionSource actionSource) {
        Good updated = new Good(good);
        Good before = null;
        if (GoodUtils.isNew(good)) {
            updated.setId(idGenerator.getAndIncrement());
        } else {
            before = goods.stream()
                .filter(g -> good.getId().equals(g.getId()))
                .map(Good::new)
                .findFirst().orElse(null);
            goods.removeIf(g -> good.getId().equals(g.getId()));
        }
        goods.add(updated);
        notifyObservers(before, updated);
        return updated;
    }

    @Override
    public List<Good> getByIds(Collection<Long> goodIds) {
        if (goodIds.isEmpty()) {
            return Collections.emptyList();
        }
        return find(new GoodFilter().addIds(goodIds));
    }

    @Override
    public List<Good> save(Collection<Good> goods, ActionSource actionSource) {
        return goods.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public List<Good> find(GoodFilter filter, Sorting<GoodSortBy> sort, PageFilter page) {
        Stream<Good> stream = goods.stream()
            .filter(filter::test);

        if (sort != null) {
            stream = stream.sorted(sortBy(sort));
        }

        if (page != null) {
            stream = stream.skip(page.getOffset()).limit(page.getLimit());
        }

        return stream.collect(Collectors.toList());
    }

    private static Comparator<? super Good> sortBy(Sorting<GoodSortBy> sort) {
        switch (sort.getField()) {
            case LAST_CHANGE_DATE:
                return comparingField(sort, Comparator.comparing(Good::getLastChangeDate));
            case ID:
                return comparingField(sort, Comparator.comparing(Good::getId));
            default:
                throw new IllegalArgumentException();
        }
    }

    private static Comparator<? super Good> comparingField(Sorting<GoodSortBy> sort, Comparator<Good> comparing) {
        Comparator<Good> comparator = comparing;
        if (sort.getOrder() == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    @Override
    public int count(GoodFilter filter) {
        return find(filter).size();
    }

    @Override
    public List<GoodType> getTypes() {
        return Arrays.asList(
            new GoodType().setId(CLOTHES_ID).setDisplayName("Одежда"),
            new GoodType().setId(TOYS_ID).setDisplayName("Игрушки")
        );
    }

    @Override
    public List<GoodStateHistory> findStateHistory(GoodFilter goodFilter) {
        return find(goodFilter).stream()
            .map(Good::getId)
            .map(states::get)
            .collect(Collectors.toList());
    }

    @Override
    public List<ShopSkuKey> findShopSkuKeys(PageFilter page) {
        return goods.stream()
            .map(ShopSkuKey::ofGood)
            .sorted()
            .distinct()
            .skip(page.getOffset())
            .limit(page.getLimit())
            .collect(Collectors.toList());
    }

    @Override
    public List<Good> findNoData(GoodFilter filter, Sorting<GoodSortBy> sort, PageFilter page, User user) {
        return find(filter, sort, page);
    }

    @Override
    public List<Good> findBySearchKeyNoData(String goodSearchKey) {
        Set<Long> candidatesIds = goods.stream()
            .filter(g -> goodSearchKey.equals(g.getWhBarcode()))
            .map(Good::getId)
            .collect(Collectors.toSet());
        if (NumberUtils.isParsable(goodSearchKey)) {
            Long mskuId = Long.parseLong(goodSearchKey);
            candidatesIds.addAll(goods.stream()
                .filter(g -> mskuId.equals(g.getMskuId()))
                .map(Good::getId)
                .collect(Collectors.toSet()));
        }
        if (candidatesIds.isEmpty()) {
            return Collections.emptyList();
        }

        return find(new GoodFilter().setIds(candidatesIds));
    }
}
