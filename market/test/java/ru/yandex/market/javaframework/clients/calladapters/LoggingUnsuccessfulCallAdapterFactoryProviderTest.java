package ru.yandex.market.javaframework.clients.calladapters;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import retrofit2.CallAdapter;

import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientInfo;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientLoggingInfo;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientServiceProperties;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientsProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingUnsuccessfulCallAdapterFactoryProviderTest {

    @Test
    public void get() {
        String anyId = null;
        ClientsProperties anyProperties = null;
        Map<String, ClientServiceProperties> anyServiceProperties = null;
        LoggingUnsuccessfulCallAdapterFactoryProvider provider = new LoggingUnsuccessfulCallAdapterFactoryProvider();

        CallAdapter.Factory factory = provider.get(anyId, anyProperties, anyServiceProperties);

        Assertions.assertNotNull(factory);
    }

    @Test
    public void isAcceptable_ifLogUnsuccessfulIsFalseThenFalse() {
        String clientId = "clientId";
        ClientsProperties properties = properties(clientId, clientInfo(false));
        Map<String, ClientServiceProperties> anyServiceProperties = null;
        LoggingUnsuccessfulCallAdapterFactoryProvider provider = new LoggingUnsuccessfulCallAdapterFactoryProvider();

        boolean isAcceptable = provider.isAcceptable(clientId, properties, anyServiceProperties);

        assertFalse(isAcceptable);
    }

    private ClientsProperties properties(String clientId, ClientInfo clientInfo) {
        Map<String, ClientInfo> list = new HashMap<>();
        list.put(clientId, clientInfo);

        ClientsProperties properties = new ClientsProperties();
        properties.setList(list);
        return properties;
    }

    private ClientInfo clientInfo(boolean isLogUnsuccessful) {
        ClientLoggingInfo loggingInfo = new ClientLoggingInfo();
        loggingInfo.setLogUnsuccessful(isLogUnsuccessful);
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setLogging(loggingInfo);
        return clientInfo;
    }

    @Test
    public void isAcceptable_ifLogUnsuccessfulIsTrueThenTrue() {
        String clientId = "clientId";
        ClientsProperties properties = properties(clientId, clientInfo(true));
        Map<String, ClientServiceProperties> anyServiceProperties = null;
        LoggingUnsuccessfulCallAdapterFactoryProvider provider = new LoggingUnsuccessfulCallAdapterFactoryProvider();

        boolean isAcceptable = provider.isAcceptable(clientId, properties, anyServiceProperties);

        assertTrue(isAcceptable);
    }

    @Test
    public void isAcceptable_ifNoClientInfoThenTrue() {
        String clientId = "clientId";
        ClientsProperties properties = properties(clientId, null);
        Map<String, ClientServiceProperties> anyServiceProperties = null;
        LoggingUnsuccessfulCallAdapterFactoryProvider provider = new LoggingUnsuccessfulCallAdapterFactoryProvider();

        boolean isAcceptable = provider.isAcceptable(clientId, properties, anyServiceProperties);

        assertTrue(isAcceptable);
    }

}
