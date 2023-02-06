package ru.yandex.direct.ess.router.models.rule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlogbroker.logbroker_utils.models.BinlogEventWithOffset;
import ru.yandex.direct.ess.common.models.BaseEssConfig;
import ru.yandex.direct.ess.common.models.BaseLogicObject;
import ru.yandex.direct.ess.common.models.LogicObjectListWithInfo;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.ESS_ADDITIONAL_OBJECTS;

public class AbstractRuleAdditionalObjectsTest {

    private static final String TEST_LOGIC_PROCESSOR = "test_logic_processor";
    private static final int PARTITION = 0;

    @Test
    void test() {
        var testLogicObject = new TestObject(1);
        var binlogEventWithOffset = createInsertEvent(1, JsonUtils.toJson(testLogicObject));
        var rule = new TestRule();
        var ruleProcessingResult = rule.processEvents(List.of(binlogEventWithOffset));
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions()).containsKey(PARTITION);
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions().get(PARTITION)).hasSize(1);
        var processedObject = ruleProcessingResult.getGroupedLogicObjectsByPartitions().get(PARTITION).get(0);

        JavaType logicObjectWithSystemInfoType =
                JsonUtils.getTypeFactory().constructParametricType(LogicObjectListWithInfo.class, TestObject.class);
        LogicObjectListWithInfo<TestObject> gotLogicObject =
                JsonUtils.fromJson(processedObject.getLogicObjectWithSystemInfo(), logicObjectWithSystemInfoType);
        assertThat(gotLogicObject.getLogicObjectsList()).hasSize(1);
        assertThat(gotLogicObject.getLogicObjectsList().get(0)).isEqualToComparingFieldByField(testLogicObject);

        assertThat(ruleProcessingResult.getGroupedStatByPartitions()).containsKey(PARTITION);
        var stat = ruleProcessingResult.getGroupedStatByPartitions().get(PARTITION);
        assertThat(stat.getAdditionalObjectsCnt()).isEqualTo(1);
        assertThat(stat.getProcessedObjectsCnt()).isEqualTo(0);
        assertThat(stat.getAdditionalSkippedCnt()).isEqualTo(0);

    }

    @Test
    void oneNormalOneUnparsableInDifferentEventsTest() {
        var testLogicObject = new TestObject(1);
        var binlogEventWithOffsetNormal = createInsertEvent(1, JsonUtils.toJson(testLogicObject));
        var binlogEventWithOffsetUnparsable = createInsertEvent(2, "error_json");
        var rule = new TestRule();
        var ruleProcessingResult = rule.processEvents(List.of(binlogEventWithOffsetNormal,
                binlogEventWithOffsetUnparsable));
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions()).containsKey(PARTITION);
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions().get(PARTITION)).hasSize(1);
        var processedObject = ruleProcessingResult.getGroupedLogicObjectsByPartitions().get(PARTITION).get(0);

        JavaType logicObjectWithSystemInfoType =
                JsonUtils.getTypeFactory().constructParametricType(LogicObjectListWithInfo.class, TestObject.class);
        LogicObjectListWithInfo<TestObject> gotLogicObject =
                JsonUtils.fromJson(processedObject.getLogicObjectWithSystemInfo(), logicObjectWithSystemInfoType);
        assertThat(gotLogicObject.getLogicObjectsList()).hasSize(1);
        assertThat(gotLogicObject.getLogicObjectsList().get(0)).isEqualToComparingFieldByField(testLogicObject);

        assertThat(ruleProcessingResult.getGroupedStatByPartitions()).containsKey(PARTITION);
        var stat = ruleProcessingResult.getGroupedStatByPartitions().get(PARTITION);
        assertThat(stat.getAdditionalObjectsCnt()).isEqualTo(1);
        assertThat(stat.getProcessedObjectsCnt()).isEqualTo(0);
        assertThat(stat.getAdditionalSkippedCnt()).isEqualTo(1);
    }

    @Test
    void oneNormalOneUnparsableInOneEventTest() {
        var testLogicObject = new TestObject(1);
        var binlogEventWithOffset = createInsertEvent(List.of(
                Pair.of(1L, JsonUtils.toJson(testLogicObject)),
                Pair.of(2L, "error_json")));
        var rule = new TestRule();
        var ruleProcessingResult = rule.processEvents(List.of(binlogEventWithOffset));
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions()).containsKey(PARTITION);
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions().get(PARTITION)).hasSize(1);
        var processedObject = ruleProcessingResult.getGroupedLogicObjectsByPartitions().get(PARTITION).get(0);

        JavaType logicObjectWithSystemInfoType =
                JsonUtils.getTypeFactory().constructParametricType(LogicObjectListWithInfo.class, TestObject.class);
        LogicObjectListWithInfo<TestObject> gotLogicObject =
                JsonUtils.fromJson(processedObject.getLogicObjectWithSystemInfo(), logicObjectWithSystemInfoType);
        assertThat(gotLogicObject.getLogicObjectsList()).hasSize(1);
        assertThat(gotLogicObject.getLogicObjectsList().get(0)).isEqualToComparingFieldByField(testLogicObject);

        assertThat(ruleProcessingResult.getGroupedStatByPartitions()).containsKey(PARTITION);
        var stat = ruleProcessingResult.getGroupedStatByPartitions().get(PARTITION);
        assertThat(stat.getAdditionalObjectsCnt()).isEqualTo(1);
        assertThat(stat.getProcessedObjectsCnt()).isEqualTo(0);
        assertThat(stat.getAdditionalSkippedCnt()).isEqualTo(1);
    }

    @Test
    void testUnparsableObject() {
        var binlogEventWithOffset = createInsertEvent(1, "error_json");
        var rule = new TestRule();
        var ruleProcessingResult = rule.processEvents(List.of(binlogEventWithOffset));
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions()).hasSize(0);

        assertThat(ruleProcessingResult.getGroupedStatByPartitions()).containsKey(PARTITION);
        var stat = ruleProcessingResult.getGroupedStatByPartitions().get(PARTITION);
        assertThat(stat.getAdditionalObjectsCnt()).isEqualTo(0);
        assertThat(stat.getProcessedObjectsCnt()).isEqualTo(0);
        assertThat(stat.getAdditionalSkippedCnt()).isEqualTo(1);
    }

    @Test
    void testNullObject() {
        var binlogEventWithOffset = createInsertEvent(1, null);
        var rule = new TestRule();
        var ruleProcessingResult = rule.processEvents(List.of(binlogEventWithOffset));
        assertThat(ruleProcessingResult.getGroupedLogicObjectsByPartitions()).hasSize(0);

        assertThat(ruleProcessingResult.getGroupedStatByPartitions()).containsKey(PARTITION);
        var stat = ruleProcessingResult.getGroupedStatByPartitions().get(PARTITION);
        assertThat(stat.getAdditionalObjectsCnt()).isEqualTo(0);
        assertThat(stat.getProcessedObjectsCnt()).isEqualTo(0);
        assertThat(stat.getAdditionalSkippedCnt()).isEqualTo(1);
    }

    private BinlogEventWithOffset createInsertEvent(long id, String serializedObjects) {
        return createInsertEvent(List.of(Pair.of(id, serializedObjects)));
    }

    private BinlogEventWithOffset createInsertEvent(List<Pair<Long, String>> idWithSerializedObjects) {
        BinlogEvent binlogEvent = new BinlogEvent();
        binlogEvent.setOperation(INSERT);
        binlogEvent.setTable(ESS_ADDITIONAL_OBJECTS.getName());
        List<BinlogEvent.Row> rows = new ArrayList<>();
        for (var idWithSerializedObject : idWithSerializedObjects) {
            Map<String, Object> insertMap = new HashMap<>();
            insertMap.put(ESS_ADDITIONAL_OBJECTS.ID.getName(), idWithSerializedObject.getLeft());
            insertMap.put(ESS_ADDITIONAL_OBJECTS.LOGIC_PROCESS_NAME.getName(), TEST_LOGIC_PROCESSOR);
            insertMap.put(ESS_ADDITIONAL_OBJECTS.LOGIC_OBJECT.getName(), idWithSerializedObject.getRight());
            rows.add(
                    new BinlogEvent.Row()
                            .withAfter(insertMap)
                            .withBefore(Map.of())
                            .withPrimaryKey(Map.of(ESS_ADDITIONAL_OBJECTS.ID.getName(),
                                    idWithSerializedObject.getLeft())));
        }
        binlogEvent.setRows(rows);
        binlogEvent.setUtcTimestamp(LocalDateTime.of(2020, 1, 1, 1, 1));
        return new BinlogEventWithOffset(binlogEvent, 0, PARTITION, 0);
    }

    @EssRule(value = TestConfig.class)
    private static class TestRule extends AbstractRule<TestObject> {
        @Override
        public List<TestObject> mapBinlogEvent(BinlogEvent binlogEvent) {
            return List.of();
        }
    }

    public static class TestConfig extends BaseEssConfig {
        @Override
        public Class<? extends BaseLogicObject> getLogicObject() {
            return TestObject.class;
        }

        @Override
        public String getLogicProcessName() {
            return TEST_LOGIC_PROCESSOR;
        }

        @Override
        public String getTopic() {
            return "topic";
        }

        @Override
        public int getRowsThreshold() {
            return 0;
        }

        @Override
        public Duration getTimeToReadThreshold() {
            return null;
        }

        @Override
        public Duration getCriticalEssProcessTime() {
            return null;
        }

        @Override
        public boolean processReshardingEvents() {
            // выставлено явно при замене умолчания в базовом классе: DIRECT-171006
            // необязательно означает, что для этого процесса нужно обрабатывать события от решардинга, просто настройку добавили позже: DIRECT-122901
            return true;
        }
    }

    private static class TestObject extends BaseLogicObject {
        @JsonProperty("id")
        private long id;

        @JsonCreator
        public TestObject(@JsonProperty("id") long id) {
            this.id = id;
        }
    }
}
