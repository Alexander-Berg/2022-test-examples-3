package ru.yandex.direct.ess.router.rules.bsexport.adgroup

import one.util.streamex.EntryStream
import org.assertj.core.api.Assertions.assertThat
import org.jooq.Field
import org.jooq.Table
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType
import ru.yandex.direct.ess.logicobjects.bsexport.DebugInfo
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.BsExportAdGroupObject
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.MultiplierInfo
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.HierarchicalMultipliersChange
import java.math.BigInteger

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class AdGroupMultiplierFilterTest {
    @Autowired
    private lateinit var rule: BsExportAdGroupRule

    @Test
    fun insertTest() {
        val weatherOnCampaign = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(1L)
            .withCid(10L)
            .withType(HierarchicalMultipliersType.weather_multiplier)
        val weatherOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(2L)
            .withCid(20L)
            .withPid(200L)
            .withType(HierarchicalMultipliersType.weather_multiplier)
        val trafficOnCampaign = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(3L)
            .withCid(30L)
            .withType(HierarchicalMultipliersType.express_traffic_multiplier)
        val contentDurationOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(4L)
            .withCid(40L)
            .withPid(400L)
            .withType(HierarchicalMultipliersType.express_content_duration_multiplier)
        val prismaIncomeGrade = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(5L)
            .withCid(50L)
            .withPid(500L)
            .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier)
        val retargetingOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(6L)
            .withCid(60L)
            .withPid(600L)
            .withType(HierarchicalMultipliersType.retargeting_multiplier)
        val retargetingFilterOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(7L)
            .withCid(70L)
            .withPid(700L)
            .withType(HierarchicalMultipliersType.retargeting_filter)
        val binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
            listOf(weatherOnCampaign, weatherOnAdGroup, trafficOnCampaign, contentDurationOnAdGroup, prismaIncomeGrade,
                retargetingOnAdGroup, retargetingFilterOnAdGroup), Operation.INSERT)
        val objects = rule.mapBinlogEvent(binlogEvent)
        assertThat(objects)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 200L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 400L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 500L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 600,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 700,
                    campaignId = null,
                    debugInfo = DebugInfo()
                )

            )
    }

    @Test
    fun updateTest() {
        val weatherOnCampaign = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(1L)
            .withCid(10L)
            .withType(HierarchicalMultipliersType.weather_multiplier)
        val weatherOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(2L)
            .withCid(20L)
            .withPid(200L)
            .withType(HierarchicalMultipliersType.weather_multiplier)
        val trafficOnCampaign = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(3L)
            .withCid(30L)
            .withType(HierarchicalMultipliersType.express_traffic_multiplier)
        val contentDurationOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(4L)
            .withCid(40L)
            .withPid(400L)
            .withType(HierarchicalMultipliersType.express_content_duration_multiplier)
        val prismaIncomeGrade = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(5L)
            .withCid(50L)
            .withPid(500L)
            .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier)
        val retargetingOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(6L)
            .withCid(60L)
            .withPid(600L)
            .withType(HierarchicalMultipliersType.retargeting_multiplier)
        val retargetingFilterOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(7L)
            .withCid(70L)
            .withPid(700L)
            .withType(HierarchicalMultipliersType.retargeting_filter)

        val binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
            listOf(weatherOnCampaign, weatherOnAdGroup, trafficOnCampaign, contentDurationOnAdGroup, prismaIncomeGrade,
                retargetingOnAdGroup, retargetingFilterOnAdGroup), Operation.UPDATE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        assertThat(objects)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 200L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 400L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 500L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 600,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 700,
                    campaignId = null,
                    debugInfo = DebugInfo()
                )

            )
    }

    @Test
    fun deleteTest() {
        val weatherOnCampaign = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(1L)
            .withCid(10L)
            .withType(HierarchicalMultipliersType.weather_multiplier)
        val weatherOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(2L)
            .withCid(20L)
            .withPid(200L)
            .withType(HierarchicalMultipliersType.weather_multiplier)
        val trafficOnCampaign = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(3L)
            .withCid(30L)
            .withType(HierarchicalMultipliersType.express_traffic_multiplier)
        val contentDurationOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(4L)
            .withCid(40L)
            .withPid(400L)
            .withType(HierarchicalMultipliersType.express_content_duration_multiplier)
        val prismaIncomeGrade = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(5L)
            .withCid(50L)
            .withPid(500L)
            .withType(HierarchicalMultipliersType.prisma_income_grade_multiplier)
        val retargetingOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(6L)
            .withCid(60L)
            .withPid(600L)
            .withType(HierarchicalMultipliersType.retargeting_multiplier)
        val retargetingFilterOnAdGroup = HierarchicalMultipliersChange()
            .withHierarchicalMultiplierId(7L)
            .withCid(70L)
            .withPid(700L)
            .withType(HierarchicalMultipliersType.retargeting_filter)

        val binlogEvent = HierarchicalMultipliersChange.createMultiplierEvent(
            listOf(weatherOnCampaign, weatherOnAdGroup, trafficOnCampaign, contentDurationOnAdGroup, prismaIncomeGrade,
                retargetingOnAdGroup, retargetingFilterOnAdGroup), Operation.DELETE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        assertThat(objects)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 200L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 400L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 500L,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 600,
                    campaignId = null,
                    debugInfo = DebugInfo()
                ),
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = 700,
                    campaignId = null,
                    debugInfo = DebugInfo()
                )

            )
    }


    @Test
    fun demographyValueInsertTest() {
        val binlogEvent = createMultiplierEvent(Operation.INSERT, Tables.DEMOGRAPHY_MULTIPLIER_VALUES,
            java.util.Map.of(Tables.DEMOGRAPHY_MULTIPLIER_VALUES.DEMOGRAPHY_MULTIPLIER_VALUE_ID, BigInteger.valueOf(12L)),
            java.util.Map.of(Tables.DEMOGRAPHY_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID, BigInteger.valueOf(11L))
        )
        val objects = rule.mapBinlogEvent(binlogEvent)
        assertThat(objects)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = null,
                    campaignId = null,
                    debugInfo = DebugInfo(),
                    additionalInfo = MultiplierInfo(11L)
                )
            )
    }

    @Test
    fun weatherValueUpdateTest() {
        val binlogEvent = createMultiplierEvent(Operation.UPDATE, Tables.WEATHER_MULTIPLIER_VALUES,
            mapOf(Tables.WEATHER_MULTIPLIER_VALUES.WEATHER_MULTIPLIER_VALUE_ID to BigInteger.valueOf(12L)),
            mapOf(Tables.WEATHER_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID to BigInteger.valueOf(11L))
        )
        val objects = rule.mapBinlogEvent(binlogEvent)
        assertThat(objects)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = null,
                    campaignId = null,
                    debugInfo = DebugInfo(),
                    additionalInfo = MultiplierInfo(11L)
                )
            )
    }

    @Test
    fun inventoryValueDeleteTest() {
        val binlogEvent = createMultiplierEvent(Operation.DELETE, Tables.INVENTORY_MULTIPLIER_VALUES,
            mapOf(Tables.INVENTORY_MULTIPLIER_VALUES.INVENTORY_MULTIPLIER_VALUE_ID to BigInteger.valueOf(12L)),
            mapOf(Tables.INVENTORY_MULTIPLIER_VALUES.HIERARCHICAL_MULTIPLIER_ID to BigInteger.valueOf(11L))
        )
        val objects = rule.mapBinlogEvent(binlogEvent)
        assertThat(objects)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.MULTIPLIERS,
                    adGroupId = null,
                    campaignId = null,
                    debugInfo = DebugInfo(),
                    additionalInfo = MultiplierInfo(11L)
                )
            )
    }

    private fun createMultiplierEvent(
        operation: Operation, table: Table<*>, keys: Map<Field<*>, Any>, fields: Map<Field<*>, Any>): BinlogEvent {
        val binlogEvent = BinlogEvent().withTable(table.name).withOperation(operation)
        val before: MutableMap<String, Any> = HashMap()
        val after: MutableMap<String, Any> = HashMap()
        EntryStream.of(fields).forKeyValue { field: Field<*>, obj: Any ->
            if (operation != Operation.INSERT) {
                before[field.name] = obj
            }
            if (operation != Operation.DELETE) {
                after[field.name] = obj
            }
        }
        return binlogEvent.withRows(listOf(
            BinlogEvent.Row()
                .withPrimaryKey(EntryStream.of(keys).mapKeys { obj: Field<*> -> obj.name }.toMap())
                .withBefore(before)
                .withAfter(after)
        ))
    }
}
