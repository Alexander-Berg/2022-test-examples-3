package ru.yandex.direct.grid.processing.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.uac.grut.ThreadLocalGrutContext
import ru.yandex.direct.core.testing.db.TestPpcPropertiesSupport
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.test.grut.GrutTestClientFactory
import ru.yandex.grut.client.GrutClient

@Configuration
@Import(GridProcessingTestingConfiguration::class)
class GrutGridProcessingTestingConfiguration {
    @Bean
    @Primary
    fun grutClient(): GrutClient {
        return GrutTestClientFactory.getGrutClient()
    }

    @Bean
    @Primary
    fun ppcPropertiesSupport(dslContextProvider: DslContextProvider): PpcPropertiesSupport {
        return TestPpcPropertiesSupport(dslContextProvider)
    }
}
