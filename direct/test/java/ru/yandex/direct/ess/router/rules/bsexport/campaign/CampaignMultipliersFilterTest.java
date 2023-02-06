package ru.yandex.direct.ess.router.rules.bsexport.campaign;

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
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.MultiplierInfo;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.HierarchicalMultipliersChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.DEMOGRAPHY_MULTIPLIER_VALUES;
import static ru.yandex.direct.dbschema.ppc.Tables.INVENTORY_MULTIPLIER_VALUES;
import static ru.yandex.direct.dbschema.ppc.Tables.WEATHER_MULTIPLIER_VALUES;
import static ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType.MULTIPLIERS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignMultipliersFilterTest {
    @Autowired
    private BsExportCampaignRule rule;

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
        var trafficOnCampaign = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(3L)
                .withCid(30L)
                .withType(HierarchicalMultipliersType.express_traffic_multiplier);
        var contentDurationOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(4L)
                .withCid(40L)
                .withPid(400L)
                .withType(HierarchicalMultipliersType.express_content_duration_multiplier);
        var prismaIncomeGradeOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(6L)
                .withCid(60L)
                .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier);
        var binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
                List.of(weatherOnCampaign, weatherOnAdGroup, trafficOnCampaign, contentDurationOnAdGroup,
                        prismaIncomeGradeOnAdGroup),
                INSERT);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        createExportObjectBuilder().setCid(10L).build(),
                        createExportObjectBuilder().setCid(30L).build(),
                        createExportObjectBuilder().setCid(60L).build()
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
        var trafficOnCampaign = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(3L)
                .withCid(30L)
                .withType(HierarchicalMultipliersType.express_traffic_multiplier);
        var contentDurationOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(4L)
                .withCid(40L)
                .withPid(400L)
                .withType(HierarchicalMultipliersType.express_content_duration_multiplier);
        var prismaIncomeGradeOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(6L)
                .withCid(60L)
                .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier);

        var binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
                List.of(weatherOnCampaign, weatherOnAdGroup, trafficOnCampaign, contentDurationOnAdGroup,
                        prismaIncomeGradeOnAdGroup),
                UPDATE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        createExportObjectBuilder().setCid(10L).build(),
                        createExportObjectBuilder().setCid(30L).build(),
                        createExportObjectBuilder().setCid(60L).build()
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
        var trafficOnCampaign = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(3L)
                .withCid(30L)
                .withType(HierarchicalMultipliersType.express_traffic_multiplier);
        var contentDurationOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(4L)
                .withCid(40L)
                .withPid(400L)
                .withType(HierarchicalMultipliersType.express_content_duration_multiplier);
        var prismaIncomeGradeOnAdGroup = new HierarchicalMultipliersChange()
                .withHierarchicalMultiplierId(6L)
                .withCid(60L)
                .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier);

        var binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
                List.of(weatherOnCampaign, weatherOnAdGroup, trafficOnCampaign, contentDurationOnAdGroup,
                        prismaIncomeGradeOnAdGroup),
                DELETE);
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        createExportObjectBuilder().setCid(10L).build(),
                        createExportObjectBuilder().setCid(30L).build(),
                        createExportObjectBuilder().setCid(60L).build()
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
                        createExportObjectBuilder()
                                .setHierarchicalMultiplierId(11L)
                                .setAdditionalInfo(new MultiplierInfo(11))
                                .build()
                );

    }

    @Test
    void weatherValueUpdateTest() {
        BinlogEvent binlogEvent = createMultiplierEvent(UPDATE, WEATHER_MULTIPLIER_VALUES,
                Map.of(WEATHER_MULTIPLIER_VALUES.WEATHER_MULTIPLIER_VALUE_ID, BigInteger.valueOf(12L)),
                Map.of(WEATHER_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID, BigInteger.valueOf(11L))
        );
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        createExportObjectBuilder()
                                .setHierarchicalMultiplierId(11L)
                                .setAdditionalInfo(new MultiplierInfo(11))
                                .build()
                );
    }

    @Test
    void inventoryValueDeleteTest() {
        BinlogEvent binlogEvent = createMultiplierEvent(DELETE, INVENTORY_MULTIPLIER_VALUES,
                Map.of(INVENTORY_MULTIPLIER_VALUES.INVENTORY_MULTIPLIER_VALUE_ID, BigInteger.valueOf(12L)),
                Map.of(INVENTORY_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID, BigInteger.valueOf(11L))
        );
        var objects = rule.mapBinlogEvent(binlogEvent);
        assertThat(objects)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        createExportObjectBuilder()
                                .setHierarchicalMultiplierId(11L)
                                .setAdditionalInfo(new MultiplierInfo(11))
                                .build()
                );
    }

    private BinlogEvent createMultiplierEvent(
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

    private BsExportCampaignObject.Builder createExportObjectBuilder() {
        return new BsExportCampaignObject.Builder()
                .setCampaignResourceType(MULTIPLIERS);
    }
}
