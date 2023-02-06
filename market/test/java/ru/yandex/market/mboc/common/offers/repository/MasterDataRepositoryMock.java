package ru.yandex.market.mboc.common.offers.repository;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import ru.yandex.market.mboc.common.masterdata.model.MasterDataAsJsonDTO;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MasterDataRepositoryMock implements IMasterDataRepository {
    List<MasterDataAsJsonDTO> context = new Vector<>();

    @Override
    public MasterDataAsJsonDTO insert(MasterDataAsJsonDTO instance) {
        context.add(instance);
        return instance;
    }

    @Override
    public List<MasterDataAsJsonDTO> insertBatch(Collection<MasterDataAsJsonDTO> instances) {
        context.addAll(instances);
        return context;
    }

    @Override
    public List<MasterDataAsJsonDTO> updateBatch(Collection<MasterDataAsJsonDTO> instances, int batchSize) {
        var toUpdate =
            context.stream().filter(i -> instances.stream().anyMatch(j -> j.getShopSku().equals(i.getShopSku()) &&
                j.getSupplierId() == i.getSupplierId())).collect(Collectors.toList());
        context.removeAll(toUpdate);
        context.addAll(toUpdate);
        return context;
    }

    @Override
    public void delete(List<ShopSkuKey> ids) {
        var toUpdate =
            context.stream()
            .filter(i -> ids.contains(new ShopSkuKey(i.getSupplierId(), i.getShopSku())))
            .collect(Collectors.toList());
        context.removeAll(toUpdate);
    }

    @Override
    public void deleteBatch(Collection<MasterDataAsJsonDTO> masterDataAsJsonDTOS) {
        var toUpdate =
            context.stream().filter(i -> masterDataAsJsonDTOS.stream().anyMatch(j -> j.getShopSku().equals(i.getShopSku()) &&
                j.getSupplierId() == i.getSupplierId())).collect(Collectors.toList());
        context.removeAll(toUpdate);
    }

    @Override
    public void deleteAll() {
        context.clear();
    }

    @Override
    public List<MasterDataAsJsonDTO> insertOrUpdateAll(Collection<MasterDataAsJsonDTO> masterDataAsJsonDTOS) {
        var toUpdate =
            context.stream().filter(i -> masterDataAsJsonDTOS.stream().anyMatch(j -> j.getShopSku().equals(i.getShopSku()) &&
                j.getSupplierId() == i.getSupplierId())).collect(Collectors.toList());
        context.removeAll(toUpdate);
        context.addAll(masterDataAsJsonDTOS);
        return context;
    }

    @Override
    public Integer insertOrUpdateAllIfDifferent(Collection<MasterDataAsJsonDTO> masterDataAsJsonDTOS) {
        insertOrUpdateAll(masterDataAsJsonDTOS);
        return masterDataAsJsonDTOS.size();
    }

    @Override
    public MasterDataAsJsonDTO findById(ShopSkuKey id) {
        return context.stream().filter(i -> new ShopSkuKey(i.getSupplierId(), i.getShopSku()).equals(id)).findFirst().orElse(null);
    }

    @Override
    public MasterDataAsJsonDTO findByIdForUpdate(ShopSkuKey id) {
        return findById(id);
    }

    @Override
    public List<MasterDataAsJsonDTO> findByIds(Collection<ShopSkuKey> ids) {
        return context.stream()
            .filter(ctx -> ids.stream()
                .anyMatch(key -> key.getShopSku().equals(ctx.getShopSku()) && key.getSupplierId() == ctx.getSupplierId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<MasterDataAsJsonDTO> findByIdsForUpdate(Collection<ShopSkuKey> ids) {
        return findByIds(ids);
    }

    @Override
    public List<MasterDataAsJsonDTO> findAll() {
        return context;
    }

    @Override
    public int totalCount() {
        return context.size();
    }
}
