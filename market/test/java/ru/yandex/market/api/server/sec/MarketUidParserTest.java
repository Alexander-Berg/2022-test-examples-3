package ru.yandex.market.api.server.sec;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.user.order.MarketUid;
import ru.yandex.market.api.user.order.MarketUidEncodingService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
public class MarketUidParserTest extends UnitTestBase {

    @InjectMocks
    private Parameters.MarketUidParser parser;

    @Mock
    private MarketUidEncodingService uidEncodingService;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        when(uidEncodingService.decode(anyString())).thenCallRealMethod();
    }

    @Test
    public void shouldParseOldMuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Market-Uid", "1152921504607770000");

        MarketUid marketUid = parser.get(request);

        assertEquals(1152921504607770000L, (long) marketUid.getMuid());
        assertEquals(null, marketUid.getSignature());
    }
}
