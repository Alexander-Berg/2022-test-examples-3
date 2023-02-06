package ru.yandex.market.wms.dimensionmanagement.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.wms.dimensionmanagement.security.AESCypherSettings
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class CypherConfig {
    @Value("\${wms.dimension-management.encryption.aes.key}")
    private val encryptionKey: String = ""

    @Value("\${wms.dimension-management.encryption.aes.salt}")
    private val salt: String = ""

    @Bean
    fun aesCypherSettings(): AESCypherSettings {
        return AESCypherSettings(encryptionKey, salt)
    }
}
