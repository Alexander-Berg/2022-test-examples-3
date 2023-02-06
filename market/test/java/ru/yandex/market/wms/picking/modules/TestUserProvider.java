package ru.yandex.market.wms.picking.modules;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@Component
@Primary
@Profile({Profiles.TEST})
public class TestUserProvider implements SecurityDataProvider {

    @Override
    public String getUser() {
        return "anonymousUser";
    }

    @Override
    public String getToken() {
        throw new NotImplementedException();
    }

    @Override
    public Set<String> getRoles() {
        return new HashSet<>();
    }
}
