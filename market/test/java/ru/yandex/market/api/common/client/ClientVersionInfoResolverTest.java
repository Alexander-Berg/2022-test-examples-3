package ru.yandex.market.api.common.client;

import org.apache.http.HttpHeaders;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.sec.client.Client;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ClientVersionInfoResolverTest extends BaseTest {

    @Test
    public void shouldParseVersion() {
        assertEquals(new SemanticVersion(2, 5, 1), ClientVersionInfoResolver.parse("251"));
        assertEquals(new SemanticVersion(2, 5, 1), ClientVersionInfoResolver.parse("2.51"));
        assertEquals(new SemanticVersion(2, 5, 1), ClientVersionInfoResolver.parse("2.5.1"));
        assertEquals(new SemanticVersion(2, 4, 0), ClientVersionInfoResolver.parse("24"));
        assertEquals(new SemanticVersion(2, 4, 0), ClientVersionInfoResolver.parse("2.4"));
        assertEquals(new SemanticVersion(2, 0, 0), ClientVersionInfoResolver.parse("2"));
    }
}
