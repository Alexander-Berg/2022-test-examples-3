package ru.yandex.market.wms.inventorization.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
open class DbConfig {
    @Bean
    @Primary
    open fun testDbConfigService(nSqlConfigDao: NSqlConfigDao) = DbConfigService(nSqlConfigDao)
}
