package ru.yandex.market.mbi.orderservice.tms.service.pg

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DbUnitDataSet(before = ["environment-service-test-before.csv"])
class EnvironmentServiceTest : FunctionalTest() {

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Test
    fun testGetBoolean() {
        Assertions.assertThat(environmentService.getBoolean("boolKey1")).isTrue
        Assertions.assertThat(environmentService.getBoolean("boolKey2")).isTrue
        Assertions.assertThat(environmentService.getBoolean("boolKey3")).isTrue
        Assertions.assertThat(environmentService.getBoolean("boolKey4")).isFalse
    }

    @Test
    fun testGetLong() {
        Assertions.assertThat(environmentService.getLong("longKey1")).isEqualTo(123456L)
        Assertions.assertThatExceptionOfType(NumberFormatException::class.java).isThrownBy {
            environmentService.getLong("longKey2")
        }
    }

    @Test
    fun testSetValue() {
        Assertions.assertThat(environmentService.getString("testKey1")).isEqualTo("value1")
        environmentService.set("testKey1", "value2")
        Assertions.assertThat(environmentService.getString("testKey1")).isEqualTo("value2")
    }

    @Test
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun testNotifyChanged() {
        val latch = CountDownLatch(1)
        environmentService.runOnValueChanged("trackedKey", Duration.ofMillis(100)) {
            latch.countDown()
        }
        environmentService.set("trackedKey", "abracadabra")
    }
}
