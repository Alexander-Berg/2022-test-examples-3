package ru.yandex.direct.grid.processing.service.cache;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.grid.processing.service.cache.storage.GridCacheStorageHelper;
import ru.yandex.direct.grid.processing.service.cache.storage.GuavaGridCacheStorage;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class GridCacheServiceTest {
    private GridCacheService cacheService;
    private TestFilterData filter1 = new TestFilterData(ImmutableSet.of(1, 2, 3));

    @Before
    public void setUp() throws Exception {
        cacheService = new GridCacheService(
                new GridCacheStorageHelper(
                        new GuavaGridCacheStorage()
                )
        );
    }

    @Test
    public void successfullScenario() {
        TestRecordInfo info = new TestRecordInfo(12, "xxx", filter1);
        TestContext testContext = cacheService.getResultAndSaveToCache(
                info,
                new TestContext(),
                asList("1", "2", "3", "4"),
                new LimitOffset(2, 0)
        );
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(testContext.getCacheKey()).isNotEmpty();
        softly.assertThat(testContext.getRowset()).isEqualTo(asList("1", "2"));

        // Данные из кеша возвращаются корректно
        TestRecordInfo secondReq = new TestRecordInfo(12, testContext.cacheKey, filter1);
        Optional<TestContext> fromCache = cacheService.getFromCache(secondReq, new LimitOffset(3, 1));
        softly.assertThat(fromCache)
                .isNotEmpty();
        softly.assertThat(fromCache.get().getRowset())
                .isEqualTo(asList("2", "3", "4"));

        // для неправильного клиента данные не возвращаются
        TestRecordInfo incorrectClientReq = new TestRecordInfo(13, testContext.cacheKey, filter1);
        softly.assertThat(cacheService.getFromCache(incorrectClientReq, new LimitOffset(3, 1)))
                .isEmpty();

        softly.assertAll();
    }

    @Test
    public void noDataForUnknownKey() {
        TestRecordInfo info = new TestRecordInfo(12, "xxx", filter1);
        assertThat(cacheService.getFromCache(info, new LimitOffset(10, 1)))
                .isEmpty();
    }

    public static class TestContext implements CachedGridData<String> {
        List<String> rowset;
        String cacheKey;

        @Override
        public void setRowset(List<String> rowset) {
            this.rowset = rowset;
        }

        @Override
        public List<String> getRowset() {
            return rowset;
        }

        @Override
        public void setCacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        @Override
        public String getCacheKey() {
            return cacheKey;
        }
    }

    public static class TestFilterData implements CacheFilterData {
        private Set<Integer> ids;

        public TestFilterData() {
        }

        public TestFilterData(Set<Integer> ids) {
            this.ids = ids;
        }

        public Set<Integer> getIds() {
            return ids;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestFilterData that = (TestFilterData) o;

            return ids != null ? ids.equals(that.ids) : that.ids == null;
        }

        @Override
        public int hashCode() {
            return ids != null ? ids.hashCode() : 0;
        }
    }

    public static class TestRecordInfo extends CacheRecordInfo<String, TestFilterData, TestContext> {
        public TestRecordInfo() {
            // for jackson
            super();
        }

        public TestRecordInfo(
                long clientId, String key,
                TestFilterData filter) {
            super(clientId, key, filter);
        }

        public TestRecordInfo(
                long clientId, String key,
                TestFilterData filter,
                int totalSize, int chunkSize,
                TestContext data) {
            super(clientId, key, filter, totalSize, chunkSize, data);
        }

    }
}
