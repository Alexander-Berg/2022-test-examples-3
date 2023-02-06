package ru.yandex.market.api.util.parser2.typed.enums;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.CapiRequestAttribute;
import ru.yandex.market.api.server.RequestAttribute;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.parser2.ContentApiParserBuilders;
import ru.yandex.market.api.util.parser2.ParserBuilders;

public class EnumParserTestUtils extends UnitTestBase {
    public static final String PARAM_NAME = "param";
    public static ParserBuilders Parsers2 = new ContentApiParserBuilders();

    public static HttpServletRequest createRequest(String value) {
        return MockRequestBuilder.start()
                .attribute(CapiRequestAttribute.CONTEXT, ContextHolder.get())
                .attribute(RequestAttribute.CLIENT, ContextHolder.get().getClient())
                .param(PARAM_NAME, value)
                .build();
    }

    public static void setVersion(Version version) {
        Context ctx = new Context(UUID.randomUUID().toString());
        ctx.setVersion(version);
        ContextHolder.set(ctx);
    }

    public static void setClient(Client.Type clientType) {
        Context context = ContextHolder.get();
        Client client = new Client();
        client.setType(clientType);
        context.setClient(client);
        ContextHolder.set(context);
    }
}
