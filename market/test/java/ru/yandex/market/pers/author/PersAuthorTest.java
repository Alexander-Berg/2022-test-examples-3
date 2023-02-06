package ru.yandex.market.pers.author;

import javax.sql.DataSource;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.author.config.CoreConfig;
import ru.yandex.market.pers.author.config.InternalConfig;
import ru.yandex.market.pers.author.config.JdbcConfig;
import ru.yandex.market.pers.service.common.util.ConfigurationCache;
import ru.yandex.market.pers.service.common.util.ExpFlagService;
import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

/**
 * @author varvara
 * 28.01.2020
 */
@Import({
    CoreMockConfiguration.class,
    JdbcConfig.class,
    CoreConfig.class,
    InternalConfig.class
})
@ComponentScan(
    basePackageClasses = {PersAuthor.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:/test-application.properties")
public class PersAuthorTest extends AbstractPersWebTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("pgDataSource")
    protected DataSource pgDataSource;

    @Autowired
    @Qualifier("memCacheMock")
    private Cache<String, Object> cache;

    @Autowired
    @Qualifier("localCache")
    private Cache<String, Object> localCache;

    @Autowired
    private ConfigurationCache configurationCache;

    @Autowired
    protected ExpFlagService expFlagService;

    @BeforeEach
    public void setUp() {
        applySqlScript(pgDataSource,"truncate.sql");
        invalidateCache();
        resetMocks();
        expFlagService.reset();
    }

    public void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }

    protected void invalidateCache() {
        cache.invalidateAll();
        localCache.invalidateAll();
        configurationCache.invalidateCache();
    }
}
