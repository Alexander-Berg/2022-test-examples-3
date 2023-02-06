package ru.yandex.market.api.integration;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.server.sec.AuthorizationType;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.CommonClient;
import ru.yandex.market.api.server.sec.oauth.OAuthSecurityConfig;
import ru.yandex.market.api.server.sec.oauth.UserResolver;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.BlackBoxTestClient;

import static ru.yandex.market.api.util.ClientTestUtil.clientOfType;

/**
 * Created by tesseract on 09.03.17.
 */
@WithContext
@ActiveProfiles(BlackBoxServiceTest.PROFILE)
public class BlackBoxServiceTest extends BaseTest {
    static final String PROFILE = "BlackBoxServiceTest";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Bean
        @Primary
        public OAuthSecurityConfig localOAuthConfig() {
            Map<String, Collection<AuthorizationType>> types = Maps.newHashMap();
            types.put(CommonClient.Type.INTERNAL.name(), Collections.singleton(AuthorizationType.SessionId));
            return new OAuthSecurityConfig(types);
        }
    }


    @Inject
    BlackBoxTestClient blackBoxClient;
    @Inject
    UserResolver userResolver;

    @Test
    public void checkSetHostFromSessionId() {
        String host = "host";
        String sessionid = "sessionid";

        clientOfType(CommonClient.Type.INTERNAL, context);

        blackBoxClient.postUserBySessionId(host, sessionid, "blackBoxSessionId_id.xml");

        HttpServletRequest request = MockRequestBuilder.start()
            .header("X-User-Authorization", makeSessionId(host, sessionid))
            .build();

        Futures.waitAndGet(userResolver.getUser(request, context));
    }

    private String makeSessionId(String host, String sessionId) {
        return "SessionId Host=" + host + "; Id=" + sessionId;
    }

}
