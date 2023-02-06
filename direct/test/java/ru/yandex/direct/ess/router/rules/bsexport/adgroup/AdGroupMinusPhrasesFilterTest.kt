package ru.yandex.direct.ess.router.rules.bsexport.adgroup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables.MINUS_WORDS
import ru.yandex.direct.dbschema.ppc.Tables.PHRASES
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType
import ru.yandex.direct.ess.logicobjects.bsexport.DebugInfo
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.AdGroupResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.BsExportAdGroupObject
import ru.yandex.direct.ess.logicobjects.bsexport.adgroup.MinusPhrasesInfo
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.*

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class AdGroupMinusPhrasesFilterTest {
    @Autowired
    private lateinit var rule: BsExportAdGroupRule

    @Test
    fun insertPhrasesTest() {
        val change = PhrasesTableChange().withPid(45L).withCid(12L)
        change.addChangedColumn(PHRASES.ADGROUP_TYPE, null, PhrasesAdgroupType.base.literal)
        val changes = listOf(change)
        val binlogEvent = PhrasesTableChange.createPhrasesEvent(changes, Operation.INSERT)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = 12L,
            resourceType = AdGroupResourceType.MINUS_PHRASES,
            debugInfo = DebugInfo(),
            additionalInfo = null)
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updatePhrasesMwIdTest() {
        val change = PhrasesTableChange().withPid(45L).withCid(12L)
        change.addChangedColumn(PHRASES.MW_ID, 1234, 4321)
        val changes = listOf(change)
        val binlogEvent = PhrasesTableChange.createPhrasesEvent(changes, Operation.UPDATE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = 12L,
            resourceType = AdGroupResourceType.MINUS_PHRASES,
            debugInfo = DebugInfo(),
            additionalInfo = null)
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun insertAdGroupsMinusWordsTest() {
        val change = AdGroupsMinusWordsTableChange(pid = 45L)
        val changes = listOf(change)
        val binlogEvent = createdAdGroupsMinusWordsEvent(changes, Operation.INSERT)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = null,
            resourceType = AdGroupResourceType.MINUS_PHRASES,
            debugInfo = DebugInfo(),
            additionalInfo = null)
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun deleteAdGroupsMinusWordsTest() {
        val change = AdGroupsMinusWordsTableChange(pid = 45L)
        val changes = listOf(change)
        val binlogEvent = createdAdGroupsMinusWordsEvent(changes, Operation.DELETE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = 45L,
            campaignId = null,
            resourceType = AdGroupResourceType.MINUS_PHRASES,
            debugInfo = DebugInfo(),
            additionalInfo = null)
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updateMinusWordsTextTest() {
        val change = MinusWordsTableChange(minusWordId = 14567L)
        change.addChangedColumn(MINUS_WORDS.MW_TEXT, "aaaa", "bbbb")
        val changes = listOf(change)
        val binlogEvent = createdMinusWordsEvent(changes, Operation.UPDATE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportAdGroupObject(
            adGroupId = null,
            campaignId = null,
            resourceType = AdGroupResourceType.MINUS_PHRASES,
            debugInfo = DebugInfo(),
            additionalInfo = MinusPhrasesInfo(14567L))
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }
}
