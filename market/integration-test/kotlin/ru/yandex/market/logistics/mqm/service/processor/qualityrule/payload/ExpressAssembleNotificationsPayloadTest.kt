package ru.yandex.market.logistics.mqm.service.processor.qualityrule.payload

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import java.time.Duration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.rules.payloads.CabinetInfo
import ru.yandex.market.logistics.mqm.entity.rules.payloads.ChannelNotificationSettings
import ru.yandex.market.logistics.mqm.entity.rules.payloads.ExpressAssembleNotificationsPayload
import ru.yandex.market.logistics.mqm.entity.rules.payloads.Range
import ru.yandex.market.logistics.mqm.repository.QualityRuleRepository

class ExpressAssembleNotificationsPayloadTest: AbstractContextualTest() {

    @Autowired
    lateinit var repository: QualityRuleRepository

    @Test
    @DatabaseSetup("/service/processor/qualityrule/payload/rule_with_payload.xml")
    fun parserPayload() {
        repository.findById(1L).get().rule!! shouldBeEqualToComparingFields ExpressAssembleNotificationsPayload(
            sender = "test_sender",
            notificationRepeatInterval = Duration.ofMinutes(3),
            detailedDelayLevels = listOf(
                Range(from = Duration.ofMinutes(3), to = Duration.ofMinutes(6)),
                Range(from = Duration.ofMinutes(6), to = Duration.ofMinutes(8)),
            ),
            channelsSettings = mapOf(
                "notification_for_brand_1" to ChannelNotificationSettings(
                    enabled = true,
                    channel = "test_channel",
                    threshold = 1,
                    thresholdDelay = Duration.ofMinutes(2),
                    limit = 4,
                    partnersInMessage = 5,
                    ordersInMessage = 6,
                    cabinets = listOf(
                        CabinetInfo(
                            id = 1,
                            name = "test_cabinet_1",
                            partners = listOf(1, 2, 3),
                        ),
                        CabinetInfo(
                            id = 2,
                            name = "test_cabinet_2",
                            partners = listOf(4, 5, 6),
                        )
                    ),
                    commonPartnerName = "test_common_name"
                )
            ),
        )
    }
}
