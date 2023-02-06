package ru.yandex.market.api.integration;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongLists;
import org.junit.Test;

import ru.yandex.market.api.controller.v2.SearchControllerV2;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.report.CommonReportOptions.ResultType;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * Created by fettsery on 29.11.18.
 */
public class PassPPListTest extends BaseTest {

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private SearchControllerV2 controller;

    @Test
    public void passPpListTest() {
        ContextHolder.update(ctx -> {
            ctx.setPpList(new IntArrayList(Arrays.asList(100, 200)));
        });

        reportTestClient.doRequest("prime",
                x -> x.param("pp", "100")
                        .param("pp-list", "100,200")
        ).ok().body("report_prime_pp_list.json");

        controller.search(LongLists.EMPTY_LIST, true, null, true, true,
                Collections.emptySet(), SearchQuery.text("abc"), Collections.emptyList(),
                100, 200, PageInfo.DEFAULT,
                null, true, ResultType.ALL,
                Collections.emptyMap(), GenericParams.DEFAULT, 0L, true,null
        ).waitResult();
    }

}
