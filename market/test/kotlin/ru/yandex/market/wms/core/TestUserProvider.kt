package ru.yandex.market.wms.core

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.yandex.market.wms.core.configuration.authentication.UserProvider
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Component
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class TestUserProvider : UserProvider {
    override fun getUser(): String = "anonymousUser"
}
