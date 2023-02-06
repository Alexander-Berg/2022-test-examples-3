package ru.yandex.market.core.testing;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.core.history.HistoryItem;
import ru.yandex.market.core.history.SnapshotSaxHandler;
import ru.yandex.market.core.moderation.Moderation;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.mbi.util.db.DbUtil;

/**
 * @author ashevenkov
 */
public class DbTestingService implements TestingService {
    private static final Logger log = LoggerFactory.getLogger(DbTestingService.class);

    private static final Set<ShopProgram> MODERATION_PROGRAMS = Sets.immutableEnumSet(
            ShopProgram.CPC,
            ShopProgram.CPA
    );

    // Для совместимости со старой версией премодерации
    private static final Set<ParamType> OLD_CHECK_PARAM_TYPES = Sets.immutableEnumSet(
            ParamType.NEED_STATUS_CHECK_IN_TEST_BASE,
            ParamType.NEED_CLONES_CHECK_IN_TEST_BASE,
            ParamType.NEED_QUALITY_CHECK_IN_TEST_BASE
    );

    private JdbcTemplate jdbcTemplate;
    private TestingStatusDao testingStatusDao;
    private ParamService paramService;

    private static int getAttemptsLeft(TestingState status) {
        return isAttemptAwareTesting(status)
                ? Math.max(0, Moderation.MAX_ATTEMPTS - status.getPushReadyButtonCount())
                : -1;
    }

    private static boolean isAttemptAwareTesting(TestingState status) {
        // TODO replace with ru.yandex.market.core.testing.TestingState.isAttemptCountRequired()
        TestingType testingType = status.getTestingType();
        return testingType == TestingType.FULL_PREMODERATION || testingType == TestingType.CPC_PREMODERATION;
    }

    @Override
    public Map<Long, TestingInfo> getTestingInfos(Collection<Long> datasourceIds) {
        Map<Long, TestingState> statuses = getTestingStatuses(datasourceIds);
        Map<Long, TestingInfo> infos = Maps.newHashMapWithExpectedSize(statuses.size());
        MultiMap<Long, TestingParamStatus> testingParamsStatuses = getTestingParamsStatuses(datasourceIds);
        for (TestingState status : statuses.values()) {
            if (status != null) {
                long datasourceId = status.getDatasourceId();
                TestingInfo info =
                        new TestingInfo(status, testingParamsStatuses.get(datasourceId), getAttemptsLeft(status));
                infos.put(datasourceId, info);
            }
        }
        return infos;
    }

    @Override
    public TestingState getTestingStatus(long datasourceId, ShopProgram shopProgram) {
        return testingStatusDao.load(datasourceId, shopProgram);
    }

    @Override
    public FullTestingState getFullTestingState(long datasourceId) {
        return new FullTestingState(testingStatusDao.load(datasourceId));
    }

    /**
     * Возвращает состояния премодерации по программе CPC. CPC взята для обратной совместимости. По хорошему, нужно
     * переделать код, использующий данный метод.
     */
    @Override
    public Map<Long, TestingState> getTestingStatuses(Collection<Long> datasourceIds) {
        return testingStatusDao.loadMany(datasourceIds, ShopProgram.CPC);
    }

    @Override
    public int getCountOfShopsInTesting() {
        return testingStatusDao.loadAll().size();
    }

    private MultiMap<Long, TestingParamStatus> getTestingParamsStatuses(Collection<Long> datasourceIds) {
        Map<Long, MultiMap<ParamType, ParamValue>> params =
                paramService.getParams(datasourceIds, OLD_CHECK_PARAM_TYPES);
        MultiMap<Long, TestingParamStatus> paramStatuses = new MultiMap<>(datasourceIds.size());
        for (Long datasourceId : datasourceIds) {
            MultiMap<ParamType, ParamValue> dsParams = params.get(datasourceId);
            if (dsParams != null) {
                for (List<ParamValue> values : dsParams.values()) {
                    for (ParamValue paramValue : values) {
                        ParamType paramType = paramValue.getType();
                        paramStatuses.append(datasourceId, new TestingParamStatus(
                                ((BooleanParamValue) paramValue).getValue(),
                                paramType.getName(),
                                paramType.getId()
                        ));
                    }
                }
            }
        }
        return paramStatuses;
    }

    /**
     * @return историю записей в {@code datasources_in_testing} и параметров 29-31.
     */
    @Override
    public List<HistoryItem> getModerationHistory(long datasourceId) {
        Collection<TestingState> testingStates = getFullTestingState(datasourceId)
                .getTestingStates().stream()
                .filter(testingState -> MODERATION_PROGRAMS.contains(testingState.getTestingType().getShopProgram()))
                .collect(Collectors.toList());

        List<HistoryItem> history = new ArrayList<>();
        // Добавляем историю datasources_in_testing для всех типов.
        testingStates.stream()
                .map(TestingState::getId)
                .forEach(id -> history.addAll(loadHistory(id, "datasources_in_testing")));
        // Добавляем историю параметров
        OLD_CHECK_PARAM_TYPES.stream()
                .map(paramType -> getParamValueId(paramType, datasourceId))
                .filter(Objects::nonNull)
                .forEach(pvId -> history.addAll(loadHistory(pvId, "param_value")));
        return history;
    }

    private List<HistoryItem> loadHistory(long entityId, String name) {
        return jdbcTemplate.query(
                "select" +
                        " eh.id," +
                        " eh.action_id," +
                        " eh.entity_name," +
                        " eh.entity_id," +
                        " eh.edit_type," +
                        " eh.xml_snapshot," +
                        " a.action_type_id," +
                        " a.actor_id," +
                        " a.time," +
                        " a.actor_comment," +
                        " a.actor_type" +
                        " from shops_web.entity_history eh, shops_web.action a" +
                        " where eh.action_id = a.id and eh.entity_id = ? and eh.entity_name = ?",
                HistoryItemExtractor.INSTANCE,
                entityId, name
        );
    }

    private Long getParamValueId(ParamType type, long entityId) {
        ParamValue<?> paramValue = paramService.getParam(type, entityId);
        return paramValue == null
                ? null
                : paramValue.getParamId();
    }

    @Override
    public void insertState(ShopActionContext ctx, TestingState state) {
        testingStatusDao.makeNeedTesting(ctx, state);
    }

    @Override
    public void updateState(ShopActionContext ctx, TestingState state) {
        testingStatusDao.update(ctx, state);
    }

    @Override
    public void removeState(ShopActionContext ctx, TestingState state) {
        testingStatusDao.removeFromTesting(ctx.getActionId(), ctx.getShopId(), state.getId());
    }

    @Required
    public void setParamService(ParamService paramService) {
        this.paramService = paramService;
    }

    @Required
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTestingStatusDao(TestingStatusDao testingStatusDao) {
        this.testingStatusDao = testingStatusDao;
    }

    private enum HistoryItemExtractor implements RowMapper<HistoryItem> {
        INSTANCE;

        public HistoryItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            var item = new HistoryItem();
            item.setId(rs.getLong("id"));
            item.setActionId(rs.getLong("action_id"));
            item.setEntityName(rs.getString("entity_name"));
            item.setEntityId(rs.getLong("entity_id"));
            item.setEditType(rs.getInt("edit_type"));
            item.setSnapshot(extractSnapshot(rs));
            item.setActionType(ActionType.getById(rs.getInt("action_type_id")));
            item.setActorId(rs.getLong("actor_id"));
            item.setTime(new Date(rs.getTimestamp("time").getTime()));
            item.setComment(rs.getString("actor_comment"));
            item.setActorType(rs.getLong("actor_type"));
            return item;
        }

        private static Map<String, String> extractSnapshot(ResultSet rs) throws SQLException {
            var snapshot = DbUtil.getClobAsString(rs, "xml_snapshot");
            Map<String, String> result = new HashMap<>(2);
            if (StringUtils.isNotBlank(snapshot)) {
                try {
                    var saxParser = SAXParserFactory.newInstance().newSAXParser();
                    var handler = new SnapshotSaxHandler();
                    saxParser.parse(new InputSource(new StringReader(snapshot)), handler);
                    result = handler.getResult();
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    log.error("Error parsing snapshot : {}", snapshot, e);
                }
            }
            return result;
        }
    }
}
