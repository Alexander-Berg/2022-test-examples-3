package ru.yandex.market.marketpromo.test.client;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.yandex.market.marketpromo.core.application.security.MBOCAuthenticationRequest;
import ru.yandex.market.marketpromo.security.MBOCAuthenticationFilter;

import javax.annotation.Nonnull;

public class AuthForRequests {

    @Nonnull
    public static MockHttpServletRequestBuilder addAuthHeaders(
            @Nonnull MockHttpServletRequestBuilder requestBuilder,
            @Nonnull MBOCAuthenticationRequest authenticationRequest
    ) {
        if (authenticationRequest.getLogin() != null) {
            requestBuilder.header(MBOCAuthenticationFilter.USER_LOGIN_HEADER, authenticationRequest.getLogin());
        }
        if (!authenticationRequest.getRoles().isEmpty()) {
            requestBuilder.header(
                    MBOCAuthenticationFilter.USER_ROLES_HEADER, authenticationRequest.getRoles().toArray());
        }
        return requestBuilder;
    }
}
