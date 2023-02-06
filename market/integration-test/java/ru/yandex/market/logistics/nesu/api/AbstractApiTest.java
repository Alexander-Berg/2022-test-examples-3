package ru.yandex.market.logistics.nesu.api;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;

@ParametersAreNonnullByDefault
public class AbstractApiTest extends AbstractContextualTest {

    @Autowired
    protected BlackboxService blackboxService;

    protected ApiAuthHolder authHolder;

    @BeforeEach
    void setupBlackbox() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
    }

    protected HttpHeaders authHeaders() {
        return authHolder.authHeaders();
    }

    @Override
    @Nonnull
    protected MockHttpServletRequestBuilder request(HttpMethod method, String url, Object body) throws Exception {
        return super.request(method, url, body)
            .headers(authHeaders());
    }
}
