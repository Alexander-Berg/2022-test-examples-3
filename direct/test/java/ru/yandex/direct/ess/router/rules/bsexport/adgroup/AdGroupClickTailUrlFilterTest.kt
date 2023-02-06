package ru.yandex.direct.ess.router.rules.bsexport.adgroup

import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.direct.ess.router.testutils.GroupParamsTableChange
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange
import ru.yandex.direct.ess.router.testutils.createGroupParamsEvent

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class AdGroupClickTailUrlFilterTest {
    @Autowired
    private lateinit var rule: BsExportAdGroupRule

    @Test
    fun `phrases insert test`() {
        val change = PhrasesTableChange()
            .withPid(423)
            .withCid(123456)
        change.addChangedColumn(Tables.PHRASES.ADGROUP_TYPE, null, PhrasesAdgroupType.base.literal)
        val binlogEvent = PhrasesTableChange.createPhrasesEvent(listOf(change), Operation.INSERT)
        val logicObject = rule.mapBinlogEvent(binlogEvent)
        assertThat(logicObject)
            .usingRecursiveFieldByFieldElementComparator()
            .contains(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.CLICK_URL_TAIL,
                    adGroupId = change.pid,
                    campaignId = change.cid,
                    debugInfo = DebugInfo(),
                )
            )
    }

    @Test
    fun `group_params insert test`() {
        val change = GroupParamsTableChange(pid = 123)
        val binlogEvent = createGroupParamsEvent(listOf(change), Operation.INSERT)
        val logicObject = rule.mapBinlogEvent(binlogEvent)
        assertThat(logicObject)
            .usingRecursiveFieldByFieldElementComparator()
            .contains(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.CLICK_URL_TAIL,
                    adGroupId = change.pid,
                    campaignId = null,
                    debugInfo = DebugInfo(),
                )
            )
    }

    @Test
    fun `group_params update test`() {
        val change = GroupParamsTableChange(pid = 123)
        change.addChangedColumn(Tables.GROUP_PARAMS.HREF_PARAMS, "x", "{banner_id}")
        val binlogEvent = createGroupParamsEvent(listOf(change), Operation.UPDATE)
        val logicObject = rule.mapBinlogEvent(binlogEvent)
        assertThat(logicObject)
            .usingRecursiveFieldByFieldElementComparator()
            .contains(
                BsExportAdGroupObject(
                    resourceType = AdGroupResourceType.CLICK_URL_TAIL,
                    adGroupId = change.pid,
                    campaignId = null,
                    debugInfo = DebugInfo(),
                )
            )
    }
}
