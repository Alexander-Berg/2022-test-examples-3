package ru.yandex.market.pers.qa;

import javax.sql.DataSource;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.pers.service.common.util.ExpFlagService;

/**
 * @author korolyov
 * 20.06.18
 */
@ActiveProfiles("test")
public abstract class PersQACoreTest {

    @Autowired
    @Qualifier("datasource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("memCacheMock")
    private Cache<String, Object> cache;

    @Autowired
    @Qualifier("localCache")
    private Cache<String, Object> localCache;

    @Autowired
    @Qualifier("localCacheOneHour")
    private Cache<String, Object> localCacheOneHour;

    @Autowired
    @Qualifier("localCacheGradeMapping")
    private Cache<Long, Long> gradeFixIdCache;

    @Autowired
    protected ExpFlagService expFlagService;

    @BeforeEach
    public void cleanDatabase() {
        DatabaseMockConfiguration.applySqlScript("truncate.sql", dataSource);;
        invalidateCache();
        expFlagService.reset();

        prepareExpFlags();
    }

    public void prepareExpFlags() {
        // fill when required
//        expFlagService.setFlag(SOME_FLAG, true);
    }

    protected void invalidateCache() {
        cache.invalidateAll();
        localCache.invalidateAll();
        localCacheOneHour.invalidateAll();
        gradeFixIdCache.invalidateAll();
    }

    protected abstract void resetMocks();

    @BeforeEach
    public final void resetAllMocks() {
        PersQaServiceMockFactory.resetMocks();
        resetMocks();
    }
}
