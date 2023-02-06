package ru.yandex.direct.ess.router.rules.bsexport.resources

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.BinlogEvent.Row
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables.BANNERS
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGN_PROMOACTIONS
import ru.yandex.direct.dbschema.ppc.Tables.PROMOACTIONS
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.No
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Ready
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Sending
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Sent
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Yes
import ru.yandex.direct.ess.common.utils.TablesEnum
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.ess.logicobjects.bsexport.resources.DebugInfo
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.BaseTableChange
import ru.yandex.direct.ess.router.testutils.TestUtils
import java.util.stream.Collectors
import java.util.stream.Stream

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerPromoExtensionFilterTest {
    @Autowired
    private lateinit var rule: BsExportBannerResourcesRule

    private fun paramsUpdatePromoExtension(): Stream<Arguments> = Stream.of(
        Arguments.of(123L, Ready, Sending, null),
        Arguments.of(123L, Sending, Sent, null),
        Arguments.of(123L, Sending, Ready, null),
        Arguments.of(123L, Sent, Ready, null),
        Arguments.of(123L, Sent, Yes, createBannerPromoExtensionObject(123L, TablesEnum.PROMOACTIONS)),
        Arguments.of(123L, Sent, No, createBannerPromoExtensionObject(123L, TablesEnum.PROMOACTIONS)),
        Arguments.of(123L, Yes, Ready, null),
        Arguments.of(123L, No, Ready, null),
    )

    @ParameterizedTest(name = "Update {1} -> {2}")
    @MethodSource("paramsUpdatePromoExtension")
    fun testUpdatePromoactions(
        promoExtensionId: Long,
        statusBefore: PromoactionsStatusmoderate,
        statusAfter: PromoactionsStatusmoderate,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = PromoactionChange(promoExtensionId).apply {
            addChangedColumn(PROMOACTIONS.STATUS_MODERATE, statusBefore.literal, statusAfter.literal)
        }
        val event = createPromoactionsBinlogEvent(listOf(change), Operation.UPDATE)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun paramsAddPromoactionsBinding(): Stream<Arguments> = Stream.of(
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
    )

    @ParameterizedTest(name = "Insert {1}")
    @MethodSource("paramsAddPromoactionsBinding")
    fun testAddPromoactionsBinding(
        cid: Long,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = CampaignPromoactionsChange(cid)
        val event = createCampaignPromoactionsBinlogEvent(listOf(change), Operation.INSERT)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun paramsUpdatePromoExtensionBinding(): Stream<Arguments> = Stream.of(
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
    )

    @ParameterizedTest(name = "Update {1} -> {2}")
    @MethodSource("paramsUpdatePromoExtensionBinding")
    fun testUpdatePromoactionBinding(
        cid: Long,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = CampaignPromoactionsChange(cid)
        val event = createCampaignPromoactionsBinlogEvent(listOf(change), Operation.UPDATE)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun paramsDeletePromoExtensionBinding(): Stream<Arguments> = Stream.of(
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
        Arguments.of(123L,
            createBannerPromoExtensionObject(123L, TablesEnum.CAMPAIGN_PROMOACTIONS)),
    )

    @ParameterizedTest(name = "Delete {1}")
    @MethodSource("paramsDeletePromoExtensionBinding")
    fun testUnbindPromoactions(
        cid: Long,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = CampaignPromoactionsChange(cid)
        val event = createCampaignPromoactionsBinlogEvent(listOf(change), Operation.DELETE)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun paramsInsertIntoBanners(): Stream<Arguments> = Stream.of(
        Arguments.of(123L, Ready, createBannerPromoExtensionObject(123L, TablesEnum.BANNERS)),
        Arguments.of(123L, Sending, createBannerPromoExtensionObject(123L, TablesEnum.BANNERS)),
        Arguments.of(123L, Sent, createBannerPromoExtensionObject(123L, TablesEnum.BANNERS)),
        Arguments.of(123L, Yes, createBannerPromoExtensionObject(123L, TablesEnum.BANNERS)),
        Arguments.of(123L, No, createBannerPromoExtensionObject(123L, TablesEnum.BANNERS)),
    )

    @ParameterizedTest(name = "Insert {1}")
    @MethodSource("paramsInsertIntoBanners")
    fun testInsertIntoBanners(
        bid: Long,
        statusModerate: PromoactionsStatusmoderate,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = BannerChange(bid).apply {
            addInsertedColumn(PROMOACTIONS.STATUS_MODERATE, statusModerate.literal)
            addInsertedColumn(BANNERS.BANNER_TYPE, BannersBannerType.text.literal)
        }
        val event = createBannersBinlogEvent(listOf(change), Operation.INSERT)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun createBannerPromoExtensionObject(
        additionalId: Long,
        additionalTable: TablesEnum,
        isDeleted: Boolean = false,
    ): BsExportBannerResourcesObject {
        val result = BsExportBannerResourcesObject.Builder().apply {
            setBid(null)
            setDeleted(isDeleted)
            setResourceType(BannerResourceType.BANNER_PROMO_EXTENSION)
            setAdditionalId(additionalId)
            setAdditionalTable(additionalTable)
        }.build()
        result.debugInfo = DebugInfo(0, "", "")
        return result
    }

    private fun ListAssert<BsExportBannerResourcesObject>.containsObjects(vararg objects: BsExportBannerResourcesObject) {
        usingRecursiveFieldByFieldElementComparator().contains(*objects)
    }

    private fun createPromoactionsBinlogEvent(
        promoExtensionChangeList: List<PromoactionChange>,
        operation: Operation
    ): BinlogEvent {
        val event = BinlogEvent()
            .withTable(PROMOACTIONS.name)
            .withOperation(operation)
        val rows = promoExtensionChangeList.stream()
            .map { change -> createPromoExtensionTableRow(change, operation) }
            .collect(Collectors.toList())
        event.withRows(rows)
        return event
    }

    private fun createPromoExtensionTableRow(change: PromoactionChange, operation: Operation): Row {
        val primaryKeys = mapOf(PROMOACTIONS.ID.name to change.promoExtensionId)
        val before: Map<String, Any?> = mutableMapOf()
        val after: Map<String, Any?> = mutableMapOf()
        TestUtils.fillChangedInRow(before, after, change, operation)
        return Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
    }

    private fun createCampaignPromoactionsBinlogEvent(
        campaignPromoactionChangeList: List<CampaignPromoactionsChange>,
        operation: Operation
    ): BinlogEvent {
        val event = BinlogEvent()
            .withTable(CAMPAIGN_PROMOACTIONS.name)
            .withOperation(operation)
        val rows: List<Row> = campaignPromoactionChangeList.stream()
            .map { change -> createCampaignPromoactionsTableRow(change, operation) }
            .collect(Collectors.toList())
        event.withRows(rows)
        return event
    }

    private fun createCampaignPromoactionsTableRow(change: CampaignPromoactionsChange, operation: Operation): Row {
        val primaryKeys = mapOf(CAMPAIGN_PROMOACTIONS.CID.name to change.cid)
        val before: Map<String, Any?> = mutableMapOf()
        val after: Map<String, Any?> = mutableMapOf()
        TestUtils.fillChangedInRow(before, after, change, operation)
        return Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
    }

    private fun createBannersBinlogEvent(
        promoExtensionChangeList: List<BannerChange>,
        operation: Operation
    ): BinlogEvent {
        val event = BinlogEvent()
            .withTable(BANNERS.name)
            .withOperation(operation)
        val rows = promoExtensionChangeList.stream()
            .map { change -> createBannersTableRow(change, operation) }
            .collect(Collectors.toList())
        event.withRows(rows)
        return event
    }

    private fun createBannersTableRow(change: BannerChange, operation: Operation): Row {
        val primaryKeys = mapOf(BANNERS.BID.name to change.bid)
        val before: Map<String, Any?> = mutableMapOf()
        val after: Map<String, Any?> = mutableMapOf()
        TestUtils.fillChangedInRow(before, after, change, operation)
        return Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after)
    }

    internal inner class PromoactionChange(var promoExtensionId: Long) : BaseTableChange()

    internal inner class CampaignPromoactionsChange(var cid: Long) : BaseTableChange()

    internal inner class BannerChange(var bid: Long) : BaseTableChange()
}
