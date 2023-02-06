package ru.yandex.market.mbo.integration.test;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.mbo.db.MboDbSelector;

import javax.annotation.Resource;

/**
 * @author s-ermakov
 */
public class BasicOracleTest extends BaseIntegrationTest {

    @Resource(name = "siteCatalogJdbcTemplate")
    protected JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "contentJdbcTemplate")
    protected JdbcTemplate contentJdbcTemplate;

    @Resource(name = "funcTestsDbSelector")
    protected MboDbSelector funcTestsDbSelector;

    @Resource(name = "contentDraftJdbcTemplate")
    protected JdbcTemplate contentDraftJdbcTemplate;

    @Resource(name = "tmsJdbcTemplate")
    protected JdbcTemplate mboTmsJdbcTemplate;

    @Resource(name = "marketDepotJdbcTemplate")
    protected JdbcTemplate marketDepotJdbcTemplate;

    @Test
    public void testSiteCatalogSelect1FromDual() {
        int result = siteCatalogJdbcTemplate.queryForObject("select 1 from dual", Integer.class);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testMarketContentSelect1FromDual() {
        int result = contentJdbcTemplate.queryForObject("select 1 from dual", Integer.class);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testMarketContentDraftSelect1FromDual() {
        int result = contentDraftJdbcTemplate.queryForObject("select 1 from dual", Integer.class);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testMarketDepotSelect1FromDual() {
        int result = marketDepotJdbcTemplate.queryForObject("select 1 from dual", Integer.class);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testNoAccessToMarketContentTemplate() {
        Assertions.assertThatThrownBy(() ->
            funcTestsDbSelector.getJdbcTemplate().
                queryForObject("select count(*) from market_content.mc_category", Integer.class)
        ).hasMessageContaining("insufficient privileges");
    }
}
