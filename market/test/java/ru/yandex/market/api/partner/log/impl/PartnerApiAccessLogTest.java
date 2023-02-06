package ru.yandex.market.api.partner.log.impl;

import java.util.stream.Stream;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.request.PartnerServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link PartnerApiAccessLog}.
 *
 * @author Vadim Lyalin
 */
public class PartnerApiAccessLogTest extends FunctionalTest {
    private static final String REQUEST_URI = "/campaign/123/offer";

    @Autowired
    private PartnerApiAccessLog partnerApiAccessLog;

    /**
     * Проверяет корректность формирования урла на моках.
     */
    @ParameterizedTest
    @MethodSource("testArgs")
    void testUrl(String queryString, String expectedUrl) {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        PartnerServletRequest partnerServletRequest = mock(PartnerServletRequest.class);

        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getQueryString()).thenReturn(queryString);
        when(request.getAttribute(PartnerServletRequest.class.getName())).thenReturn(partnerServletRequest);

        String logMessage = partnerApiAccessLog.buildLogMessage(request, response);
        String[] msgTokens = logMessage.split("\t");
        String url = msgTokens[3];
        assertEquals(expectedUrl, url);
    }

    static Stream<Arguments> testArgs() {
        return Stream.of(
                Arguments.of("offerId=123qwe", REQUEST_URI + "?offerId=123qwe"),
                Arguments.of("", REQUEST_URI),
                Arguments.of(null, REQUEST_URI),
                Arguments.of("/v2/campaigns/21532469/hidden-offers.json?oauth_token=AgAAAAAHYVLuAAX_m477zavGV0GMlcItT-kpRm1&oauth_client_id=0aut4c11ent&oauth_login=just-sales3",
                        REQUEST_URI + "?/v2/campaigns/21532469/hidden-offers.json?oauth_token=AgAAAAAHYVLuAAX_m4XXXXXXXXXXXXXXXXXXXXX&oauth_client_id=0aut4c11ent&oauth_login=just-sales3")
                );
    }
}
