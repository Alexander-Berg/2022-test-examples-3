package ru.yandex.market.mbi.orderservice.tms.service.logbroker.events

import com.google.protobuf.Timestamp
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.partner.event.PartnerInfo

/**
 * Тесты для [PartnerInfoSnapshotProcessor]
 */
@DbUnitDataSet(before = ["PartnerInfoSnapshotProcessorTest.csv"])
class PartnerInfoSnapshotProcessorTest : FunctionalTest() {
    private val NEWER = Timestamp.newBuilder().setSeconds(1635168817).build()
    private val OLDER = Timestamp.newBuilder().setSeconds(1632576816).build()

    @Autowired
    lateinit var partnerInfoSnapshotProcessor: PartnerInfoSnapshotProcessor

    @Test
    @DbUnitDataSet(after = ["PartnerInfoSnapshotProcessorTest.old.after.csv"])
    fun `verify that old partnerInfo doesn't erase actual`() {
        partnerInfoSnapshotProcessor.process(MessageBatch("partner-info", 0, listOf(createSnapshot(100L, true))))
    }

    @Test
    @DbUnitDataSet(after = ["PartnerInfoSnapshotProcessorTest.new.after.csv"])
    fun `verify that new partnerInfo erase current`() {
        partnerInfoSnapshotProcessor.process(MessageBatch("partner-info", 0, listOf(createSnapshot(100L, false))))
    }

    @Test
    @DbUnitDataSet(after = ["PartnerInfoSnapshotProcessorTest.insert.after.csv"])
    fun `verify insert new partnerInfo`() {
        partnerInfoSnapshotProcessor.process(
            MessageBatch(
                "partner-info",
                0,
                listOf(createSnapshot(300L, true))
            )
        )
    }

    @Test
    @DbUnitDataSet(after = ["PartnerInfoSnapshotProcessorTest.batch.after.csv"])
    fun `verify new and old partnerInfo in single batch`() {
        partnerInfoSnapshotProcessor.process(
            MessageBatch(
                "partner-info", 0,
                listOf(
                    createSnapshot(100L, false),
                    createSnapshot(200L, true),
                    createSnapshot(
                        300L, true
                    )
                )
            )
        )
    }

    private fun createSnapshot(partnerId: Long, older: Boolean): MessageData {
        return MessageData(
            PartnerInfo.PartnerInfoEvent.newBuilder()
                .setBusinessId(1000L)
                .setId(partnerId)
                .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
                .setIsDropship(true)
                .setName("test$partnerId")
                .setOrganizationName("testOrgName$partnerId")
                .setTimestamp(if (older) OLDER.seconds else NEWER.seconds)
                .build().toByteArray(),
            0,
            mock {
                on(it.codec).thenReturn(CompressionCodec.RAW)
            }
        )
    }
}
