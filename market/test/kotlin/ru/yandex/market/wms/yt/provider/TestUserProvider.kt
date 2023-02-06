package ru.yandex.market.wms.yt.provider

import org.apache.commons.lang.NotImplementedException
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Component
@Primary
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class TestUserProvider : ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider {
    override fun getUser(): String {
        return "anonymousUser"
    }

    override fun getToken(): String {
        throw NotImplementedException()
    }

    override fun getRoles(): Set<String> {
        return HashSet()
    }
}
