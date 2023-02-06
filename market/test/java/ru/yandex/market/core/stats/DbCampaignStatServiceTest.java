package ru.yandex.market.core.stats;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.stats.model.GroupType;
import ru.yandex.market.core.stats.model.StatFilter;
import ru.yandex.market.core.stats.model.TimeType;

class DbCampaignStatServiceTest extends FunctionalTest {
    private static final StatFilter FILTER = new StatFilter(1L, new Date(), new Date(), GroupType.BY_DAY,
            true, true, TimeType.CLICKTIME);
    DbCampaignStatService statService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        statService = new DbCampaignStatService();
        statService.setJdbcTemplate(jdbcTemplate);
    }

    @Test
    void getStatInfo() {
        statService.getStatInfo(FILTER);
    }

    @Test
    void getCachedStatInfo() {
        statService.getCachedStatInfo(FILTER);
    }

    @Test
    void getMobStatInfo() {
        statService.getMobStatInfo(FILTER);
    }

    @Test
    void getCachedMobStatInfo() {
        statService.getCachedMobStatInfo(FILTER);
    }

    @Test
    void getDetailStatInfo() {
        statService.getDetailStatInfo(FILTER);
    }

    @Test
    void getLastWeekClicksSummary() {
        statService.getLastWeekClicksSummary(FILTER.getCampaignId());
    }
}
