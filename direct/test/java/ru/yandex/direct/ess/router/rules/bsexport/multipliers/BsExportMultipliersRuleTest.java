package ru.yandex.direct.ess.router.rules.bsexport.multipliers;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.jooq.Field;
import org.jooq.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.BsExportMultipliersObject;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.DeleteInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.UpsertInfo;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.HierarchicalMultipliersChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.DEMOGRAPHY_MULTIPLIER_VALUES;
import static ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType.CONTENT_DURATION;
import static ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType.DEMOGRAPHY;
import static ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType.PRISMA_INCOME_MULTIPLIER;
import static ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType.RETARGETING;
import static ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType.TRAFFIC;
import static ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType.WEATHER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BsExportMultipliersRuleTest {
    @Autowired
    private BsExportMultipliersRule rule;

    public static BinlogEvent createMultiplierEvent(
            Operation operation, Table<?> table, Map<Field<?>, Object> keys, Map<Field<?>, Object> fields) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(table.getName()).withOperation(operation);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        EntryStream.of(fields).forKeyValue((field, obj) -> {
            if (!operation.equals(INSERT)) {
                before.put(field.getName(), obj);
            }
            if (!operation.equals(DELETE)) {
                after.put(field.getName(), obj);
            }
        });

        return binlogEvent.withRows(List.of(
                new BinlogEvent.Row()
                        .withPrimaryKey(EntryStream.of(keys).mapKeys(Field::getName).toMap())
                        .withBefore(before)
                        .withAfter(after)
        ));
    }

    @Test
    void insertTest() {
        var weatherOnCampaign = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(1L)
                .withCid(10L)
                .withType(HierarchicalMultipliersType.weather_multiplier);
        var weatherOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(2L)
                .withCid(20L)
                .withPid(200L)
                .withType(HierarchicalMultipliersType.weather_multiplier);
        var trafficOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(3L)
                .withCid(30L)
                .withPid(300L)
                .withType(HierarchicalMultipliersType.express_traffic_multiplier);
        var contentDurationOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(4L)
                .withCid(40L)
                .withPid(400L)
                .withType(HierarchicalMultipliersType.express_content_duration_multiplier);
        var prismaIncomeGradeOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(5L)
                .withCid(50L)
                .withPid(500L)
                .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier);
        var retargetingOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(6L)
                .withCid(60L)
                .withPid(600L)
                .withType(HierarchicalMultipliersType.retargeting_multiplier);
        var retargetingFilterOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(7L)
                .withCid(70L)
                .withPid(700L)
                .withType(HierarchicalMultipliersType.retargeting_filter);

        var binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
                List.of(weatherOnCampaign, weatherOnAdGroup, trafficOnAdGroup, contentDurationOnAdGroup,
                        prismaIncomeGradeOnAdGroup, retargetingOnAdGroup, retargetingFilterOnAdGroup),
                Operation.INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        upserted(WEATHER, 1L),
                        upserted(WEATHER, 2L),
                        upserted(TRAFFIC, 3L),
                        upserted(CONTENT_DURATION, 4L),
                        upserted(PRISMA_INCOME_MULTIPLIER, 5L),
                        upserted(RETARGETING, 6L),
                        upserted(RETARGETING, 7L)
                );
    }

    @Test
    void updateTest() {
        var weatherOnCampaign = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(1L)
                .withCid(10L)
                .withType(HierarchicalMultipliersType.weather_multiplier);
        var weatherOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(2L)
                .withCid(20L)
                .withPid(200L)
                .withType(HierarchicalMultipliersType.weather_multiplier);
        var trafficOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(3L)
                .withCid(30L)
                .withPid(300L)
                .withType(HierarchicalMultipliersType.express_traffic_multiplier);
        var contentDurationOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(4L)
                .withCid(40L)
                .withPid(400L)
                .withType(HierarchicalMultipliersType.express_content_duration_multiplier);
        var prismaIncomeGradeOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(5L)
                .withCid(50L)
                .withPid(500L)
                .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier);
        var retargetingOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(6L)
                .withCid(60L)
                .withPid(600L)
                .withType(HierarchicalMultipliersType.retargeting_multiplier);
        var retargetingFilterOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(7L)
                .withCid(70L)
                .withPid(700L)
                .withType(HierarchicalMultipliersType.retargeting_filter);

        var binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
                List.of(weatherOnCampaign, weatherOnAdGroup, trafficOnAdGroup, contentDurationOnAdGroup,
                        prismaIncomeGradeOnAdGroup, retargetingOnAdGroup, retargetingFilterOnAdGroup),
                Operation.UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        upserted(WEATHER, 1L),
                        upserted(WEATHER, 2L),
                        upserted(TRAFFIC, 3L),
                        upserted(CONTENT_DURATION, 4L),
                        upserted(PRISMA_INCOME_MULTIPLIER, 5L),
                        upserted(RETARGETING, 6L),
                        upserted(RETARGETING, 7L)
                );
    }

    @Test
    void deleteTest() {
        var weatherOnCampaign = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(1L)
                .withCid(10L)
                .withType(HierarchicalMultipliersType.weather_multiplier);
        var weatherOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(2L)
                .withCid(20L)
                .withPid(200L)
                .withType(HierarchicalMultipliersType.weather_multiplier);
        var trafficOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(3L)
                .withCid(30L)
                .withPid(300L)
                .withType(HierarchicalMultipliersType.express_traffic_multiplier);
        var contentDurationOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(4L)
                .withCid(40L)
                .withPid(400L)
                .withType(HierarchicalMultipliersType.express_content_duration_multiplier);
        var prismaIncomeGradeOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(5L)
                .withCid(50L)
                .withPid(500L)
                .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier);
        var retargetingOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(6L)
                .withCid(60L)
                .withPid(600L)
                .withType(HierarchicalMultipliersType.retargeting_multiplier);
        var retargetingFilterOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(7L)
                .withCid(70L)
                .withPid(700L)
                .withType(HierarchicalMultipliersType.retargeting_filter);

        var binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
                List.of(weatherOnCampaign, weatherOnAdGroup, trafficOnAdGroup, contentDurationOnAdGroup,
                        prismaIncomeGradeOnAdGroup, retargetingOnAdGroup, retargetingFilterOnAdGroup),
                Operation.DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        deleted(WEATHER, 10L, null),
                        deleted(WEATHER, 20L, 200L),
                        deleted(TRAFFIC, 30L, 300L),
                        deleted(CONTENT_DURATION, 40L, 400L),
                        deleted(PRISMA_INCOME_MULTIPLIER, 50L, 500L),
                        deleted(RETARGETING, 60L, 600L),
                        deleted(RETARGETING, 70L, 700L)
                );
    }

    @Test
    void demographyValueInsertTest() {
        BinlogEvent binlogEvent = createMultiplierEvent(INSERT, DEMOGRAPHY_MULTIPLIER_VALUES,
                Map.of(DEMOGRAPHY_MULTIPLIER_VALUES.DEMOGRAPHY_MULTIPLIER_VALUE_ID, BigInteger.valueOf(12L)),
                Map.of(DEMOGRAPHY_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID, BigInteger.valueOf(11L))
        );
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        upserted(DEMOGRAPHY, 11L)
                );

    }

    @Test
    void demographyValueUpdateTest() {
        BinlogEvent binlogEvent = createMultiplierEvent(UPDATE, DEMOGRAPHY_MULTIPLIER_VALUES,
                Map.of(DEMOGRAPHY_MULTIPLIER_VALUES.DEMOGRAPHY_MULTIPLIER_VALUE_ID, BigInteger.valueOf(12L)),
                Map.of(DEMOGRAPHY_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID, BigInteger.valueOf(11L))
        );
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        upserted(DEMOGRAPHY, 11L)
                );

    }

    @Test
    void demographyValueDeleteTest() {
        BinlogEvent binlogEvent = createMultiplierEvent(DELETE, DEMOGRAPHY_MULTIPLIER_VALUES,
                Map.of(DEMOGRAPHY_MULTIPLIER_VALUES.DEMOGRAPHY_MULTIPLIER_VALUE_ID, BigInteger.valueOf(12L)),
                Map.of(DEMOGRAPHY_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID, BigInteger.valueOf(11L))
        );
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        upserted(DEMOGRAPHY, 11L)
                );

    }

    private BsExportMultipliersObject upserted(MultiplierType type, Long id) {
        return BsExportMultipliersObject.upsert(new UpsertInfo(type, id), 0L, "", "");
    }

    private BsExportMultipliersObject deleted(MultiplierType type, Long campaignId, Long adGroupId) {
        return BsExportMultipliersObject.delete(new DeleteInfo(type, campaignId, adGroupId), 0L, "", "");
    }
}
