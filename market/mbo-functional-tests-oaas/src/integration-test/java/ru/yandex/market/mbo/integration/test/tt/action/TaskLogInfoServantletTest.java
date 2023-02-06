package ru.yandex.market.mbo.integration.test.tt.action;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Resource;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.framework.core.AbstractServRequest;
import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.catalogue.MarketDepotService;
import ru.yandex.market.mbo.catalogue.action.GetMarketEntityServantlet;
import ru.yandex.market.mbo.core.report.filter.ColumnType;
import ru.yandex.market.mbo.core.report.filter.EnumMultiValueSqlFilterDescriptor;
import ru.yandex.market.mbo.core.report.filter.EnumSqlFilterDescriptor;
import ru.yandex.market.mbo.core.report.filter.SelectorOperator;
import ru.yandex.market.mbo.core.report.filter.StringSqlFilterDescriptor;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.integration.test.tt.TtTaskInitializer;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.action.TaskLogInfoServantlet;
import ru.yandex.market.mbo.tt.action.filter.LogIdStaticFilter;
import ru.yandex.market.mbo.tt.model.TaskType;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class TaskLogInfoServantletTest extends BaseIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(TaskLogInfoServantletTest.class);

    private static final long VENDOR_ID = 9825494L;
    private static final long PARENT_VENDOR_ID = 220565L;
    private static final long LANG_ID = 225L;
    private static final long MODEL_ID = 21830039L;
    private static final long MODIFICATION_ID = 46421493L;
    private static final long TYPE = 7L;
    private static final long PREVIOUS_TYPE = 5L;
    private static final int PAGER_SIZE = 50;
    private static final long GURU_CATEGORY_ID = 123L;
    private static final long CONTENT_ID = 456L;
    private static final long USER_ID = 10000L;

    @Autowired
    private TaskTracker taskTracker;

    @Resource(name = "siteCatalogJdbcTemplate")
    private JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "siteCatalogPgJdbcTemplate")
    private JdbcOperations siteCatalogPgJdbcTemplate;

    private TaskLogInfoServantlet servantlet;
    private Long taskListId;
    ServRequest<Object> req;
    private MockServResponse res;

    @Before
    public void setUp() {
        LogIdStaticFilter filter = new LogIdStaticFilter("task_id = ?", taskTracker);

        MarketDepotService marketDepotService = mock(MarketDepotService.class);
        when(marketDepotService.getAttribute(anyLong(), any(), anyLong())).thenReturn(Collections.singletonList("5"));

        servantlet = new TaskLogInfoServantlet(
            siteCatalogJdbcTemplate,
            siteCatalogPgJdbcTemplate,
            null,
            null,
            "V_TT_LOG_INFO",
            marketDepotService,
            taskTracker,
            null,
            mock(GetMarketEntityServantlet.class),
            siteCatalogJdbcTemplate,
            siteCatalogJdbcTemplate
        );

        servantlet.setPagerSize(PAGER_SIZE);
        servantlet.setOrderSuffix("type, vendor_id ASC, long_cluster_id DESC, model_id ASC");
        servantlet.setSortTail("long_cluster_id DESC");
        servantlet.setCheckBoxIdColumn("offer_id");
        servantlet.setStaticFilters(Collections.singletonList(filter));
        servantlet.setFilters(Arrays.asList(
            new StringSqlFilterDescriptor("offer", ColumnType.TEXT, SelectorOperator.ORACLE_LIKE),
            new StringSqlFilterDescriptor("description", ColumnType.TEXT, SelectorOperator.ORACLE_LIKE),
            new StringSqlFilterDescriptor("model_name", ColumnType.TEXT, SelectorOperator.ORACLE_LIKE),
            new EnumSqlFilterDescriptor("vendor_id", "vendor_name", SelectorOperator.EQUALS),
            new EnumMultiValueSqlFilterDescriptor("datasource", "datasource"),
            new EnumSqlFilterDescriptor("type", "type_name", SelectorOperator.EQUALS)
        ));

        TtTaskInitializer.create(siteCatalogPgJdbcTemplate);

        Pair<Long, List<Long>> taskListWithTasks = taskTracker.createTaskListWithTasks(
            TaskType.FILL_MODEL, GURU_CATEGORY_ID, Collections.singletonList(CONTENT_ID));

        taskListId = taskListWithTasks.getFirst();
        log.info("taskListId = {}", taskListId);

        siteCatalogJdbcTemplate.update(
            "insert into tt_log (" +
                "TASK_ID, " +
                "OFFER_ID, " +
                "FEED_ID, " +
                "OFFER, " +
                "DESCR, " +
                "OFFER_HASH, " +
                "SHOP_ID, " +
                "HIGHLIGHTED_DESCR, " +
                "HIGHLIGHTED_OFFER, " +
                "GOOD_ID, " +
                "OFFER_PARAMS, " +
                "HIGHLIGHTED_OFFER_PARAMS, " +
                "DATE_OF_SYNCHRONIZATION, " +
                "LAST_SESSION, " +
                "VENDOR_CODE, " +
                "BARCODE, " +
                "HIGHLIGHTED_BARCODE, " +
                "DEEP_MATCH_TRASH_SCORE, " +
                "MATCHED_TARGET, " +
                "VENDOR_ID, " +
                "MODEL_ID, " +
                "MODIFICATION_ID, " +
                "TYPE, " +
                "PREVIOUS_TYPE" +
                ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            taskTracker.getTasksIds(taskListId).get(0),
            0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0,
            VENDOR_ID, MODEL_ID, MODIFICATION_ID, TYPE, PREVIOUS_TYPE);

        // TODO упразднить это, просто накатывать на заэмбеженный pg ликвибейз
        prepareSchema();

        siteCatalogPgJdbcTemplate.update(
            "INSERT INTO market_content.enum_option (" +
                "ID, NAME, PRIORITY, TAG, COLOR_CODE, PARENT_VENDOR_ID, " +
                "NUMERIC_VALUE, PARAM_ID, OPERATOR_COMMENTS, MBO_DUMP_ID, " +
                "PICKER_IMAGE_URL, PICKER_IMAGE_NAME" +
                ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            VENDOR_ID, 0, 0, 0, 0, PARENT_VENDOR_ID, 0, 0, 0, 0, 0, 0
        );

        siteCatalogPgJdbcTemplate.update(
            "INSERT INTO market_content.global_vendor_name (" +
                "VENDOR_ID, NAME, LANG_ID" +
                ") values (?, ?, ?)",
            PARENT_VENDOR_ID, "vendor " + PARENT_VENDOR_ID, LANG_ID
        );

        siteCatalogPgJdbcTemplate.update(
            "insert into sc_model (" +
                "ID, NAME, VENDOR_ID, GURU_CATEGORY_ID, URL, STATUS, WITHOUT_MODIFICATIONS, PARENT_ID" +
                ") values (?, ?, ?, ?, ?, ?, ?, ?)",
            MODEL_ID, "model " + MODEL_ID, 0, 0, 0, 0, 0, 0
        );

        siteCatalogPgJdbcTemplate.update(
            "insert into sc_model (" +
                "ID, NAME, VENDOR_ID, GURU_CATEGORY_ID, URL, STATUS, WITHOUT_MODIFICATIONS, PARENT_ID" +
                ") values (?, ?, ?, ?, ?, ?, ?, ?)",
            MODIFICATION_ID, "model " + MODIFICATION_ID, 0, 0, 0, 0, 0, 0
        );

        req = new AbstractServRequest<>(USER_ID, null, null) { };
        res = new MockServResponse();
    }

    private void prepareSchema() {
        siteCatalogPgJdbcTemplate.update("CREATE SCHEMA IF NOT EXISTS market_content");

        siteCatalogPgJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.enum_option (" +
            "ID bigint NOT NULL primary key," +
            "NAME VARCHAR(1000)," +
            "PRIORITY bigint," +
            "TAG VARCHAR(100)," +
            "COLOR_CODE VARCHAR(100)," +
            "PARENT_VENDOR_ID bigint," +
            "NUMERIC_VALUE decimal," +
            "DO_NOT_USE_AS_ALIAS CHAR default 0," +
            "ACTIVE CHAR default 1," +
            "PUBLISHED CHAR default 0," +
            "PARAM_ID bigint NOT NULL," +
            "OPERATOR_COMMENTS VARCHAR(4000)," +
            "MBO_DUMP_ID bigint," +
            "MODIFIED_TS TIMESTAMP(6) with time zone default current_timestamp," +
            "DEFAULT_FOR_MATCHER bigint default 0," +
            "IS_TOP_VALUE bigint default 0," +
            "IS_FILTER_VALUE bigint default 1," +
            "IS_DEFAULT_VALUE bigint default 0," +
            "PICKER_IMAGE_URL VARCHAR(4000)," +
            "PICKER_IMAGE_NAME VARCHAR(500)," +
            "INHERITANCE_STRATEGY VARCHAR(90) NOT NULL default 'INHERIT'," +
            "IS_FILTER_VALUE_RED bigint default 1," +
            "IS_DEFAULT_VALUE_RED bigint default 0," +
            "DISPLAY_NAME VARCHAR(1000)," +
            "PARENT_OPTION_ID bigint," +
            "CATEGORY_ID bigint" +
            ")");

        siteCatalogPgJdbcTemplate.update("CREATE TABLE IF NOT EXISTS market_content.global_vendor_name (" +
            "VENDOR_ID bigint NOT NULL," +
            "NAME VARCHAR(4000) NOT NULL," +
            "LANG_ID bigint NOT NULL," +
            "primary key (VENDOR_ID, NAME, LANG_ID)" +
            ")");

        siteCatalogPgJdbcTemplate.update("create table if not exists sc_model (" +
            "id                    bigint not null constraint sc_model_pk primary key," +
            "name                  varchar(2000)," +
            "vendor_id             bigint," +
            "guru_category_id      bigint," +
            "url                   varchar(4000)," +
            "status                bigint," +
            "publish_level         bigint                   default 2," +
            "created               timestamp with time zone default CURRENT_TIMESTAMP not null," +
            "without_modifications bigint," +
            "parent_id             bigint" +
            ")");
    }

    @After
    public void tearDown() {
        siteCatalogPgJdbcTemplate.update("delete from market_content.enum_option where id = ?", VENDOR_ID);
        siteCatalogPgJdbcTemplate.update("delete from market_content.global_vendor_name where vendor_id = ?",
            PARENT_VENDOR_ID);
        siteCatalogPgJdbcTemplate.update("delete from sc_model where id in (?, ?)", MODEL_ID, MODIFICATION_ID);
    }

    @Test
    public void testNoFilter() {
        req.setParam("task-list-id", taskListId.toString());

        servantlet.process(req, res);
        assertThat(res.getErrors(), Matchers.hasSize(0));
    }

    @Test
    public void testModelFilterHasMatch() {
        req.setParam("task-list-id", taskListId.toString());
        req.setParam("model-name-value", Long.toString(MODEL_ID));

        servantlet.process(req, res);
        assertThat(res.getErrors(), Matchers.hasSize(0));
    }

    @Test
    public void testModelFilterNoMatch() {
        req.setParam("task-list-id", taskListId.toString());
        req.setParam("model-name-value", "no such model");

        servantlet.process(req, res);
        assertThat(res.getErrors(), Matchers.hasSize(0));
    }
}
