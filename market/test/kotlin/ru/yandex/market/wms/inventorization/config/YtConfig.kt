package ru.yandex.market.wms.inventorization.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class YtConfig {
    @Bean
    fun yt(): Yt = Mockito.mock(Yt::class.java)
}
