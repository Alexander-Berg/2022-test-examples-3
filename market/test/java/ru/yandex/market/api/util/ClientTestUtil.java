package ru.yandex.market.api.util;

import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.CommonClient;

public class ClientTestUtil {
    public static void clientOfType(CommonClient.Type type) {
        ContextHolder.update(ctx -> clientOfType(type, ctx));
    }

    public static void clientOfType(CommonClient.Type type, Context context) {
        context.setClient(new Client() {{
            setType(type);
            setId(type.name());
        }});
    }
}
