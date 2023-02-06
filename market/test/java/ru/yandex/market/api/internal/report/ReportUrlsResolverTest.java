package ru.yandex.market.api.internal.report;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

/**
 * @authror dimkarp93
 */
@WithMocks
public class ReportUrlsResolverTest extends UnitTestBase {
    @Mock
    private BlueRule blueRule;

    @Mock
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    private ReportUrlsResolver reportUrlsResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.mockClientHelper = new MockClientHelper(clientHelper);
        this.reportUrlsResolver = new ReportUrlsResolver(blueRule, clientHelper);
    }

    @Test
    public void nullContext() {
        Collection<ReportUrls> urls = reportUrlsResolver.resolve(null).getUrls();

        Assert.assertNotNull(urls);
        Assert.assertEquals(0, urls.size());
    }

    @Test
    public void blue() {
        Mockito.when(blueRule.test()).thenReturn(true);
        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(true);

        Collection<ReportUrls> urls = reportUrlsResolver.resolve(new Context("abcd")).getUrls();
        Assert.assertNotNull(urls);
        Assert.assertThat(
                urls,
                Matchers.containsInAnyOrder(
                    Matchers.is(ReportUrls.CPA),
                    Matchers.is(ReportUrls.PROMOTION)
                )
        );
    }

    @Test
    public void external() {
        Mockito.when(blueRule.test()).thenReturn(false);
        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(false);

        Context ctx = new Context("abcd");
        Client client = new Client();
        client.setType(Client.Type.EXTERNAL);
        ctx.setClient(client);

        Collection<ReportUrls> urls = reportUrlsResolver.resolve(ctx).getUrls();

        Assert.assertNotNull(urls);
        Assert.assertThat(
                urls,
                Matchers.containsInAnyOrder(
                        Matchers.is(ReportUrls.EXTERNAL),
                        Matchers.is(ReportUrls.GEO),
                        Matchers.is(ReportUrls.CPA),
                        Matchers.is(ReportUrls.FAST_ORDER),
                        Matchers.is(ReportUrls.GEO_SHIPPING),
                        Matchers.is(ReportUrls.PRODUCT_VENDOR_BID)
                )
        );
    }

    @Test
    public void widgets() {
        Mockito.when(blueRule.test()).thenReturn(false);
        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(false);

        Context ctx = new Context("abcd");
        Client client = new Client();
        client.setType(Client.Type.EXTERNAL);
        ctx.setClient(client);

        mockClientHelper.is(ClientHelper.Type.WIDGET, true);

        Collection<ReportUrls> urls = reportUrlsResolver.resolve(ctx).getUrls();

        Assert.assertNotNull(urls);
        Assert.assertThat(
                urls,
                Matchers.containsInAnyOrder(
                        Matchers.is(ReportUrls.EXTERNAL),
                        Matchers.is(ReportUrls.GEO),
                        Matchers.is(ReportUrls.PHONE),
                        Matchers.is(ReportUrls.CPA),
                        Matchers.is(ReportUrls.FAST_ORDER),
                        Matchers.is(ReportUrls.GEO_SHIPPING),
                        Matchers.is(ReportUrls.PRODUCT_VENDOR_BID),
                        Matchers.is(ReportUrls.TURBO)
                )
        );
    }

    @Test
    public void other() {
        Mockito.when(blueRule.test()).thenReturn(false);
        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(false);

        Context ctx = new Context("abcd");
        Client client = new Client();
        client.setType(Client.Type.INTERNAL);
        ctx.setClient(client);

        Collection<ReportUrls> urls = reportUrlsResolver.resolve(ctx).getUrls();

        Assert.assertNotNull(urls);
        Assert.assertThat(
                urls,
                Matchers.containsInAnyOrder(
                        Matchers.is(ReportUrls.EXTERNAL),
                        Matchers.is(ReportUrls.GEO),
                        Matchers.is(ReportUrls.PHONE),
                        Matchers.is(ReportUrls.CPA),
                        Matchers.is(ReportUrls.FAST_ORDER),
                        Matchers.is(ReportUrls.GEO_SHIPPING),
                        Matchers.is(ReportUrls.PRODUCT_VENDOR_BID)
                )
        );
    }
}
