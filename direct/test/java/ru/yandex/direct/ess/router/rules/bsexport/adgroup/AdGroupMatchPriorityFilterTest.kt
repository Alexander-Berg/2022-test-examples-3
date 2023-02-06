package ru.yandex.direct.ess.router.rules.bsexport.adgroup

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType
import ru.yandex.direct.ess.logicobjects.bsexport.DebugInfo
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.BsExportAdGroupObject
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.AdGroupPriorityTableChange
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange
import ru.yandex.direct.ess.router.testutils.createAdGroupPriorityEvent

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class AdGroupMatchPriorityFilterTest {
    @Autowired
    private lateinit var rule: BsExportAdGroupRule

    @Test
    fun insertPhrasesTest() {
        val change = PhrasesTableChange().withPid(45L).withCid(12L)
        change.addChangedColumn(Tables.PHRASES.ADGROUP_TYPE, null, PhrasesAdgroupType.cpm_price.literal)
        val changes = listOf(change)
        val binlogEvent = PhrasesTableChange.createPhrasesEvent(changes, Operation.INSERT)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = 12L,
            resourceType = AdGroupResourceType.MATCH_PRIORITY,
            debugInfo = DebugInfo(),
        )
        Assertions.assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun insertAdgroupPriorityTest() {
        val change = AdGroupPriorityTableChange(pid = 45)
        change.addChangedColumn(Tables.ADGROUP_PRIORITY.PRIORITY, null, 3)
        val changes = listOf(change)
        val binlogEvent = createAdGroupPriorityEvent(changes, Operation.INSERT)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = null,
            resourceType = AdGroupResourceType.MATCH_PRIORITY,
            debugInfo = DebugInfo(),
        )
        Assertions.assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updateAdgroupPriorityTest() {
        val change = AdGroupPriorityTableChange(pid = 45)
        change.addChangedColumn(Tables.ADGROUP_PRIORITY.PRIORITY, 1, 3)
        val changes = listOf(change)
        val binlogEvent = createAdGroupPriorityEvent(changes, Operation.UPDATE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = null,
            resourceType = AdGroupResourceType.MATCH_PRIORITY,
            debugInfo = DebugInfo(),
        )
        Assertions.assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }
}
