package ru.yandex.market.logistics.mqm.service.ytevents

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import java.time.Instant

class YtEventCleanerTest : AbstractContextualTest() {

    @Autowired
    private lateinit var ytEventCleaner: YtEventCleaner

    @Test
    @DatabaseSetup("/service/ytevents/cleaner/before/clear_outdated_events.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/cleaner/after/clear_outdated_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun clearOutdatedYtEvents() {
        clock.setFixed(Instant.parse("2021-08-02T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        ytEventCleaner.run()
    }
}
