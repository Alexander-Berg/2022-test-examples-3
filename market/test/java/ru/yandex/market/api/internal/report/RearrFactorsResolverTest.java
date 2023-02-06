package ru.yandex.market.api.internal.report;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

import static ru.yandex.market.api.server.sec.client.CommonClient.Type.INTERNAL;

/**
 * @author dimkarp93
 */
@WithMocks
public class RearrFactorsResolverTest extends BaseTest {
    @InjectMocks
    private RearrFactorsResolver resolver;

    @Mock
    private ClientHelper clientHelper;

    private MockClientHelper helper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.helper = new MockClientHelper(clientHelper);
    }

    @Test
    public void nullGenericParams() {
        GenericParams genericParams = new GenericParamsBuilder()
                .setRearrFactors(null)
                .build();
        helper.rearr(null);
        helper.is(ClientHelper.Type.MAIL_RU, true);

        Assert.assertEquals("market_cpa_only_enabled=0", resolver.resolve(genericParams, new Client()));
    }

    @Test
    public void notExternalClient() {
        GenericParams genericParams = new GenericParamsBuilder()
                .setRearrFactors("")
                .build();
        helper.rearr(null);
        helper.is(ClientHelper.Type.MAIL_RU, true);
        Client client = new Client();
        client.setType(INTERNAL);
        Assert.assertEquals("", resolver.resolve(genericParams, client));
    }

    @Test
    public void emptyGenericParamsAndClient() {
        GenericParams genericParams = new GenericParamsBuilder()
                .setRearrFactors("")
                .build();
        helper.rearr(null);
        helper.is(ClientHelper.Type.MAIL_RU, true);
        Assert.assertEquals("market_cpa_only_enabled=0", resolver.resolve(genericParams, new Client()));
    }

    @Test
    public void nullGenericParamsButClientNotNull() {
        helper.rearr("market_use_knn=0;test=1");
        helper.is(ClientHelper.Type.MAIL_RU, true);

        String rearr = resolver.resolve(null, new Client());

        Assert.assertEquals("market_use_knn=0;test=1;market_cpa_only_enabled=0", rearr);
    }

    @Test
    public void notNullGenericParamsButClientNull() {
        GenericParams genericParams = new GenericParamsBuilder()
                .setRearrFactors("a=1;b=2;c=3")
                .build();
        helper.rearr(null);
        String rearr = resolver.resolve(genericParams, null);
        Assert.assertEquals("a=1;b=2;c=3", rearr);
    }

    @Test
    public void notNullGenericParamsAndClient() {
        helper.rearr("market_use_knn=0;test=1");
        helper.is(ClientHelper.Type.MAIL_RU, true);

        GenericParams genericParams = new GenericParamsBuilder()
                .setRearrFactors("a=1;b=2;c=3")
                .build();
        String rearr = resolver.resolve(genericParams, new Client());
        Assert.assertEquals("a=1;b=2;c=3;market_use_knn=0;test=1;market_cpa_only_enabled=0", rearr);
    }

}
