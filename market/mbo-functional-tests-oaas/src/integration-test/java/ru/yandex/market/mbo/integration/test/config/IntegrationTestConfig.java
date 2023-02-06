package ru.yandex.market.mbo.integration.test.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.yandex.market.mbo.configs.AutoUserConfig;
import ru.yandex.market.mbo.configs.audit.AuditTestConfig;
import ru.yandex.market.mbo.configs.category_wiki.CategoryWikiConfiguration;
import ru.yandex.market.mbo.configs.db.filter_config.FilterConfigConfiguration;
import ru.yandex.market.mbo.configs.db.parameter.ParameterDAOConfig;
import ru.yandex.market.mbo.configs.db.parameter.ParameterLoaderServiceConfig;
import ru.yandex.market.mbo.configs.db.recipe.RecipeDaoConfiguration;
import ru.yandex.market.mbo.core.conf.databases.IdGeneratorConfig;
import ru.yandex.market.mbo.core.conf.databases.MboOracleDBConfig;
import ru.yandex.market.mbo.core.conf.databases.MboTmsDBConfig;
import ru.yandex.market.mbo.integration.test.billing.BillingIntegrationTestsConfig;
import ru.yandex.market.mbo.integration.test.config.db.MultiTransactionManagerConfig;
import ru.yandex.market.mbo.integration.test.config.db.SpringLiquibaseConfig;
import ru.yandex.market.mbo.tms.config.DumpHistoryDaoConfig;

/**
 * @author s-ermakov
 */
@Configuration
@Lazy
@Import({
    MasterDatabaseConfig.class,
    MboOracleDBConfig.class,
    IdGeneratorConfig.class,
    MboTmsDBConfig.class,
    MultiTransactionManagerConfig.class,
    SpringLiquibaseConfig.class,
    BillingIntegrationTestsConfig.class,
    ParameterLoaderServiceConfig.class,
    ParameterDAOConfig.class,
    DumpHistoryDaoConfig.class,
    CategoryWikiConfiguration.class,
    AutoUserConfig.class,
    RecipeDaoConfiguration.class,
    FilterConfigConfiguration.class,
    AuditTestConfig.class
})
@EnableTransactionManagement
@SuppressWarnings("checkstyle:hideUtilityClassConstructor")
public class IntegrationTestConfig {
    private static final String SITE_CATALOG_INIT_SQL =
        "create schema if not exists site_catalog; set search_path to site_catalog, public";
    private static final String MARKET_CONTENT_INIT_SQL =
        "create schema if not exists market_content; set search_path to market_content, public";
    private static final List<String> SITE_CATALOG_INIT_SQLS = new ArrayList<>();
    private static final List<String> MARKET_CONTENT_INIT_SQLS = new ArrayList<>();
    static {
        //TODO костыль. В будущем надо решить корректной накаткой ликви в pg embedded
        SITE_CATALOG_INIT_SQLS.add("create table if not exists site_catalog.ng_billing_session_marks " +
                                       "(" +
                                       "marking        varchar(127)               not null, " +
                                       "mark_timestamp timestamp with time zone   not null, " +
                                       "success        smallint, " +
                                       "job_hostname   varchar(253), " +
                                       "constraint session_marks_pk " +
                                       "primary key (marking, mark_timestamp) " +
                                       ")");
        MARKET_CONTENT_INIT_SQLS.add("create table if not exists market_content.CATEGORY_WIKI\n" +
                                         "(\n" +
                                         "    CATEGORY_ID                      bigint not null\n" +
                                         "    constraint PK_CATEGORY_WIKI_ID\n" +
                                         "    primary key,\n" +
                                         "    EXPORT_STRUCT_MBO                boolean default true,\n" +
                                         "    EXPORT_STRUCT_PARTNERS           boolean default true,\n" +
                                         "    EXPORT_TITLE_MBO                 boolean default true,\n" +
                                         "    EXPORT_TITLE_PARTNERS            boolean default true,\n" +
                                         "    EXPORT_DEF_PARAM_MBO             boolean default true,\n" +
                                         "    EXPORT_DEF_PARAM_PARTN           boolean default true,\n" +
                                         "    EXPORT_WOC_SKU_MBO               boolean default true,\n" +
                                         "    EXPORT_WOC_SKU_PARTN             boolean default true,\n" +
                                         "    EXPORT_UNIQ_INF_MBO              boolean default true,\n" +
                                         "    EXPORT_UNIQ_INF_PARTN            boolean default true,\n" +
                                         "    EXPORT_LINK_MBO                  boolean default true,\n" +
                                         "    EXPORT_LINK_PARTN                boolean default false,\n" +
                                         "    EXPORT_INT_COMMENT_TO_MBO        boolean default false,\n" +
                                         "    EXPORT_INT_COMMENT_TO_PARTN      boolean default false,\n" +
                                         "    INTERNAL_COMMENT                 text,\n" +
                                         "    MODEL_NAME_COMMENT               text,\n" +
                                         "    INCLUDED_HINT                    text,\n" +
                                         "    EXCLUDED_HINT                    text,\n" +
                                         "    IN_CATEGORY                      text,\n" +
                                         "    OUT_OF_CATEGORY                  text,\n" +
                                         "    DEFINING_PARAMS_COMMENT          text,\n" +
                                         "    WAY_OF_CREATING_SKU              text,\n" +
                                         "    UNIQUE_INFORMATION               text,\n" +
                                         "    TICKETS_LINK                     text,\n" +
                                         "    TLK_REQUIRED_PARAM_IN_CARD       text,\n" +
                                         "    TLK_REQUIRED_PARAM_TO_COMPARE    text,\n" +
                                         "    TLK_DECISION_BY_PHOTO            text,\n" +
                                         "    TLK_ADDITIONAL_COMMENTS          text,\n" +
                                         "    EXPORT_TLK_INSTRUCTIONS_TO_MBO   boolean default true,\n" +
                                         "    EXPORT_TLK_INSTRUCTIONS_TO_PARTN boolean default false\n" +
                                         ")");
        MARKET_CONTENT_INIT_SQLS.add("create table if not exists market_content.CATEGORY_WIKI_PICTURE_ROW\n" +
                                         "(\n" +
                                         "    CATEGORY_ID     bigint not null,\n" +
                                         "    POSITION        bigint not null,\n" +
                                         "    IMAGE_URL       VARCHAR(4000),\n" +
                                         "    COMMENTS        VARCHAR(4000),\n" +
                                         "    INCLUDES_VISUAL boolean,\n" +
                                         "    constraint CAT_WIKI_PICTURE_ROW_PK\n" +
                                         "    primary key (CATEGORY_ID, POSITION)\n" +
                                         "    )");
        MARKET_CONTENT_INIT_SQLS.add("create table if not exists MARKET_CONTENT.GOODS_RETURN_POLICY\n" +
                                         "(\n" +
                                         "  ID          int constraint UNIQUE_GOODS_RETURN_POLICY_ID unique,\n" +
                                         "  REPORT_TEXT VARCHAR(50),\n" +
                                         "  GUI_TEXT    VARCHAR(200),\n" +
                                         "  GUI_INDEX   int\n" +
                                         ")");
        MARKET_CONTENT_INIT_SQLS.add("insert into MARKET_CONTENT.GOODS_RETURN_POLICY values\n" +
                                         "  (0, '7d', 'возврат до 7 дней', 0) on conflict do nothing");
        MARKET_CONTENT_INIT_SQLS.add("insert into MARKET_CONTENT.GOODS_RETURN_POLICY values\n" +
                                         "  (1, '14d', 'возврат до 14 дней', 1) on conflict do nothing");
        MARKET_CONTENT_INIT_SQLS.add("insert into MARKET_CONTENT.GOODS_RETURN_POLICY values\n" +
                                         "  (2, '28d', 'возврат до 28 дней', 2) on conflict do nothing");
        MARKET_CONTENT_INIT_SQLS.add("insert into MARKET_CONTENT.GOODS_RETURN_POLICY values\n" +
                                         "  (3, null, 'нет возврата', 3) on conflict do nothing");
        MARKET_CONTENT_INIT_SQLS.add("insert into MARKET_CONTENT.GOODS_RETURN_POLICY values\n" +
                                         "  (4, 'with_problems',\n" +
                                         "      'с возвратом товара могут возникнуть проблемы на стороне магазина', " +
                                         "4) on conflict do nothing");
        MARKET_CONTENT_INIT_SQLS.add("create table if not exists market_content.CATEGORY_GOODS_RETURN_POLICY\n" +
                                         "(\n" +
                                         "    CATEGORY_ID bigint not null,\n" +
                                         "    REGION_ID   int not null,\n" +
                                         "    POLICY_ID   int not null\n" +
                                         "    references market_content.GOODS_RETURN_POLICY (ID),\n" +
                                         "    constraint UNIQUE_CATEGORY_POLICY\n" +
                                         "    unique (CATEGORY_ID, REGION_ID)\n" +
                                         "    )");
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocations(
            new ClassPathResource("/db-test.properties"),
            new ClassPathResource("/mbo-functional-tests-oaas/master-db.properties"),
            new ClassPathResource("/mbo-functional-tests-oaas/integration-test.properties"),
            new ClassPathResource("/mbo-functional-tests-oaas/yt-test.properties")
        );
        return configurer;
    }

    @Bean
    public JdbcTemplate siteCatalogPgJdbcTemplate(DataSource siteCatalogPgDataSource) {
        DataSource wrappedSiteCatalogPgDataSource = new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return prepareConnection(siteCatalogPgDataSource.getConnection());
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return prepareConnection(siteCatalogPgDataSource.getConnection(username, password));
            }

            private Connection prepareConnection(Connection connection) throws SQLException {
                try (PreparedStatement initStatement = connection.prepareStatement(SITE_CATALOG_INIT_SQL)) {
                    initStatement.execute();
                }
                return connection;
            }
        };

        JdbcTemplate jdbcTemplate = new JdbcTemplate(wrappedSiteCatalogPgDataSource);
        for (String s : SITE_CATALOG_INIT_SQLS) {
            jdbcTemplate.execute(s);
        }
        return jdbcTemplate;
    }

    @Bean
    public JdbcTemplate contentPgJdbcTemplate(DataSource contentPgDataSource) {
        DataSource wrappedContentPgDataSource = new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return prepareConnection(contentPgDataSource.getConnection());
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return prepareConnection(contentPgDataSource.getConnection(username, password));
            }

            private Connection prepareConnection(Connection connection) throws SQLException {
                try (PreparedStatement mcInitStatement = connection.prepareStatement(MARKET_CONTENT_INIT_SQL)) {
                    mcInitStatement.execute();
                }
                return connection;
            }
        };

        JdbcTemplate jdbcTemplate = new JdbcTemplate(wrappedContentPgDataSource);
        for (String s : MARKET_CONTENT_INIT_SQLS) {
            jdbcTemplate.execute(s);
        }
        return jdbcTemplate;
    }
}
