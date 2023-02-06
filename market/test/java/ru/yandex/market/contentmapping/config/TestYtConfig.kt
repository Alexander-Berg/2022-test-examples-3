package ru.yandex.market.contentmapping.config

import com.nhaarman.mockitokotlin2.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.inside.yt.kosher.Yt

@Configuration
class TestYtConfig : YtConfig() {
    @Bean
    override fun ytHttpApi(): Yt {
        return mock()
    }
}
