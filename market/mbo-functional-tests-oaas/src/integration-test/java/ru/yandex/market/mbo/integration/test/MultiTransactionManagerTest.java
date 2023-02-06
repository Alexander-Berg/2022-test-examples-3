package ru.yandex.market.mbo.integration.test;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import ru.yandex.market.mbo.db.MboDbSelectorFactory;
import ru.yandex.market.mbo.integration.test.config.db.MultiTransactionManagerConfig;

import javax.annotation.Resource;

/**
 * Tests to check {@link MultiTransactionManagerConfig}.
 * Inserts data to several databases and checks that everything is rolled back;
 *
 * @author s-ermakov
 */
public class MultiTransactionManagerTest extends BaseIntegrationTest {

    @Resource(name = "siteCatalogDbSelectorFactory")
    protected MboDbSelectorFactory mboDbSelectorFactory;

    protected JdbcOperations siteCatalogJdbcTemplate;

    @Resource(name = "contentJdbcTemplate")
    protected JdbcOperations contentJdbcTemplate;

    @Resource(name = "contentPgJdbcTemplate")
    protected JdbcOperations contentPgJdbcTemplate;

    @Resource(name = "contentDraftPgJdbcTemplate")
    protected JdbcOperations contentDraftPgJdbcTemplate;

    @SuppressWarnings("ConstantConditions")
    @Before
    public void setUp() {
        siteCatalogJdbcTemplate =
            mboDbSelectorFactory.constructProxyingJdbcTemplate("multiTransactionManagerTest");

        initPostgres();

        int siteCatalogSize =
            siteCatalogJdbcTemplate.queryForObject("select count(*) from SC_MODEL", Integer.class);
        int contentSize =
            contentJdbcTemplate.queryForObject("select count(*) from dependency_rule_erv", Integer.class);
        int contentDraftSize =
            contentDraftPgJdbcTemplate.queryForObject("select count(*) from NAVIGATION_TREE", Integer.class);

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(contentSize)
                .describedAs("Some data didn't rollback from previous test")
                .isZero();
            assertions.assertThat(contentDraftSize)
                .describedAs("Some data didn't rollback from previous test")
                .isZero();
            assertions.assertThat(siteCatalogSize)
                .describedAs("Some data didn't rollback from previous test")
                .isZero();
        });
    }

    private void initPostgres() {
        contentPgJdbcTemplate.update("CREATE SCHEMA IF NOT EXISTS market_content");

        contentPgJdbcTemplate.update("CREATE TABLE IF NOT EXISTS navigation_tree " +
                "    (id bigint PRIMARY KEY," +
                "     name text not null, " +
                "     root_node_id bigint, " +
                "     sync_tree_id bigint," +
                "     sync_type_id bigint, " +
                "     code text, " +
                "     illustration_depth bigint, " +
                "     tt_sync_status boolean, " +
                "     main_nids_mandatory boolean)");

        contentDraftPgJdbcTemplate.update("CREATE SCHEMA IF NOT EXISTS market_content_draft");
        contentDraftPgJdbcTemplate.update("CREATE TABLE IF NOT EXISTS navigation_tree " +
                "    (id bigint PRIMARY KEY," +
                "     name text not null, " +
                "     root_node_id bigint, " +
                "     sync_tree_id bigint," +
                "     sync_type_id bigint, " +
                "     code text, " +
                "     illustration_depth bigint, " +
                "     tt_sync_status boolean, " +
                "     main_nids_mandatory boolean)");
    }

    // Equal tests, just to make sure nothing is saved after one test finished and other started

    @Test
    public void testInsertData1() {
        int contentUpdated = contentJdbcTemplate.update(
            "insert into dependency_rule_erv (id, value_id) values (?, ?)", 1, 1);
        int contentDraftUpdated = contentPgJdbcTemplate.update(
            "insert into NAVIGATION_TREE (id, name) values (?, ?)", 1, "test");
        int siteCatalogUpdated = siteCatalogJdbcTemplate.update(
            "insert into SC_MODEL (id, name) values (?, ?)", 1, "test");

        Assertions.assertThat(contentUpdated).isEqualTo(1);
        Assertions.assertThat(contentDraftUpdated).isEqualTo(1);
        Assertions.assertThat(siteCatalogUpdated).isEqualTo(1);
    }

    @Test
    public void testInsertData2() {
        int contentUpdated = contentJdbcTemplate.update(
            "insert into dependency_rule_erv (id, value_id) values (?, ?)", 1, 1);
        int contentDraftUpdated = contentDraftPgJdbcTemplate.update(
            "insert into NAVIGATION_TREE (id, name) values (?, ?)", 1, "test");
        int siteCatalogUpdated = siteCatalogJdbcTemplate.update(
            "insert into SC_MODEL (id, name) values (?, ?)", 1, "test");

        Assertions.assertThat(contentUpdated).isEqualTo(1);
        Assertions.assertThat(contentDraftUpdated).isEqualTo(1);
        Assertions.assertThat(siteCatalogUpdated).isEqualTo(1);
    }
}
