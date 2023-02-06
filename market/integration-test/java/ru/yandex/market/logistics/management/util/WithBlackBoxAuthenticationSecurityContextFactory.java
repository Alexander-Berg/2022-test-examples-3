package ru.yandex.market.logistics.management.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import ru.yandex.market.logistics.management.blackbox.BlackBoxProfile;
import ru.yandex.market.logistics.management.service.authentication.BlackBoxAuthentication;

public class WithBlackBoxAuthenticationSecurityContextFactory implements WithSecurityContextFactory<WithBlackBoxUser> {
    @Override
    public SecurityContext createSecurityContext(WithBlackBoxUser annotation) {
        Authentication authentication = new BlackBoxAuthentication(
            new BlackBoxProfile(annotation.login(), annotation.uid()),
            Arrays.stream(annotation.authorities()).collect(Collectors.toSet())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
