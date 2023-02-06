package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;

/**
 * @author dmserebr
 * @date 18/12/2020
 */
public class MappingsCacheRepositoryAuditTest extends MdmServicesAuditTestBase {
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    @Override
    protected String getEntityType() {
        return "mappings_cache";
    }

    @Override
    protected Set<String> getFieldsIgnoredInComparator() {
        return Set.of("update_stamp", "modified_timestamp");
    }

    @Override
    protected AuditRecordTestInfo insertTestValue() {
        var eoxTs = Instant.ofEpochMilli(12000L);
        var mbocTs = Instant.ofEpochMilli(21000L);
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            new MappingCacheDao()
                .setSupplierId(123)
                .setShopSku("shop-sku")
                .setMskuId(12345L)
                .setCategoryId(12)
                .setEoxTimestamp(eoxTs)
                .setMbocTimestamp(mbocTs)
                .setMappingSource(MappingCacheDao.MappingSource.DATACAMP)
                .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED)
        ));

        return new AuditRecordTestInfo("123 shop-sku", List.of(
            Pair.of("supplier_id", "123"),
            Pair.of("shop_sku", "shop-sku"),
            Pair.of("market_sku_id", "12345"),
            Pair.of("market_category_id", "12"),
            Pair.of("version_timestamp", "null"),
            Pair.of("mapping_source", "DATACAMP"),
            Pair.of("eox_timestamp", "1970-01-01T03:00:12"),
            Pair.of("mboc_timestamp", "1970-01-01T03:00:21"),
            Pair.of("mapping_kind", "SUGGESTED")
        ));
    }

    @Override
    protected AuditRecordTestInfo updateTestValue() {
        var eoxTs = Instant.ofEpochMilli(21000L);
        var mbocTs = Instant.ofEpochMilli(12000L);
        mappingsCacheRepository.insertOrUpdateAll(List.of(new MappingCacheDao()
            .setSupplierId(123)
            .setShopSku("shop-sku")
            .setMskuId(123456L)
            .setCategoryId(123)
            .setEoxTimestamp(eoxTs)
            .setMbocTimestamp(mbocTs)
            .setMappingSource(MappingCacheDao.MappingSource.MBOC_API)
            .setMappingKind(MappingCacheDao.MappingKind.APPROVED)
        ));

        return new AuditRecordTestInfo("123 shop-sku", List.of(
            Pair.of("market_sku_id", "[12345, 123456]"),
            Pair.of("market_category_id", "[12, 123]"),
            Pair.of("mapping_source", "[DATACAMP, MBOC_API]"),
            Pair.of("eox_timestamp", "[1970-01-01T03:00:12, 1970-01-01T03:00:21]"),
            Pair.of("mboc_timestamp", "[1970-01-01T03:00:21, 1970-01-01T03:00:12]"),
            Pair.of("mapping_kind", "[SUGGESTED, APPROVED]")
        ));
    }
}
