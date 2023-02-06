package ru.yandex.market.wms.inventory_management.config

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@Component
class TestSecurityDataProvider : ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider {
    override fun getUser(): String {
        return "TEST"
    }

    override fun getToken(): String {
        return "TEST_TOKEN"
    }

    override fun getRoles(): MutableSet<String> {
        return HashSet()
    }
}
