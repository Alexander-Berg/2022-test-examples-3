package ru.yandex.market.logistics.calendaring.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.calendaring.config.dbqueue.DbqueueProperties

@EnableConfigurationProperties(DbqueueProperties::class)
@Import(DbqueueNotActiveConfig::class, DataBaseConnectionConfig::class)
@ComponentScan(
    "ru.yandex.market.logistics.calendaring.dbqueue",
    "ru.yandex.market.logistics.calendaring.config.dbqueue",
    "ru.yandex.market.logistics.calendaring.solomon.repository"
)
class DbqueueNotActiveIntegrationTestConfig
