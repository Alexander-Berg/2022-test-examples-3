package ru.yandex.market.api.redirect;

import java.util.HashMap;

import javax.inject.Inject;

import io.netty.util.concurrent.Promise;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.category.CategoryService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.v1.redirect.Params;
import ru.yandex.market.api.domain.v1.redirect.RedirectV1;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.report.ReportClient;
import ru.yandex.market.api.search.SearchType;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.service.RedirectServiceV1Impl;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
@WithMocks
public class RedirectServiceV1ImplTest extends BaseTest {

    @Mock
    ReportClient reportClient;

    @Inject
    CategoryService categoryService;

    @Inject
    MarketUrls marketUrls;

    @Inject
    UrlParamsFactoryImpl urlParamsFactoryImpl;

    RedirectServiceV1Impl redirectService;

    @Before
    public void setUp() {

        Promise<RedirectV1> redirectPromise = Futures.newPromise();

        RedirectV1 reportServiceRedirect = new RedirectV1();
        reportServiceRedirect.setParams(new Params());
        reportServiceRedirect.getParams().setId("brands");
        reportServiceRedirect.getParams().setParams(new HashMap<String, String>() {{
            put("vendor_id", "5");
        }});

        redirectPromise.trySuccess(reportServiceRedirect);

        when(reportClient.getRedirectV1(any(SearchQuery.class), any(GenericParams.class)))
            .thenReturn(redirectPromise);

        redirectService = new RedirectServiceV1Impl(
                reportClient,
                categoryService,
                marketUrls,
                urlParamsFactoryImpl
        );
    }

    /**
     * Проверяем, что для редиеркта на страницу вендора (id="brands") в случае отсутствия текста в параметрах
     * туда добавляется текст запроса.
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-1590">MARKETAPI-1590</a>
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-1958">MARKETAPI-1958</a>
     */
    @Test
    public void shouldReturnRedirectWithTextForBrands() {
        ContextHolder.get().getRegionInfo().setRawRegionId(213);

        RedirectV1 result = Futures.waitAndGet(redirectService.redirect(
            new SearchQuery("Some text", SearchType.TEXT, null), GenericParams.DEFAULT));

        assertEquals("Some text", result.getParams().getParams().get("text"));
    }

}
