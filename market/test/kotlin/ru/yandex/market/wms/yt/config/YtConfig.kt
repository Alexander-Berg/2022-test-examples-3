package ru.yandex.market.wms.yt.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class YtConfig {
    @Bean
    fun yt(): Yt = Mockito.mock(Yt::class.java)

    @Bean
    fun runningJobsMap(): ConcurrentHashMap<Long, Future<*>> = ConcurrentHashMap()

    @Bean
    fun yqlTemplate(): NamedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate::class.java)
}
