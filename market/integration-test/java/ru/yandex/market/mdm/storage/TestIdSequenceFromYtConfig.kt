package ru.yandex.market.mdm.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import ru.yandex.common.util.db.MultiIdGenerator
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.market.mdm.storage.config.MdmStorageYtConfig
import ru.yandex.market.mdm.storage.helper.YtIdGenerator
import ru.yandex.market.mdm.storage.service.YtEventGenerator

@Configuration
@Import(MdmStorageYtConfig::class)
open class TestIdSequenceFromYtConfig(
    private val mdmStorageYtConfig: MdmStorageYtConfig
) {

    @Value("\${mdm-storage-api.sequence-path}")
    private var sequencePath: String = ""

    @Bean(initMethod = "init")
    open fun ytIdGenerator(): MultiIdGenerator {
        return YtIdGenerator(
            mdmStorageYtConfig.markovYt(),
            YPath.simple(sequencePath)
        )
    }

    @Bean
    @Primary
    open fun ytEventGenerator() = YtEventGenerator(ytIdGenerator())

    @Bean
    @Primary
    open fun pgSequenceMdmIdGenerator() = ytIdGenerator()
}
