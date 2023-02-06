package ru.yandex.market.wms.shippingsorter.configuration;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@Profile(Profiles.TEST)
@Component
public class TestSecurityDataProvider implements SecurityDataProvider {
    @Override
    public String getUser() {
        return "TEST";
    }

    @Override
    public String getToken() {
        return "TEST_TOKEN";
    }

    @Override
    public Set<String> getRoles() {
        return new HashSet<>();
    }
}
