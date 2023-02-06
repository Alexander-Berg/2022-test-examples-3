package ru.yandex.market.api.domain;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.yandex.market.api.ContextHolderTestHelper;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.server.ApplicationContextHolder;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.Environment;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ResultContextProviderTest extends BaseTest {
    @Inject
    ResultContextProvider resultContextProvider;

    @Before
    public void setUp() throws Exception {
        Context context = new Context("testContentApiRequestId");
        context.setMarketRequestId("testMarketRequestId");
        context.setVersion(Version.V2_0_0);
        ContextHolderTestHelper.initContext(context);
    }

    @Test
    public void shouldReturnMarketRequestIdForPrestable() {
        ApplicationContextHolder.setEnvironment(Environment.PRESTABLE);

        ResultContextV2 header = (ResultContextV2) resultContextProvider.getHeader();

        assertEquals("testMarketRequestId", header.getId());
    }

    @Test
    public void shouldReturnMarketRequestIdForProduction() {
        ApplicationContextHolder.setEnvironment(Environment.PRODUCTION);

        ResultContextV2 header = (ResultContextV2) resultContextProvider.getHeader();

        assertEquals("testMarketRequestId", header.getId());
    }

    @Test
    public void shouldReturnContentApiRequestIdForTesting() {
        ApplicationContextHolder.setEnvironment(Environment.DEVELOPMENT);

        ResultContextV2 header = (ResultContextV2) resultContextProvider.getHeader();

        assertEquals("testContentApiRequestId", header.getId());
    }
}
