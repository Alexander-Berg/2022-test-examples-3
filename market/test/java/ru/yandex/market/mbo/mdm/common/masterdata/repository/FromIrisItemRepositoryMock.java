package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.SourceItemKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class FromIrisItemRepositoryMock extends GenericMapperRepositoryMock<FromIrisItemWrapper, SourceItemKey>
    implements FromIrisItemRepository {

    public FromIrisItemRepositoryMock() {
        super(null, FromIrisItemWrapper::getSourceItemKey);
    }

    @Override
    protected SourceItemKey nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FromIrisItemWrapper> getUnprocessedItemsBatch(int batchSize) {
        return findAll().stream()
            .filter(i -> !i.isProcessed())
            .collect(Collectors.toList());
    }

    @Override
    public List<FromIrisItemWrapper> findByIdsForUpdate(Collection<SourceItemKey> ids) {
        return findByIds(ids);
    }

    @Override
    public List<FromIrisItemWrapper> findByShopSkuKeysWithSource(Collection<ShopSkuKey> keys,
                                                                 MdmIrisPayload.MasterDataSource source,
                                                                 boolean forUpdate,
                                                                 boolean includeInvalid) {
        return findByShopSkuKeysForUpdate(keys).stream()
            .filter(i -> i.getSourceType() == source)
            .filter(i -> includeInvalid || i.isValid())
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Long> getNextSupplier(Long supplierId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FromIrisItemWrapper> getBatchForCurrentSupplier(Long supplierId, String shopSku, int batchSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FromIrisItemWrapper> findByFilter(FromIrisItemFilter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FromIrisItemWrapper> findByShopSkuKeys(Collection<ShopSkuKey> shopSkuKeys,
                                                       boolean forUpdate,
                                                       boolean includeInvalid) {
        return findAll().stream()
            .filter(i -> shopSkuKeys.contains(i.getShopSkuKey()))
            .filter(i -> includeInvalid || i.isValid())
            .collect(Collectors.toList());
    }

    @Override
    public Iterator<List<FromIrisItemWrapper>> allItemsIterator(int batchSize, SourceItemKey offset) {
        throw new UnsupportedOperationException();
    }
}
