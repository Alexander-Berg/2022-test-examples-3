package ru.yandex.market.replenishment.autoorder.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmAuthenticationToken;

final class WithMockLoginSecurityContextFactory implements WithSecurityContextFactory<WithMockLogin> {

    public SecurityContext createSecurityContext(WithMockLogin withUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getToken(withUser));
        return context;
    }

    private Authentication getToken(WithMockLogin withUser) {
        return new TvmAuthenticationToken(withUser.sourceServiceId(), withUser.value());
    }
}
