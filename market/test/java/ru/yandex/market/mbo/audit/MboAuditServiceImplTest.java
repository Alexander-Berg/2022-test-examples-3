package ru.yandex.market.mbo.audit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.mbo.audit.clickhouse.AuditTemplatesProvider;
import ru.yandex.market.mbo.audit.yt.YtActionLogRepository;
import ru.yandex.market.mbo.core.activitylock.LockHelper;
import ru.yandex.market.mbo.db.CachedKeyValueMapService;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAudit.FindActionsRequest;
import ru.yandex.market.mbo.http.MboAudit.MboAction;
import ru.yandex.market.mbo.http.MboAudit.WriteActionsRequest;

import static ru.yandex.market.mbo.audit.MboAuditServiceImpl.AUDIT_DEFAULT_TYPES_KEY;
import static ru.yandex.market.mbo.http.MboAudit.EntityType.CATEGORY;
import static ru.yandex.market.mbo.http.MboAudit.EntityType.CM_BLUE_OFFER;

public class MboAuditServiceImplTest {

    private static final String TABLE_NAME = "test_action";

    private MboAuditServiceImpl auditService;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private EnhancedRandom enhancedRandom;

    private YtActionLogRepository ytActionLogRepository;

    private LockHelper lockHelper = Mockito.mock(LockHelper.class);

    private static Logger logger = LoggerFactory.getLogger(MboAuditServiceImplTest.class);

    @Before
    public void setUp() throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + getClass().getSimpleName() + UUID.randomUUID().toString());

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate = Mockito.spy(namedTemplate);
        Mockito.doAnswer(invok -> {
            String query = invok.getArgument(0);
            Map<String, Object> args = invok.getArgument(1);
            RowMapper<Object> rowMapper = invok.getArgument(2);
            query = query.replaceAll("count\\(\\)", "count(*)");
            query = query.replaceAll(" prewhere 1=1", "");
            query = query.replaceAll("limit :offset, :length", "LIMIT :length OFFSET :offset");
            return namedTemplate.query(query, args, rowMapper);
        }).when(jdbcTemplate).query(Mockito.anyString(), Mockito.anyMap(), Mockito.<RowMapper>any());

        jdbcTemplate.getJdbcOperations().update("create table " + TABLE_NAME + " ( " +
            " date Date, " +
            " timestamp DateTime, " +
            " user_id int, " +
            " staff_login text, " +
            " action_type text, " +
            " entity_type text, " +
            " entity_id int, " +
            " entity_name text, " +
            " property_name text, " +
            " old_value text, " +
            " new_value text, " +
            " event_id int, " +
            " category_id int, " +
            " parameter_id int, " +
            " environment text, " +
            " billing_mode text, " +
            " action_id int," +
            " source text," +
            " source_id text," +
            " system_property int," +
            " check (action_type in ('CREATE', 'UPDATE', 'DELETE', 'CHECK'))," +
            " check (billing_mode in ('BILLING_MODE_NONE', 'BILLING_MODE_FILL', 'BILLING_MODE_COPY'," +
            " 'BILLING_MODE_CHECK', 'BILLING_MODE_FILL_CUSTOM', 'BILLING_MODE_MOVE'))," +
            " check (entity_type in ('CATEGORY', 'PARAMETER', 'OPTION', 'NAVIGATION_TREE', 'NAVIGATION_NODE'," +
            " 'MODEL_PARAM', 'RECIPE', 'MODEL_COMPATIBILITY', 'DEPENDENCY_RULE', 'MODEL_GURU'," +
            " 'MODEL_SKU', 'MODEL_PICTURE', 'MODEL_ENUM_VALUE_ALIAS', 'MODEL_PICKER', 'PICTURE_PARAMETER_VALUES'," +
            " 'SKU_PARAM', 'CM_BLUE_OFFER', 'APPROVED_SKU_MAPPING', 'MODEL_PARTNER', 'MODEL_PARTNER_SKU', " +
            " 'PARTNER_PARAM', 'PARTNER_SKU_PARAM', 'PARAM_HYPOTHESIS', 'CATEGORY_PARAM', 'MSKU_STATUS'," +
            " 'ANTI_MAPPING', 'OFFER_ANTI_MAPPING', 'SKU_ANTI_MAPPING'))," +
            " check (source in ('MBO', 'YANG_TASK', 'CONTENT_LAB'))" +
            ")"
        );

        jdbcTemplate.getJdbcOperations().update("create table KEY_VALUE_MAP ( " +
            "key text, " +
            "value text)"
        );

        jdbcTemplate.getJdbcOperations().update(
            "insert into KEY_VALUE_MAP values (?, ?)",
            ps -> {
                ps.setString(1, AUDIT_DEFAULT_TYPES_KEY);
                String types = baseTypesOrdinal();
                ps.setString(2, "[" + types + "]");
            });

        enhancedRandom = new EnhancedRandomBuilder()
            .seed(1)
            .stringLengthRange(0, 100)
            .build();
        AuditTemplatesProvider auditTemplatesProvider = new AuditTemplatesProvider(jdbcTemplate,
            Collections.emptyList(), null,
            1, "test", false, TABLE_NAME, 1, 1, "audit",
            lockHelper);
        auditTemplatesProvider.afterPropertiesSet();
        CachedKeyValueMapService cachedKeyValueMapService = new CachedKeyValueMapService();
        ReflectionTestUtils.setField(cachedKeyValueMapService, "namedScatJdbcTemplate", namedTemplate);
        ytActionLogRepository = Mockito.mock(YtActionLogRepository.class);
        auditService = new MboAuditServiceImpl(auditTemplatesProvider, new SimpleMultiIdGenerator(),
            new AuditSystemPropertiesService(), TABLE_NAME,
            ytActionLogRepository, cachedKeyValueMapService);
    }

    @NotNull
    private String baseTypesOrdinal() {
        return Arrays.stream(MboAudit.EntityType.values())
            .map(Enum::ordinal).sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    }

    @NotNull
    private String baseTypesNames() {
        return Arrays.stream(MboAudit.EntityType.values())
            .map(Enum::name)
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldCreateSqlFilterWithWhereAndPrewhere() {
        SqlFilter actual = auditService.makeSqlFilter(FindActionsRequest.newBuilder()
            // prewhere
            .setSourceId("source_id")
            .setEventId(1)
            .setActionId(2)
            .setEntityId(3)
            // where
            .addActionType(MboAudit.ActionType.CHECK)
            .addEntityType(MboAudit.EntityType.CM_BLUE_OFFER)
            .addPropertyName("property")
            .setCategoryId(4)
            .setParameterId(5)
            .setEnvironment("environment")
            .addBillingMode(MboAudit.BillingMode.BILLING_MODE_COPY)
            .setSource(MboAudit.Source.YANG_TASK)
            .addStaffLogin("staff_login")
            .addUserId(6)
            .build());
        Assertions.assertThat(actual.toString()).isEqualTo("SqlFilter{" +
            // preWhere
            "preWhereText='1=1 and entity_id = :entity_id and event_id = :event_id and action_id = :action_id " +
            "and source_id = :source_id', " +
            // where
            "whereText='1=1 and action_type in (:action_type) " +
            "and entity_type in (:entity_type) and property_name in (:property_name) and category_id = :category_id " +
            "and parameter_id = :parameter_id and environment = :environment and billing_mode in (:billing_mode) " +
            "and source = :source and (user_id in (:user_id) or staff_login in (:staff_login))', " +
            // params
            "params={action_type=[CHECK], entity_type=[CM_BLUE_OFFER], entity_id=3, " +
            "property_name=[property], event_id=1, category_id=4, parameter_id=5, environment=environment, " +
            "billing_mode=[BILLING_MODE_COPY], action_id=2, source=YANG_TASK, source_id=source_id, user_id=[6], " +
            "staff_login=[staff_login], offset=0, length=0}}");
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldSetupDateIfTsPassed() {
        FindActionsRequest request = FindActionsRequest.newBuilder()
            .addAllEntityType(Arrays.asList(MboAudit.EntityType.values())).build();
        SqlFilter actual = auditService.makeSqlFilter(request);

        String dynamicBaseTypes = baseTypesNames();
        Assertions.assertThat(actual.toString()).isEqualTo("SqlFilter{" +
            "preWhereText='1=1', " +
            "whereText='1=1 and entity_type in (:entity_type)', params={entity_type=[" +
            dynamicBaseTypes +
            "], offset=0, length=0}}");

        LocalDate startDate = LocalDate.of(2021, 4, 7);
        LocalDate finDate = LocalDate.of(2021, 4, 8);

        ZoneOffset current = ZonedDateTime.now().getOffset();
        actual = auditService.makeSqlFilter(FindActionsRequest.newBuilder()
            .setStartDate(startDate.atStartOfDay().toInstant(current).toEpochMilli())
            .setFinishDate(finDate.atStartOfDay().toInstant(current).toEpochMilli())
            .build());
        Assertions.assertThat(actual.toString()).isEqualTo("SqlFilter{" +
            "preWhereText='1=1 and date >= :start_date and date < :finish_date', " +
            "whereText='1=1 and entity_type in (:entity_type)', " +
            "params={start_date=2021-04-07, finish_date=2021-04-08, " +
            "entity_type=[" + dynamicBaseTypes + "], offset=0," +
            " length=0}}");
        actual = auditService.makeSqlFilter(FindActionsRequest.newBuilder()
            .setStartTimestamp(startDate.atStartOfDay().toInstant(current).toEpochMilli())
            .setFinishTimestamp(finDate.atStartOfDay().toInstant(current).toEpochMilli())
            .build());
        Assertions.assertThat(actual.toString()).isEqualTo("SqlFilter{" +
            "preWhereText='1=1 and date >= :start_date and date <= :finish_date', " +
            "whereText='1=1 and timestamp >= :start_timestamp and timestamp < :finish_timestamp " +
            "and entity_type in (:entity_type)', " +
            "params={start_timestamp=2021-04-07 00:00:00.0, start_date=2021-04-07, " +
            "finish_timestamp=2021-04-08 00:00:00.0, finish_date=2021-04-08, " +
            "entity_type=[" + dynamicBaseTypes + "], " +
            "offset=0, length=0}}");
        LocalDateTime startTs = LocalDateTime.of(2021, 4, 9, 21, 51, 43, 361000000);
        LocalDateTime finTs = LocalDateTime.of(2021, 4, 10, 21, 51, 43, 361000000);
        actual = auditService.makeSqlFilter(FindActionsRequest.newBuilder()
            .setStartTimestamp(startTs.toInstant(current).toEpochMilli())
            .setFinishTimestamp(finTs.toInstant(current).toEpochMilli())
            .build());
        Assertions.assertThat(actual.toString()).isEqualTo("SqlFilter{" +
            "preWhereText='1=1 and date >= :start_date and date <= :finish_date', " +
            "whereText='1=1 and timestamp >= :start_timestamp and timestamp < :finish_timestamp " +
            "and entity_type in (:entity_type)', " +
            "params={start_timestamp=2021-04-09 21:51:43.361, start_date=2021-04-09, " +
            "finish_timestamp=2021-04-10 21:51:43.361, finish_date=2021-04-10, " +
            "entity_type=[" + dynamicBaseTypes + "], " +
            "offset=0, length=0}}");
        actual = auditService.makeSqlFilter(FindActionsRequest.newBuilder()
            .setStartDate(startDate.atStartOfDay().toInstant(current).toEpochMilli())
            .setFinishDate(finDate.atStartOfDay().toInstant(current).toEpochMilli())
            .setStartTimestamp(startTs.toInstant(current).toEpochMilli())
            .setFinishTimestamp(finTs.toInstant(current).toEpochMilli())
            .build());
        Assertions.assertThat(actual.toString()).isEqualTo("SqlFilter{" +
            "preWhereText='1=1 and date >= :start_date and date < :finish_date', " +
            "whereText='1=1 and timestamp >= :start_timestamp and timestamp < :finish_timestamp " +
            "and entity_type in (:entity_type)', " +
            "params={start_date=2021-04-07, finish_date=2021-04-08, " +
            "start_timestamp=2021-04-09 21:51:43.361, finish_timestamp=2021-04-10 21:51:43.361, " +
            "entity_type=[" + dynamicBaseTypes + "], " +
            "offset=0, length=0}}");
    }

    @Test
    public void testWriteActions() {
        MboAction.Builder action = MboAction.newBuilder()
            .setEnvironment(enhancedRandom.nextObject(String.class))
            .setActionType(enhancedRandom.nextObject(MboAudit.ActionType.class))
            .setEntityType(enhancedRandom.nextObject(MboAudit.EntityType.class))
            .setEntityId(enhancedRandom.nextInt())
            .setUserId(enhancedRandom.nextInt());
        // write action
        auditService.writeActions(WriteActionsRequest.newBuilder().addActions(action).build());

        // check write is correct
        List<Map<String, Object>> result = jdbcTemplate.getJdbcOperations()
            .queryForList("select * from " + TABLE_NAME);
        Assertions.assertThat(result).hasSize(1);

        // check count request
        MboAudit.CountActionsResponse countActionsResponse = auditService.countActions(
            FindActionsRequest.newBuilder().setLength(Integer.MAX_VALUE).build()
        );
        Assertions.assertThat(countActionsResponse.getCount(0)).isEqualTo(1);

        // check find request
        MboAudit.FindActionsResponse actions = auditService.findActions(
            FindActionsRequest.newBuilder().setLength(Integer.MAX_VALUE).build()
        );
        verifyAction(action, actions.getActionsList(), "", false);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytActionLogRepository, Mockito.times(0))
            .saveAuditActions(Mockito.anyList());
    }

    @Test
    public void testWriteActionsBlueOffers() {
        MboAction.Builder action = MboAction.newBuilder()
            .setEnvironment(enhancedRandom.nextObject(String.class))
            .setActionType(enhancedRandom.nextObject(MboAudit.ActionType.class))
            .setEntityType(CM_BLUE_OFFER)
            .setEntityId(enhancedRandom.nextInt())
            .setUserId(enhancedRandom.nextInt());
        // write action
        auditService.writeActions(WriteActionsRequest.newBuilder().addActions(action).build());

        // check write is correct
        List<Map<String, Object>> result = jdbcTemplate.getJdbcOperations()
            .queryForList("select * from " + TABLE_NAME);
        Assertions.assertThat(result).hasSize(0);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytActionLogRepository, Mockito.times(1))
            .saveAuditActions(captor.capture());
        List<MboAction> actionsToYt = captor.getValue();
        verifyAction(action, actionsToYt, "", false);
    }

    @Test
    public void testWriteAndCountNotBlueOffer() {
        MboAction.Builder action = MboAction.newBuilder()
            .setEnvironment(enhancedRandom.nextObject(String.class))
            .setActionType(enhancedRandom.nextObject(MboAudit.ActionType.class))
            .setEntityType(CATEGORY)
            .setEntityId(enhancedRandom.nextInt())
            .setUserId(enhancedRandom.nextInt())
            .setPropertyName("a_prop");
        // write action
        auditService.writeActions(WriteActionsRequest.newBuilder().addActions(action).build());

        // check write is correct
        List<Map<String, Object>> result = jdbcTemplate.getJdbcOperations()
            .queryForList("select * from " + TABLE_NAME);
        Assertions.assertThat(result).hasSize(1);

        // check count request
        MboAudit.CountActionsResponse countActionsResponse = auditService.countActions(
            FindActionsRequest.newBuilder().setLength(Integer.MAX_VALUE).build()
        );
        // should be 1
        Assertions.assertThat(countActionsResponse.getCount(0)).isEqualTo(1);
    }

    @Test
    public void testWriteSystemProperty() {
        MboAction.Builder action = MboAction.newBuilder()
            .setEnvironment(enhancedRandom.nextObject(String.class))
            .setActionType(enhancedRandom.nextObject(MboAudit.ActionType.class))
            .setEntityType(CM_BLUE_OFFER)
            .setEntityId(enhancedRandom.nextInt())
            .setUserId(enhancedRandom.nextInt())
            .setPropertyName("acceptance_status_modified");
        // write action
        auditService.writeActions(WriteActionsRequest.newBuilder().addActions(action).build());

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ytActionLogRepository, Mockito.times(1))
            .saveAuditActions(captor.capture());
        List<MboAction> actionsToYt = captor.getValue();
        verifyAction(action, actionsToYt, "acceptance_status_modified", true);
    }


    private void verifyAction(MboAction.Builder action, List<MboAction> actionsToYt, String s, boolean b) {
        Assertions.assertThat(actionsToYt)
            .hasSize(1)
            .first()
            .isEqualToIgnoringGivenFields(
                action
                    .setEventId(0)
                    .setEntityName("")
                    .setPropertyName(s)
                    .setOldValue("")
                    .setNewValue("")
                    .setCategoryId(0)
                    .setParameterId(0)
                    .setBillingMode(MboAudit.BillingMode.BILLING_MODE_NONE)
                    .setStaffLogin("")
                    .setActionId(1)
                    .setSource(MboAudit.Source.MBO)
                    .setSourceId("")
                    .setSystemProperty(b)
                    .build(),
                "bitField0_", "date_", "environment_");
    }

    private static class SimpleMultiIdGenerator implements MultiIdGenerator {
        private AtomicLong id = new AtomicLong();

        @Override
        public List<Long> getIds(int count) {
            ArrayList<Long> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(getId());
            }
            return ids;
        }

        @Override
        public long getId() {
            return id.getAndIncrement();
        }
    }
}
