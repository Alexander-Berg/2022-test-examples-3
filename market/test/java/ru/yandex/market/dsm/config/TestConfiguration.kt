package ru.yandex.market.dsm.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.dsm.core.test.ClockUtil
import ru.yandex.market.javaframework.main.config.TraceConfiguration
import ru.yandex.market.tpl.common.util.DateTimeUtil
import java.time.Clock
import java.time.Duration

@Configuration
@Import(
    TraceConfiguration::class,
)
@EnableAutoConfiguration
@EnableCaching
class TestConfiguration {

    @Bean
    @Primary
    fun clockForTests(): Clock {
        val clock = TestableClock()
        clock.setFixed(
            ClockUtil.defaultDateTime().toInstant(DateTimeUtil.DEFAULT_ZONE_ID),
            DateTimeUtil.DEFAULT_ZONE_ID
        )
        return Mockito.spy(clock)
    }


    @Bean
    @Profile(DsmConstants.ENV.FUNCTIONAL_TEST_PROFILE)
    fun fiveMinutesCacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(0)
                .expireAfterWrite(Duration.ofMinutes(0))
        )
        return caffeineCacheManager
    }

}
