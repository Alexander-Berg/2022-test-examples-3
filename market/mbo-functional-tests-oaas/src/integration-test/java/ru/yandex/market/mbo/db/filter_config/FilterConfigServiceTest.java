package ru.yandex.market.mbo.db.filter_config;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.db.navigation.FilterConfigService;
import ru.yandex.market.mbo.gwt.models.navigation.FilterConfig;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class FilterConfigServiceTest extends BaseIntegrationTest {

    private static final List<String> INIT_QUERIES = new ArrayList<>();
    static {
        INIT_QUERIES.add("CREATE SCHEMA IF NOT EXISTS market_content");
        INIT_QUERIES.add("create table if not exists MARKET_CONTENT.NAVIGATION_FILTER_CONFIG" +
                             " (" +
                             "   ID           bigint          not null," +
                             "   PARAM_ID     bigint          not null," +
                             "   ADVANCED     boolean default false not null," +
                             "   FILTER_ORDER int,\n" +
                             "   constraint UNIQUE_NAVIGATION_FILTER_CONFIG" +
                             "       unique (ID, PARAM_ID, ADVANCED)" +
                             ")");
    }

    @Resource
    private FilterConfigService filterConfigService;

    @Resource
    private NamedParameterJdbcTemplate contentPgNamedJdbcTemplate;

    @Before
    public void before() {
        INIT_QUERIES.forEach(s -> contentPgNamedJdbcTemplate.update(s, Collections.emptyMap()));
    }

    @Test
    public void saveEmptyFilterConfig() {
        FilterConfig filterConfig = new FilterConfig();
        FilterConfig savedFilterConfig = filterConfigService.saveFilterConfig(filterConfig);

        assertThat(savedFilterConfig.getId()).isNotPositive();

        FilterConfig loadedFilterConfig = loadFilterConfig(savedFilterConfig.getId());

        assertThat(loadedFilterConfig).isNull();
    }

    @Test
    public void saveNewFilterConfig() {
        FilterConfig filterConfig = new FilterConfig();
        filterConfig.addFilter(616L);

        FilterConfig savedFilterConfig = filterConfigService.saveFilterConfig(filterConfig);

        assertThat(savedFilterConfig.getId()).isPositive();

        FilterConfig loadedFilterConfig = loadFilterConfig(savedFilterConfig.getId());

        assertThat(loadedFilterConfig).isNotNull();
        assertThat(loadedFilterConfig.getId()).isPositive();
        assertThat(loadedFilterConfig.getFilters()).containsExactlyInAnyOrder(616L);
        assertThat(loadedFilterConfig.getAdvancedFilters()).isEmpty();
    }

    @Test
    public void saveFilterConfig() {
        FilterConfig filterConfig = new FilterConfig()
            .setId(18L)

            .addFilter(181L)
            .addFilter(184L)
            .addFilter(183L)
            .addFilter(182L)

            .addAdvancedFilter(281L)
            .addAdvancedFilter(282L);

        filterConfigService.saveFilterConfig(filterConfig);
        FilterConfig savedFilterConfig = loadFilterConfig(18L);
        assertTrue(savedFilterConfig.getId() > 0);

        List<Long> manualFilters = savedFilterConfig.getFilters();
        assertFalse(manualFilters.isEmpty());
        assertThat(manualFilters).containsExactly(181L, 184L, 183L, 182L);

        List<Long> hiddenFilters = savedFilterConfig.getAdvancedFilters();
        assertFalse(hiddenFilters.isEmpty());
        assertThat(hiddenFilters).containsExactlyInAnyOrder(281L, 282L);
    }

    private FilterConfig loadFilterConfig(Long id) {
        if (id == null) {
            return null;
        }
        return filterConfigService.getFilterConfig(id);
    }
}
