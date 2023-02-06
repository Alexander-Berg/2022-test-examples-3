package ru.yandex.market.mdm.storage.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import ru.yandex.market.mdm.storage.service.physical.YtTableProvider

@Profile("test")
@TestConfiguration
@Import(MdmYtTableRpcConfig::class)
open class TestMdmYtTableRpcConfig(
    private val mdmYtTableRpcConfig: MdmYtTableRpcConfig
) {

    // Чуть магии: в тестах всегда используем тяжелый провайдер, которому нужны все кластеры,
    // так как в тестах нет доступа к skv
    @Bean
    open fun ytTableProvider(): YtTableProvider {
        return mdmYtTableRpcConfig.newAndExistedYtTableProvider()
    }
}
