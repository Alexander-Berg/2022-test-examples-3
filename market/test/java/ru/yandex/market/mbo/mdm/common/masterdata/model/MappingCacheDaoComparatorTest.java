package ru.yandex.market.mbo.mdm.common.masterdata.model;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

public class MappingCacheDaoComparatorTest {

    @Test
    public void mappingCachesShouldCompareByVersionTimestamp() {
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setVersionTimestamp(Instant.now().minusSeconds(100))
            .setModifiedTimestamp(LocalDateTime.now());

        MappingCacheDao mapping2 = new MappingCacheDao()
            .setVersionTimestamp(Instant.now())
            .setModifiedTimestamp(LocalDateTime.now());

        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping1, mapping2) < 0);
        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping2, mapping1) > 0);
    }

    @Test
    public void mappingCachesShouldCompareByVersionTimestampEvenIfUpdateTimestampIsNotNull() {
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setVersionTimestamp(Instant.now().minusSeconds(100))
            .setUpdateStamp(5432L)
            .setModifiedTimestamp(LocalDateTime.now());

        MappingCacheDao mapping2 = new MappingCacheDao()
            .setVersionTimestamp(Instant.now())
            .setUpdateStamp(123L)
            .setModifiedTimestamp(LocalDateTime.now());

        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping1, mapping2) < 0);
        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping2, mapping1) > 0);
    }

    @Test
    public void mappingCachesWithEmptyVersionTimestampShouldCompareWithUpdateStamp() {
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setUpdateStamp(123L)
            .setModifiedTimestamp(LocalDateTime.now());

        MappingCacheDao mapping2 = new MappingCacheDao()
            .setUpdateStamp(432L)
            .setModifiedTimestamp(LocalDateTime.now());

        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping1, mapping2) < 0);
        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping2, mapping1) > 0);
    }

    @Test
    public void mappingWithEmptyVersionTimestampLessEvenIfModifiedTimestampIsGreater() {
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setUpdateStamp(432L)
            .setModifiedTimestamp(LocalDateTime.now().minusDays(1));

        MappingCacheDao mapping2 = new MappingCacheDao()
            .setVersionTimestamp(Instant.now().minusSeconds(100))
            .setModifiedTimestamp(LocalDateTime.now());

        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping1, mapping2) < 0);
        Assert.assertTrue(MappingCacheDao.VERSION_TIMESTAMP_COMPARATOR.compare(mapping2, mapping1) > 0);
    }
}
