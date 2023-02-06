package ru.yandex.market.replenishment.autoorder.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmAuthenticationToken;

final class WithMockTvmSecurityContextFactory implements WithSecurityContextFactory<WithMockTvm> {

    public SecurityContext createSecurityContext(WithMockTvm withUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(getToken(withUser));
        return context;
    }

    private Authentication getToken(WithMockTvm withUser) {
        return new TvmAuthenticationToken(withUser.value(), null);
    }
}
