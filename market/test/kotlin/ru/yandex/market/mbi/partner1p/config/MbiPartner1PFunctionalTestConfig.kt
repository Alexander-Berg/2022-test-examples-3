package ru.yandex.market.mbi.partner1p.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import ru.yandex.market.mbi.partner1p.SpringApplicationConfig
import javax.annotation.PostConstruct

/**
 *
 * @author lozovskii@yandex-team.ru
 */
@Configuration
@Profile("functionalTest")
@Import(
    SpringApplicationConfig::class,
    BalanceConfigTest::class
)
open class MbiPartner1PFunctionalTestConfig {

    @PostConstruct
    open fun setSystemProperties() {
        System.setProperty("org.jooq.no-logo", "true")
    }
}
