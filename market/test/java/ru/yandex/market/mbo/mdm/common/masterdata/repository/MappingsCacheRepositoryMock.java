package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MappingsCacheRepositoryMock
    extends GenericMapperRepositoryMock<MappingCacheDao, ShopSkuKey>
    implements MappingsCacheRepository {

    public MappingsCacheRepositoryMock() {
        super(null, MappingCacheDao::getShopSkuKey);
    }

    @Override
    protected ShopSkuKey nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MappingCacheDao> findByIds(Collection<ShopSkuKey> ids, ExpectedMappingQuality quality) {
        return null;
    }

    @Override
    public List<MappingCacheDao> findByIdsForUpdate(Collection<ShopSkuKey> ids) {
        return findByIds(ids);
    }

    @Override
    public List<MappingCacheDao> findByMskuIds(Collection<Long> mskuIds, ExpectedMappingQuality quality) {
        return findAll().stream()
            .filter(m -> mskuIds.contains(m.getMskuId()))
            .filter(satisfiesQuality(quality))
            .collect(Collectors.toList());
    }

    @Override
    public List<MappingCacheDao> findByCategoryId(long categoryId, ExpectedMappingQuality quality) {
        return findAll().stream()
            .filter(m -> m.getCategoryId() == categoryId)
            .filter(satisfiesQuality(quality))
            .collect(Collectors.toList());
    }

    @Override
    public List<MappingCacheDao> findByCategoryIds(Collection<Long> categoryIds, ExpectedMappingQuality quality) {
        return findAll().stream()
            .filter(m -> categoryIds.stream().anyMatch(categoryId -> categoryId.longValue() == m.getCategoryId()))
            .filter(satisfiesQuality(quality))
            .collect(Collectors.toList());
    }

    @Override
    public List<MappingCacheDao> findBySupplierId(int supplierId, ExpectedMappingQuality quality) {
        return findAll().stream()
            .filter(m -> m.getSupplierId() == supplierId)
            .filter(satisfiesQuality(quality))
            .collect(Collectors.toList());
    }

    @Override
    public List<MappingCacheDao> findBySupplierIds(Collection<Integer> supplierIds, ExpectedMappingQuality quality) {
        return findAll().stream()
            .filter(m -> supplierIds.contains(m.getSupplierId()))
            .filter(satisfiesQuality(quality))
            .collect(Collectors.toList());
    }

    @Override
    public long countUniqueMskusByCategoryIds(Collection<Long> categoryIds) {
        return 0;
    }

    @Override
    public long countUniqueSskusByCategoryIds(Collection<Long> categoryIds) {
        return 0;
    }

    @Override
    public Map<Long, Long> findCategoriesForMskuByLatestTs(List<MappingCacheDao> mappings) {
        return Map.of();
    }

    @Override
    public List<ShopSkuKey> getSampleSskuKeys(double percentage, int limit, long seed) {
        return findAll().stream()
            .map(MappingCacheDao::getShopSkuKey)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> getSampleMskuIds(double percentage, int limit, long seed) {
        return findAll().stream()
            .map(MappingCacheDao::getMskuId)
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }

    private Predicate<MappingCacheDao> satisfiesQuality(ExpectedMappingQuality quality) {
        return mapping -> {
            if (quality == ExpectedMappingQuality.ANY) {
                return true;
            }
            if (quality == ExpectedMappingQuality.SUGGESTED_ONLY &&
                mapping.getMappingKind() == MappingCacheDao.MappingKind.SUGGESTED) {
                return true;
            }
            if (quality == ExpectedMappingQuality.APPROVED_ONLY &&
                mapping.getMappingKind() == MappingCacheDao.MappingKind.APPROVED) {
                return true;
            }
            return false;
        };
    }

    @Override
    public List<MappingCacheDao> insertBatch(MappingCacheDao... instances) {
        return insertOrUpdateAll(List.of(instances));
    }
}
