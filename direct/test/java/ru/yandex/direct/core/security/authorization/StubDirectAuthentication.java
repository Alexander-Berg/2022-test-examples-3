package ru.yandex.direct.core.security.authorization;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.security.core.GrantedAuthority;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;

@ParametersAreNonnullByDefault
public class StubDirectAuthentication extends DirectAuthentication {

    public StubDirectAuthentication(User operator, User client) {
        super(operator, client);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

}
