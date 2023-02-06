package ru.yandex.market.api.util;

import ru.yandex.market.api.server.sec.OAuthToken;
import ru.yandex.market.api.server.sec.SessionIdToken;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dimkarp93
 */
public class AuthorizatoinTokenTestUtil {
    public static void assertEqualsOAuthTokens(OAuthToken o1, OAuthToken o2) {
        if (null == o1 || null == o2) {
            assertTrue("Both OAuthTokens must be null or not null", null == o1 && null == o2);
            return;
        }

        assertEquals("OAuthToken must have same tokens", o1.getOAuthToken(), o2.getOAuthToken());
    }

    public static void assertEqualsSessionIds(SessionIdToken o1, SessionIdToken o2) {
        if (null == o1 || null == o2) {
            assertTrue("Both SessionId must be null or not null", null == o1 && null == o2);
            return;
        }

        assertEquals("SessionId must have same host", o1.getHost(), o2.getHost());
        assertEquals("SessionId must have same sessionId", o1.getSessionId(), o2.getSessionId());
    }
}
