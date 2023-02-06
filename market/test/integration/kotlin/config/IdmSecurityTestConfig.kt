package ru.yandex.market.logistics.calendaring.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.calendaring.config.idm.IdmRoleSecurityConfigurationAdapter

@Configuration
@Import(IdmRoleSecurityConfigurationAdapter::class)
open class IdmSecurityTestConfig
