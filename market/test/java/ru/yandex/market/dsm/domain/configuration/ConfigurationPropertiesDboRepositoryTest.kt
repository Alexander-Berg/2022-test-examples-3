package ru.yandex.market.dsm.domain.configuration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationProperties
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationPropertiesDboRepository
import java.util.UUID

class ConfigurationPropertiesDboRepositoryTest : AbstractTest() {
    @Autowired
    private lateinit var configurationPropertiesDboRepository: ConfigurationPropertiesDboRepository

    @Test
    fun findByKey() {
        val config = ConfigurationProperties(
            id = UUID.randomUUID().toString(),
            key = "KEY",
            value = "VALUE"
        )
        configurationPropertiesDboRepository.save(config)

        val configFromRep = configurationPropertiesDboRepository.findByKey(config.key)
        Assertions.assertThat(configFromRep).isNotNull
        Assertions.assertThat(configFromRep!!.value).isEqualTo(config.value)
    }
}

