package ru.yandex.market.api.integration;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.internal.common.PP;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.server.RequestMode;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;

/**
 * Created by vdorogin on 18.05.17.
 */
public class BaseTestContext {
    private static final AtomicInteger id = new AtomicInteger(100000);
    public static final int REGION_ID = 213;

    public static Context newContext() {
        Context context = new Context("trid" + id.incrementAndGet());

        context.getRegionInfo().setRawRegionId(REGION_ID);
        context.setCurrency(Currency.RUR);
        context.setRequestMode(RequestMode.DEBUG);
        context.setSections(Sets.newHashSet());
        context.setVersion(Version.V2_0_1);
        context.setVersionPathPart("v2");
        context.setUrlSchema(UrlSchema.HTTPS);
        context.setRequest(new MockHttpServletRequest());
        context.setPpList(IntLists.singleton(PP.DEFAULT));

        ContextHolder.set(context);
        return context;
    }
}
