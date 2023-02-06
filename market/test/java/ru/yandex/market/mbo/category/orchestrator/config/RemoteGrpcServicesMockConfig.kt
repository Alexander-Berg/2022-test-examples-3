package ru.yandex.market.mbo.category.orchestrator.config

import com.nhaarman.mockitokotlin2.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.mbo.category.orchestrator.service.mboc.MbocCategoryMigrationService

@Profile("test")
@Configuration
class RemoteGrpcServicesMockConfig {

    @Bean
    fun mbocCategoryMigrationService(): MbocCategoryMigrationService = mock()
}
