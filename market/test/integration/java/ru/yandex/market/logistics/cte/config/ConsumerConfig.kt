package ru.yandex.market.logistics.cte.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan("ru.yandex.market.logistics.cte.dbqueue.*")
class ConsumerConfig
