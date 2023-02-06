package ru.yandex.market.mboc.common.masterdata.repository;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MasterDataRepositoryMock
    extends EmptyGenericMapperRepositoryMock<MasterData, ShopSkuKey>
    implements MasterDataRepository {

    public static final Comparator<MasterData> ORDER_BY_KEY = (o1, o2) ->
        o1.getSupplierId() == o2.getSupplierId() ?
            o1.getShopSku().compareTo(o2.getShopSku()) :
            Integer.compare(o1.getSupplierId(), o2.getSupplierId());
    public static final Comparator<MasterData> ORDER_BY_MODIFIED_TIME_AND_KEY = (o1, o2) ->
        Objects.equals(o1.getModifiedTimestamp(), o2.getModifiedTimestamp())
            ? ORDER_BY_KEY.compare(o1, o2)
            : o1.getModifiedTimestamp().compareTo(o2.getModifiedTimestamp());

    public MasterDataRepositoryMock() {
        super(MasterData::getShopSkuKey);
    }

    @Override
    public List<MasterData> findBy(MasterDataFilter filter) {
        List<MasterData> masterData = super.findAll().stream()
            .filter(md -> filter.getCriteria().stream().allMatch(cr -> cr.matches(md)))
            .filter(md -> filter.getModifiedAfter() == null
                || md.getModifiedTimestamp().isAfter(filter.getModifiedAfter())
            )
            .collect(Collectors.toList());

        Set<ShopSkuKey> shopSkuKeys = new HashSet<>(filter.getShopSkuKeys());
        if (!shopSkuKeys.isEmpty()) {
            masterData.removeIf(m -> !shopSkuKeys.contains(m.getShopSkuKey()));
        }

        Integer filterOffset = filter.getOffset();
        Integer filterLimit = filter.getLimit();
        if (filterLimit != null || filterOffset != null) {
            if (List.of("supplier_id", "shop_sku").equals(filter.getOrderByFields())) {
                masterData.sort(ORDER_BY_KEY);
            }
            if (List.of("modified_timestamp", "supplier_id", "shop_sku").equals(filter.getOrderByFields())) {
                masterData.sort(ORDER_BY_MODIFIED_TIME_AND_KEY);
            }

        }
        int fromIndex = filterOffset == null ? 0 : filterOffset > masterData.size() ? masterData.size() : filterOffset;
        int limit = filterLimit == null ? masterData.size() : filterLimit;
        int toIndex = Math.min(fromIndex + limit, masterData.size());
        return masterData.subList(fromIndex, toIndex);
    }

    @Override
    public void processInBatchesStoppable(MasterDataFilter filter,
                                          int batchSize,
                                          Predicate<List<MasterData>> stoppableConsumer) {
        throw new NotImplementedException("Not implemented yet!");
    }

    @Override
    public long count(MasterDataFilter filter) {
        return 0;
    }

    @Override
    public int deleteBySupplierType(MdmSupplierType supplierType) {
        return 0;
    }
}
