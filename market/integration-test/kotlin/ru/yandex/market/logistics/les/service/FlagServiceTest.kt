package ru.yandex.market.logistics.les.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.configuration.CacheConfiguration
import ru.yandex.market.logistics.les.entity.enums.FlagKey

class FlagServiceTest : AbstractContextualTest() {

    @Autowired
    lateinit var flagService: FlagService

    @Autowired
    lateinit var cacheService: CacheService

    @BeforeEach
    fun setUp() {
        cacheService.evict(CacheConfiguration.FLAG_CACHE)
        cacheService.evict(CacheConfiguration.BOOLEAN_FLAG_CACHE)
    }

    @Test
    @DatabaseSetup("/services/flag/before/existing_flag.xml")
    fun getExistingValue() {
        flagService.getValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe "test value"
    }

    @Test
    fun getNonExistingValue() {
        flagService.getValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe FlagKey.EXAMPLE_FLAG_KEY.defaultValue
    }

    @Test
    @DatabaseSetup("/services/flag/before/true_flag.xml")
    fun getBooleanValueTrue() {
        flagService.getBooleanValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe true
    }

    @Test
    @DatabaseSetup("/services/flag/before/false_flag.xml")
    fun getBooleanValueFalse() {
        flagService.getBooleanValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe false
    }

    @Test
    @DatabaseSetup("/services/flag/before/unparseable_flag.xml")
    fun getBooleanValueUnparseable() {
        flagService.getBooleanValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe
            FlagKey.EXAMPLE_FLAG_KEY.defaultValue.toBooleanStrict()
    }

    @Test
    fun getBooleanValueNonExisting() {
        flagService.getBooleanValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe
            FlagKey.EXAMPLE_FLAG_KEY.defaultValue.toBooleanStrict()
    }
}
