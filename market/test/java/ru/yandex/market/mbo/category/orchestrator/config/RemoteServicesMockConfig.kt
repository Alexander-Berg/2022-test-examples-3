package ru.yandex.market.mbo.category.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.mbo.category.orchestrator.service.mbo.ModelStorageServiceMock

@Profile("test")
@Configuration
class RemoteServicesMockConfig {

    @Bean
    fun modelStorageService() = ModelStorageServiceMock()
}
