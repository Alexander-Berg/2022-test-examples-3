package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class ReferenceItemRepositoryMock extends GenericMapperRepositoryMock<ReferenceItemWrapper, ShopSkuKey>
    implements ReferenceItemRepository {

    public ReferenceItemRepositoryMock() {
        super(null, ReferenceItemWrapper::getShopSkuKey);
    }

    @Override
    public void batchReadBySupplierIds(int batchSize, Collection<Long> supplierIds,
                                       Consumer<List<ReferenceItemWrapper>> action) {
        Lists.partition(findAll(), batchSize).forEach(items -> {
            List<ReferenceItemWrapper> itemsFilteredBySupplierIds = items.stream()
                .filter(item -> supplierIds.contains(item.getSupplierId()))
                .collect(Collectors.toList());
            action.accept(itemsFilteredBySupplierIds);
        });
    }

    @Override
    protected ShopSkuKey nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processWithLock(Collection<ShopSkuKey> keys,
                                Function<List<ReferenceItemWrapper>, List<ReferenceItemWrapper>> action) {
        Set<ShopSkuKey> keyset = new HashSet<>(keys);
        action.apply(findAll().stream()
            .filter(i -> keyset.contains(i.getShopSkuKey()))
            .collect(Collectors.toList())
        );
    }

    @Override
    public List<ReferenceItemWrapper> findByIdsForUpdate(Collection<ShopSkuKey> ids) {
        return findByIds(ids);
    }
}
