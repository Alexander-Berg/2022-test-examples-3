package ru.yandex.market.api.server.sec.client.strategy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

/**
 * Created by tesseract on 11.07.17.
 */
@WithContext
public class ClientStrategyRegisterTest extends UnitTestBase {

    ClientStrategyRegister registry;

    @Test
    public void checkStrategy_External() {
        // настройка системы
        prepareContext(Client.Type.EXTERNAL);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(DefaultClientStrategy.INSTANCE == strategy);
    }

    @Test
    public void checkStrategy_Interal() {
        // настройка системы
        prepareContext(Client.Type.INTERNAL);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(DefaultClientStrategy.INSTANCE == strategy);
    }

    @Test
    public void checkStrategy_Mobile() {
        // настройка системы
        prepareContext(Client.Type.MOBILE);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(DefaultClientStrategy.INSTANCE == strategy);
    }

    @Test
    public void checkStrategy_Vendor() {
        // настройка системы
        prepareContext(Client.Type.VENDOR);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(VendorClientStrategy.INSTANCE == strategy);
    }

    @Test
    public void checkStrategy_non_context() {
        // настройка системы
        ContextHolder.set(null);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(DefaultClientStrategy.INSTANCE == strategy);
    }

    @Test
    public void checkStrategy_non_client() {
        // настройка системы
        Context ctx = new Context("123");
        ContextHolder.set(ctx);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(DefaultClientStrategy.INSTANCE == strategy);
    }

    @Test
    public void checkStrategy_non_clientType() {
        // настройка системы
        Context ctx = new Context("123");
        ctx.setClient(new Client());
        ContextHolder.set(ctx);
        // вызов системы
        ClientStrategy strategy = registry.get();
        // проверка утверждений
        Assert.assertTrue(DefaultClientStrategy.INSTANCE == strategy);
    }

    @Before
    public void setUp() {
        registry = new ClientStrategyRegister();
    }

    private void prepareContext(Client.Type type) {
        Client client = new Client();
        client.setType(type);

        ContextHolder.update(context -> context.setClient(client));
    }


}
