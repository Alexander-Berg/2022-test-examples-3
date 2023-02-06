package ru.yandex.direct.ess.router.rules.bsexport.resources

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables.BANNER_MULTICARDS
import ru.yandex.direct.dbschema.ppc.Tables.BANNER_MULTICARD_SETS
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate.New
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate.No
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate.Ready
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate.Sending
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate.Sent
import ru.yandex.direct.dbschema.ppc.enums.BannerMulticardSetsStatusmoderate.Yes
import ru.yandex.direct.ess.common.utils.TablesEnum
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.BannerMulticardSetsChange
import ru.yandex.direct.ess.router.testutils.BannerMulticardSetsChange.createBannerMulticardSetsBinlogEvent
import ru.yandex.direct.ess.router.testutils.BannerMulticardsChange
import ru.yandex.direct.ess.router.testutils.BannerMulticardsChange.createBannerMulticardsBinlogEvent
import ru.yandex.direct.test.utils.randomPositiveLong
import java.util.stream.Stream

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerMulticardSetFilterTest {

    @Autowired
    private lateinit var rule: BsExportBannerResourcesRule

    private fun paramsInsertMulticardSet(): Stream<Arguments> = Stream.of(
        Arguments.of(123L, New, null),
        Arguments.of(123L, Ready, null),
        Arguments.of(123L, Sending, null),
        Arguments.of(123L, Sent, null),
        Arguments.of(123L, Yes, createBannerMulticardsObject(123L)),
        Arguments.of(123L, No, createBannerMulticardsObject(123L)),
    )

    @ParameterizedTest(name = "Insert {1}")
    @MethodSource("paramsInsertMulticardSet")
    fun testInsertMulticardSet(
        bid: Long,
        statusModerate: BannerMulticardSetsStatusmoderate,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = BannerMulticardSetsChange(bid).apply {
            addInsertedColumn(BANNER_MULTICARD_SETS.STATUS_MODERATE, statusModerate.literal)
        }
        val event = createBannerMulticardSetsBinlogEvent(listOf(change), Operation.INSERT)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun paramsUpdateMulticardSet(): Stream<Arguments> = Stream.of(
        Arguments.of(123L, New, Ready, null),
        Arguments.of(123L, Ready, New, null),
        Arguments.of(123L, Ready, Sending, null),
        Arguments.of(123L, Sending, Sent, null),
        Arguments.of(123L, Sending, Ready, null),
        Arguments.of(123L, Sent, Ready, null),
        Arguments.of(123L, Sent, Yes, createBannerMulticardsObject(123L)),
        Arguments.of(123L, Sent, No, createBannerMulticardsObject(123L)),
        Arguments.of(123L, Yes, Ready, null),
        Arguments.of(123L, No, Ready, null),
    )

    @ParameterizedTest(name = "Update {1} -> {2}")
    @MethodSource("paramsUpdateMulticardSet")
    fun testUpdateMulticardSet(
        bid: Long,
        statusBefore: BannerMulticardSetsStatusmoderate,
        statusAfter: BannerMulticardSetsStatusmoderate,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = BannerMulticardSetsChange(bid).apply {
            addChangedColumn(BANNER_MULTICARD_SETS.STATUS_MODERATE, statusBefore.literal, statusAfter.literal)
        }
        val event = createBannerMulticardSetsBinlogEvent(listOf(change), Operation.UPDATE)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    private fun paramsDeleteMulticardSet(): Stream<Arguments> = Stream.of(
        Arguments.of(123L, New, createBannerMulticardsObject(123L, isDeleted = true)),
        Arguments.of(123L, Ready, createBannerMulticardsObject(123L, isDeleted = true)),
        Arguments.of(123L, Sending, createBannerMulticardsObject(123L, isDeleted = true)),
        Arguments.of(123L, Sent, createBannerMulticardsObject(123L, isDeleted = true)),
        Arguments.of(123L, Yes, createBannerMulticardsObject(123L, isDeleted = true)),
        Arguments.of(123L, No, createBannerMulticardsObject(123L, isDeleted = true)),
    )

    @ParameterizedTest(name = "Delete {1}")
    @MethodSource("paramsDeleteMulticardSet")
    fun testDeleteMulticardSet(
        bid: Long,
        statusModerate: BannerMulticardSetsStatusmoderate,
        expected: BsExportBannerResourcesObject?,
    ) {
        val change = BannerMulticardSetsChange(bid).apply {
            addDeletedColumn(BANNER_MULTICARD_SETS.STATUS_MODERATE, statusModerate.literal)
        }
        val event = createBannerMulticardSetsBinlogEvent(listOf(change), Operation.DELETE)

        val objects = rule.mapBinlogEvent(event)

        if (expected != null) {
            assertThat(objects).containsObjects(expected)
        } else {
            assertThat(objects).isEmpty()
        }
    }

    @Test
    fun updateOrderTest() {
        val multicardId1 = randomPositiveLong()
        val multicardId2 = randomPositiveLong()

        val change1 = BannerMulticardsChange(multicardId1).apply {
            addChangedColumn(BANNER_MULTICARDS.ORDER_NUM, 0L, 1L)
        }
        val change2 = BannerMulticardsChange(multicardId2).apply {
            addChangedColumn(BANNER_MULTICARDS.ORDER_NUM, 1L, 0L)
        }
        val event = createBannerMulticardsBinlogEvent(listOf(change1, change2), Operation.UPDATE)

        val objects = rule.mapBinlogEvent(event)

        assertThat(objects).containsObjects(
            BsExportBannerResourcesObject.Builder().apply {
                setAdditionalTable(TablesEnum.BANNER_MULTICARDS)
                setAdditionalId(multicardId1)
                setResourceType(BannerResourceType.BANNER_MULTICARD)
            }.build(),
            BsExportBannerResourcesObject.Builder().apply {
                setAdditionalTable(TablesEnum.BANNER_MULTICARDS)
                setAdditionalId(multicardId2)
                setResourceType(BannerResourceType.BANNER_MULTICARD)
            }.build(),
        )
    }

    private fun createBannerMulticardsObject(bid: Long, isDeleted: Boolean = false): BsExportBannerResourcesObject {
        return BsExportBannerResourcesObject.Builder().apply {
            setBid(bid)
            setDeleted(isDeleted)
            setResourceType(BannerResourceType.BANNER_MULTICARD)
        }.build()
    }

    private fun ListAssert<BsExportBannerResourcesObject>.containsObjects(vararg objects: BsExportBannerResourcesObject) {
        usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(*objects)
    }
}
