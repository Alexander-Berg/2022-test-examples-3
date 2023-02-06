package ru.yandex.market.mboc.common.dict;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.favoritesupplier.FavoriteSupplier;
import ru.yandex.market.mboc.common.utils.SortBy;

/**
 * @author s-ermakov
 */
public class SupplierRepositoryMock
    extends EmptyGenericMapperRepositoryMock<Supplier, Integer> implements SupplierRepository {
    public SupplierRepositoryMock() {
        super(Supplier::getId);
    }

    @Override
    public List<Supplier> find(SupplierFilter filter, OffsetFilter offsetFilter, SortBy sortBy) {
        if (offsetFilter != OffsetFilter.all()) {
            throw new IllegalStateException("Not implemented");
        }

        Predicate<Supplier> predicate = supplier -> true;

        if (filter.getBlueSuppliers() != null) {
            if (filter.getBlueSuppliers()) {
                predicate = predicate.and(supplier -> supplier.getType() != MbocSupplierType.MARKET_SHOP);
            } else {
                predicate = predicate.and(supplier -> supplier.getType() == MbocSupplierType.MARKET_SHOP);
            }
        }

        if (filter.getWithMappings() != null) {
            throw new IllegalStateException("Not supported param in mock");
        }

        if (filter.getTypes() != null && !filter.getTypes().contains(ByType.ALL)) {
            Set<MbocSupplierType> types = filter.getTypes().stream().map(type -> {
                    switch (type) {
                        case REAL:
                            return MbocSupplierType.REAL_SUPPLIER;
                        case THIRD_PARTY:
                            return MbocSupplierType.THIRD_PARTY;
                        case FMCG:
                            return MbocSupplierType.FMCG;
                        default:
                            throw new IllegalStateException("Not supported type: " + type);
                    }
                })
                .collect(Collectors.toSet());
            predicate = predicate.and(supplier -> types.contains(supplier.getType()));
        }
        if (filter.getTestSupplier() != null) {
            predicate = predicate.and(supplier -> supplier.isTestSupplier() == filter.getTestSupplier());
        }
        if (filter.getHideFromToloka() != null) {
            predicate = predicate.and(supplier -> supplier.isHideFromToloka() == filter.getHideFromToloka());
        }
        if (filter.getIsBusinessIdNull() != null) {
            if (filter.getIsBusinessIdNull()) {
                predicate = predicate.and(supplier -> supplier.getBusinessId() == null);
            } else {
                predicate = predicate.and(supplier -> supplier.getBusinessId() != null);
            }
        }
        if (filter.getMbiBusinessIds() != null) {
            predicate = predicate.and(supplier -> filter.getMbiBusinessIds().contains(supplier.getMbiBusinessId()));
        }
        return findWhere(predicate);
    }

    @Override
    public int count(SupplierFilter filter) {
        return find(filter, OffsetFilter.all(), null).size();
    }

    @Override
    public int countByCategoryIds(List<Long> categoryIds, boolean exclusion, ByType real,
                                  SupplierSettings settings, String keyword, Boolean showHidden,
                                  List<FavoriteSupplier> favoriteSuppliers, Boolean hideEmpty) {
        throw new IllegalStateException("Not expected");
    }

    @Override
    public Map<String, Supplier> findByRealSupplierIds(Set<String> realSupplierIds) {
        var list = findWhere(supplier -> supplier.getType() == MbocSupplierType.REAL_SUPPLIER
            && realSupplierIds.contains(supplier.getRealSupplierId()));
        return list.stream().collect(Collectors.toMap(Supplier::getRealSupplierId, v -> v));
    }

    @Override
    public List<Supplier> findByCategoryIds(List<Long> categoryIds, boolean exclusion, ByType real,
                                            SupplierSettings settings, OffsetFilter offsetFilter, SortBy sortBy,
                                            String keyword, Boolean showHidden,
                                            List<FavoriteSupplier> favoriteSuppliers, Boolean hideEmpty) {
        return null;
    }

    @Override
    public List<Integer> findDistinctBusinessIds(SupplierFilter filter, OffsetFilter offsetFilter, SortBy sortBy) {
        return null;
    }

    @Override
    public List<Supplier> findByBusinessId(Integer businessId) {
        if (businessId != null) {
            return findWhere(supplier -> supplier.getBusinessId() != null
                && supplier.getBusinessId().equals(businessId));
        } else {
            return findWhere(supplier -> supplier.getBusinessId() == null);
        }
    }

    @Override
    public List<Supplier> findByBusinessIds(Collection<Integer> businessIds) {
        return findWhere(supplier -> businessIds.contains(supplier.getBusinessId()));
    }

    @Override
    public Set<Integer> findIds(SupplierFilter filter) {
        return find(filter).stream()
            .map(Supplier::getId)
            .collect(Collectors.toSet());
    }

    @Override
    public List<Supplier> findSuppliersByString(String request) {
        return null;
    }
}
