package ru.yandex.direct.ess.router.rules.bsexport.resources

import java.util.stream.Collectors
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.BinlogEvent.Row
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.binlog.model.Operation.DELETE
import ru.yandex.direct.binlog.model.Operation.INSERT
import ru.yandex.direct.binlog.model.Operation.UPDATE
import ru.yandex.direct.dbschema.ppc.Tables.BANNERS_PERFORMANCE
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType.BANNER_SHOW_TITLE_AND_BODY
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject.Builder
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.BaseTableChange
import ru.yandex.direct.ess.router.testutils.TestUtils

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerShowTitleAndBodyTest {
    @Autowired
    private lateinit var rule: BsExportBannerResourcesRule


    @Test
    fun insertTest() {
        val tableChangeYes = BannerShowTitleAndBodyTableChange(1L)
        tableChangeYes.addInsertedColumn(BANNERS_PERFORMANCE.STATUS_MODERATE, "Yes")
        tableChangeYes.addInsertedColumn(BANNERS_PERFORMANCE.BID, 1L)
        val tableChangeNo = BannerShowTitleAndBodyTableChange(2L)
        tableChangeNo.addInsertedColumn(BANNERS_PERFORMANCE.STATUS_MODERATE, "No")
        tableChangeNo.addInsertedColumn(BANNERS_PERFORMANCE.BID, 2L)
        val binlogEvent = createShowTitleAndBodyChangeEvent(listOf(tableChangeYes, tableChangeNo), INSERT)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObjects = arrayOf(
                Builder()
                        .setBid(1L)
                        .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
                        .build(),
                Builder()
                        .setBid(2L)
                        .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
                        .build()
        )
        Assertions.assertThat(objects).hasSize(2)
        Assertions.assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(*expectedObjects)
    }

    @Test
    fun updateInappropriateColumnChangTest() {
        val tableChangeStatusYes = BannerShowTitleAndBodyTableChange(1L)
        tableChangeStatusYes.addChangedColumn(BANNERS_PERFORMANCE.STATUS_MODERATE, "Sent", "Yes")
        tableChangeStatusYes.addChangedColumn(BANNERS_PERFORMANCE.BID, 1L, 1L)
        val tableChangeStatusNo = BannerShowTitleAndBodyTableChange(2L)
        tableChangeStatusNo.addChangedColumn(BANNERS_PERFORMANCE.STATUS_MODERATE, "Yes", "No")
        tableChangeStatusNo.addChangedColumn(BANNERS_PERFORMANCE.BID, 2L, 2L)
        val tableChangeCreativeid = BannerShowTitleAndBodyTableChange(3L)
        tableChangeCreativeid.addChangedColumn(BANNERS_PERFORMANCE.CREATIVE_ID, 15, 16)
        tableChangeCreativeid.addChangedColumn(BANNERS_PERFORMANCE.BID, 3L, 3L)
        val binlogEvent = createShowTitleAndBodyChangeEvent(
            listOf(tableChangeStatusYes, tableChangeStatusNo, tableChangeCreativeid), UPDATE
        )
        val objects = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(objects).isEmpty()
    }

    @Test
    fun updateAppropriateColumnTest() {
        val changeCreativeId = BannerShowTitleAndBodyTableChange(1L)
        changeCreativeId.addChangedColumn(BANNERS_PERFORMANCE.SHOW_TITLE_AND_BODY, 1, 0)
        changeCreativeId.addChangedColumn(BANNERS_PERFORMANCE.BID, 1L, 1L)
        val binlogEvent = createShowTitleAndBodyChangeEvent(listOf(changeCreativeId), UPDATE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObjects = arrayOf(
            Builder()
                .setBid(1L)
                .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
                .build()
        )
        Assertions.assertThat(objects).hasSize(1)
        Assertions.assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(*expectedObjects)
    }

    @Test
    fun deleteTest() {
        val tableChange = BannerShowTitleAndBodyTableChange(1L)
        tableChange.addDeletedColumn(BANNERS_PERFORMANCE.BID, 1L)
        val binlogEvent = createShowTitleAndBodyChangeEvent(listOf(tableChange), DELETE)
        val objects = rule.mapBinlogEvent(binlogEvent)
        val expectedObjects = arrayOf(
                Builder()
                        .setBid(1L)
                        .setResourceType(BANNER_SHOW_TITLE_AND_BODY)
                        .setDeleted(true)
                        .build()
        )
        Assertions.assertThat(objects).hasSize(1)
        Assertions.assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(*expectedObjects)
    }

    private fun createShowTitleAndBodyChangeEvent(
        promoExtensionChangeList: List<BannerShowTitleAndBodyTableChange>,
        operation: Operation
    ): BinlogEvent {
        val event = BinlogEvent()
            .withTable(BANNERS_PERFORMANCE.name)
            .withOperation(operation)
        val rows = promoExtensionChangeList.stream()
            .map { change -> createShowTitleAndBodyTableRow(change, operation) }
            .collect(Collectors.toList())
        event.withRows(rows)
        return event
    }

    private fun createShowTitleAndBodyTableRow(change: BannerShowTitleAndBodyTableChange, operation: Operation): Row {
        val primaryKeys = mapOf(BANNERS_PERFORMANCE.BID.name to change.bid)
        val before: Map<String, Any?> = mutableMapOf()
        val after: Map<String, Any?> = mutableMapOf()
        TestUtils.fillChangedInRow(before, after, change, operation)
        return Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
    }

    internal inner class BannerShowTitleAndBodyTableChange(var bid: Long) : BaseTableChange()
}
