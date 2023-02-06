package ru.yandex.market.api.integration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Test;
import ru.yandex.market.api.common.client.UnknownMobileVersionInfo;
import ru.yandex.market.api.domain.v1.redirect.RedirectFilterV1;
import ru.yandex.market.api.domain.v1.redirect.RedirectV1;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.search.SearchType;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.service.RedirectService;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by apershukov on 12.12.16.
 */
public class RedirectServiceV1Test extends BaseTest {

    @Inject
    private RedirectService redirectService;

    @Inject
    private ReportTestClient reportClient;

    private void checkParams(Map<String, String> actual, Map<String, String> expected) {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String key = entry.getKey();
            assertTrue(actual.containsKey(key));

            String expectedVal = entry.getValue();
            String actualVal = actual.get(key);
            assertEquals(String.format("Expected '%s' is '%s' but was '%s'", key, expectedVal, actualVal),
                expectedVal, actualVal);
        }
    }

    private void checkFilters(List<RedirectFilterV1> actualList, List<RedirectFilterV1> expectedList) {
        assertEquals(expectedList.size(), actualList.size());
        for (int i = 0; i < expectedList.size(); i++) {
            RedirectFilterV1 expected = expectedList.get(i);
            String expectedId = expected.getId();
            String expectedValue = expected.getValue();

            RedirectFilterV1 actual = actualList.get(i);
            String actualId = actual.getId();
            String actualValue = actual.getValue();

            assertEquals(
                String.format("Unexpected id of filter[%d]: expected '%s', but was '%s'",
                    i, expectedId, actualId),
                expectedId, actualId);
            assertEquals(
                String.format("Unexpected value of filter[%d]: expected '%s', but was '%s'",
                    i, expectedValue, actualValue),
                expectedValue, actualValue);
        }
    }

    /**
     * Тестирование того что в случае если за редиректом обращается обычный партнер для визуальной категории возвращается
     * редирект на поиск
     */
    @Test
    public void testSearchRedirectForRegularClients() {
        String text = "красное платье";
        reportClient.redirect(text, "redirect_red_dress.json");
        context.setPpList(IntLists.EMPTY_LIST);

        Future<RedirectV1> redirectFuture = redirectService.redirect(
            new SearchQuery(text, SearchType.TEXT, null),
            GenericParams.DEFAULT);
        RedirectV1 redirect = Futures.waitAndGet(redirectFuture);

        assertEquals("search", redirect.getParams().getId());
        checkParams(redirect.getParams().getParams(),
            ImmutableMap.<String, String>builder()
                .put("hid", "7811901")
                .put("nid", "57297")
                .put("text", text)
                .put("link", "https://market.yandex.ru/search?nid=57297&hid=7811901")
                .build());
        checkFilters(redirect.getFilters(),
            ImmutableList.of(
                new RedirectFilterV1("-8", text),
                new RedirectFilterV1("7925349", "7925352")));
    }

    /**
     * Тестирование того что в случае если за редиректом обращается мобильное приложение для визуальной
     * категории возвращается редирект на фильтрацию
     */
    @Test
    public void testSearchRedirectForMobileClient() {
        String text = "красное платье";
        reportClient.redirect(text, "redirect_red_dress.json");

        Client client = new Client() {{
            setType(Type.MOBILE);
        }};

        ContextHolder.get().setClient(client);
        ContextHolder.get().setClientVersionInfo(UnknownMobileVersionInfo.INSTANCE);

        Future<RedirectV1> redirectFuture = redirectService.redirect(
            new SearchQuery(text, SearchType.TEXT, null),
            GenericParams.DEFAULT);
        RedirectV1 redirect = Futures.waitAndGet(redirectFuture);

        assertEquals("filter", redirect.getParams().getId());
    }

    @Test
    public void testRedirectForSearchResponse() {
        String text = "красное платjье ASOS";
        reportClient.redirect(text, "redirect_red_dress_asos.json");
        context.setPpList(IntLists.EMPTY_LIST);

        Future<RedirectV1> redirectFuture = redirectService.redirect(
            new SearchQuery(text, SearchType.TEXT, null),
            GenericParams.DEFAULT);
        RedirectV1 redirect = Futures.waitAndGet(redirectFuture);

        assertEquals("search", redirect.getParams().getId());
        checkParams(redirect.getParams().getParams(),
            ImmutableMap.<String, String>builder()
                .put("text", text)
                .put("link", "https://market.yandex.ru/search?" +
                    "text=%D0%BA%D1%80%D0%B0%D1%81%D0%BD%D0%BE%D0%B5+%D0%BF%D0%BB%D0%B0%D1%82%D1%8C%D0%B5+ASOS")
                .build());
        checkFilters(redirect.getFilters(),
            ImmutableList.of(new RedirectFilterV1("-8", "красное платье ASOS")));
    }

}
