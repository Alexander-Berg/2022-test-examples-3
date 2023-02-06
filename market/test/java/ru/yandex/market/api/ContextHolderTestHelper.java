package ru.yandex.market.api;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.server.*;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.Version;

/**
 * Используется для инициализации контекста
 */
public class ContextHolderTestHelper {

    /**
     * Если надо просто иметь контекст
     */
    public static void initContext() {
        initContext(Version.V1_0_0);
    }

    /**
     * Инициализировать контекст с использованием конкретной версии api
     *
     * @param version версия
     */
    public static void initContextWithApiVersion(Version version) {
        initContext(version);
        ApplicationContextHolder.setEnvironment(Environment.LOCAL);
    }

    /**
     * Если с контекстом связано тестирование. Рекомендуется передавать mock объект
     * @param context объект контекста
     */
    public static void initContext(Context context) {
        ContextHolder.set(context);
    }

    public static void destroyContext() {
        ContextHolder.reset();
    }

    private static void initContext(Version version) {
        ContextHolder.set(new Context("Test-request-id")
        {{
            setUrlSchema(UrlSchema.HTTP);
            setCurrency(Currency.RUR);

            Client client = new Client();
            client.setType(Client.Type.EXTERNAL);
            setClient(client);

            setRequestMode(RequestMode.STANDART);

            setVersion(version);
            if (Version.V1_0_0.equals(version)) {
                setVersionPathPart("v1");
            } else {
                setVersionPathPart("v2");
            }
        }});
    }
}
