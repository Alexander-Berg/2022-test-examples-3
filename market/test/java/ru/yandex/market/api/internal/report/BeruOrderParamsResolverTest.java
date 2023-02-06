package ru.yandex.market.api.internal.report;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.PartnerInfo;
import ru.yandex.market.api.server.context.Context;

/**
 * @author dimkarp93
 */
public class BeruOrderParamsResolverTest extends UnitTestBase {
    private BeruOrderParamsResolver resolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.resolver = new BeruOrderParamsResolver();
    }

    @Test
    public void nullContext() {
        Assert.assertNull(resolver.resolve(null));
    }

    @Test
    public void partnerParamsWithoutCustom() {
        Context ctx = new Context("1");
        ctx.setPpList(IntLists.singleton(23));

        ctx.setPartnerInfo(createPartnerInfo());

        Assert.assertEquals("pp=23&clid=clid-123&mclid=456&distr_type=7", resolver.resolve(ctx));
    }

    @Test
    public void partnerParamsWithCustom() {
        Context ctx = new Context("1");
        ctx.setPpList(IntLists.singleton(23));

        ctx.setPartnerInfo(createPartnerInfo());
        ctx.setGenericParams(createGenericParams());

        Assert.assertEquals("pp=23&clid=clid-123&mclid=456&distr_type=7&purchase-referrer=widgets", resolver.resolve(ctx));
    }

    @Test
    public void onlyCustomParams() {
        Context ctx = new Context("1");
        ctx.setGenericParams(createGenericParams());

        Assert.assertEquals("purchase-referrer=widgets", resolver.resolve(ctx));
    }

    private PartnerInfo createPartnerInfo() {
        return PartnerInfo.create("clid-123", null, 456L,  7L,null);
    }

    private GenericParams createGenericParams() {
        return new GenericParamsBuilder()
                .setBeruOrderParamsCustom("purchase-referrer=widgets")
                .build();
    }

}
