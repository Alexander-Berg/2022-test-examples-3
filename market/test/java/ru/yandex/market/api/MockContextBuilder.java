package ru.yandex.market.api;

import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.server.RegionInfo;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MockContextBuilder {

    private Client client;
    private ValidationErrors errors = mock(ValidationErrors.class);
    private String userIp;
    private GenericParams genericParams;

    public static MockContextBuilder start() {
        return new MockContextBuilder();
    }

    public MockContextBuilder mobileClient() {
        client = mock(Client.class);
        when(client.ofType(eq(Client.Type.MOBILE))).thenReturn(true);
        return this;
    }

    public MockContextBuilder userIp(String ip) {
        this.userIp = ip;
        return this;
    }

    public MockContextBuilder externalClient() {
        client = mock(Client.class);
        when(client.ofType(Client.Type.MOBILE)).thenReturn(false);
        return this;
    }

    public Context build() {
        Context context = mock(Context.class);
        when(context.getClient()).thenReturn(client);
        when(context.getValidationErrors()).thenReturn(errors);
        when(context.getUserIp()).thenReturn(userIp);
        when(context.getRegionInfo()).thenReturn(mock(RegionInfo.class));
        when(context.getGenericParams()).thenReturn(genericParams);

        ContextHolder.set(context);
        return context;
    }
}
