package ru.yandex.market.mbi.feed.processor.feed.sync

import okhttp3.MediaType
import okhttp3.ResponseBody
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.stubbing
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.mds.MbiMdsClient
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass
import ru.yandex.market.mbi.feed.processor.test.getProto
import ru.yandex.market.protobuf.tools.NumberConvertionUtils
import java.io.ByteArrayOutputStream

/**
 * Тесты для [ImportFeedsFromMbiService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = ["csv/import.enabled.csv"])
internal class ImportFeedsFromMbiServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var importFeedsFromMbiService: ImportFeedsFromMbiService

    @Autowired
    private lateinit var mbiMdsClient: MbiMdsClient

    @Test
    @DbUnitDataSet(after = ["csv/empty.after.csv"])
    fun `empty file in mds`() {
        mockMds()
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(after = ["csv/only-new-feeds.after.csv"])
    fun `only new feeds in mds`() {
        mockMds("proto/feed1001.proto.json", "proto/feed1002.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/actual-in-db.before.csv"],
        after = ["csv/actual-in-db.after.csv"]
    )
    fun `1 - actual in db, 2 - new from mds`() {
        mockMds("proto/feed1001.proto.json", "proto/feed1002.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/old-in-db.before.csv"],
        after = ["csv/old-in-db.after.csv"]
    )
    fun `old feed in db`() {
        mockMds("proto/feed1001.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/deleted-actual.before.csv"],
        after = ["csv/deleted-actual.after.csv"]
    )
    fun `deleted feed in db, deleted_at is newer than updated_at`() {
        mockMds("proto/feed1001.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/deleted-outdated.before.csv"],
        after = ["csv/old-in-db.after.csv"]
    )
    fun `deleted feed in db, deleted_at is older than updated_at`() {
        mockMds("proto/feed1001.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/delete-active.before.csv"],
        after = ["csv/delete-active.after.csv"]
    )
    fun `active feed in db, there isn't in mds`() {
        mockMds("proto/feed1002.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/delete-deleted.before.csv"],
        after = ["csv/delete-deleted.after.csv"]
    )
    fun `deleted feed in db, there isn't in mds`() {
        mockMds("proto/feed1002.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/old-in-db.before.csv"],
        after = ["csv/default.after.csv"]
    )
    fun `url feed in db, default feed in mds`() {
        mockMds("proto/defaultFeed1001.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/old-in-db.before.csv"],
        after = ["csv/utility-feed-by-url.after.csv"]
    )
    fun `utility feed by url`() {
        mockMds("proto/feed1002.proto.json", "proto/urlStockFeed.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/old-in-db.before.csv"],
        after = ["csv/utility-feed-by-file.after.csv"]
    )
    fun `utility feed by file`() {
        mockMds("proto/feed1002.proto.json", "proto/filePriceFeed.proto.json")
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/old-in-db.before.csv"],
        after = ["csv/multifeed.after.csv"]
    )
    fun `multi feeds with utility feeds`() {
        mockMds(
            "proto/feed1002.proto.json",
            "proto/feed1003.proto.json",
            "proto/filePriceFeed.proto.json",
            "proto/urlStockFeed.proto.json"
        )
        importFeedsFromMbiService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/old-in-db.before.csv"],
        after = ["csv/old-in-db.before.csv"]
    )
    fun `utility feed before assortment`() {
        mockMds("proto/urlStockFeed.proto.json", "proto/feed1002.proto.json")
        Assertions.assertThatThrownBy { importFeedsFromMbiService.importFeeds() }
            .hasMessageStartingWith("Can't find feed_id for utility feed")
    }

    private fun mockMds(vararg proto: String) {
        val mockedMdsFile = (
            proto.map { getProto<FeedUpdateTaskOuterClass.FeedUpdateTask>(it) }
                .map { NumberConvertionUtils.toByteArrayInReversedOrder(it.serializedSize) + it.toByteArray() }
                .reduceOrNull { acc, bytes -> acc + bytes }
                ?: ByteArray(0)
            )

        val buffer = ByteArrayOutputStream()
        for (p in proto.map { getProto<FeedUpdateTaskOuterClass.FeedUpdateTask>(it) }) {
            buffer.write(NumberConvertionUtils.toByteArrayInReversedOrder(p.serializedSize))
            p.writeTo(buffer)
        }

        stubbing(mbiMdsClient) {
            onBlocking {
                downloadFile(any())
            }.thenReturn(ResponseBody.create(MediaType.parse("application/x-protobuf"), mockedMdsFile))
        }
    }
}
