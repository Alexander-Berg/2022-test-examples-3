package ru.yandex.market.javaframework.tvm;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.javaframework.tvm.configurers.tvm.ClientsTvmConfigurer;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.ServiceYaml;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientInfo;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientServiceProperties;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientsProperties;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ProxyDefaults;
import ru.yandex.market.starter.properties.tvm.TvmProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientsTvmConfigurerTest {

    @Test
    public void clientsTvmDestinationTest() {
        final int expectedId = 342;
        final String clientId = "test-client";
        final String clientApiRef = "market/test/" + clientId + "/src/main/resources/openapi/api/api.yaml";

        final TvmProperties tvmProperties = new TvmProperties();
        tvmProperties.setId(expectedId);

        final ServiceYaml serviceYaml = new ServiceYaml();
        serviceYaml.setTvm(tvmProperties);

        final ClientServiceProperties clientServiceProperties = new ClientServiceProperties();
        clientServiceProperties.setServiceYaml(serviceYaml);

        final ClientInfo clientInfo = new ClientInfo();
        clientInfo.setOpenapiSpecPath(clientApiRef);
        final ClientsProperties clientsProperties = new ClientsProperties();
        clientsProperties.setList(Map.of(clientId, clientInfo));

        final ClientsTvmConfigurer configurer =
            new ClientsTvmConfigurer(Map.of(clientId, clientServiceProperties), clientsProperties);

        assertEquals(Set.of(expectedId), configurer.getDestinations());
    }

    @Test
    void shouldAddProxyDestIdToDestinations() {
        final String clientId = "test-client";
        int dstTvmId = 342;
        int proxyTvmId = 11234;

        TvmProperties tvm = new TvmProperties();
        tvm.setId(dstTvmId);

        ServiceYaml serviceYaml = new ServiceYaml();
        serviceYaml.setTvm(tvm);

        final ClientServiceProperties clientServiceProperties = new ClientServiceProperties();
        clientServiceProperties.setServiceYaml(serviceYaml);

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.getProxy().setEnabled(true);
        clientInfo.getProxy().setType(ProxyDefaults.ZORA);
        clientInfo.getProxy().getProxySettings().getRealm().setTvmId(proxyTvmId);
        clientInfo.getProxy().getProxySettings().getRealm().setPrincipal("market-carrier-driver");

        ClientsProperties clientsProperties = new ClientsProperties();
        clientsProperties.setList(Map.of(clientId, clientInfo));

        ClientsTvmConfigurer configurer = new ClientsTvmConfigurer(
                Map.of(clientId, clientServiceProperties), clientsProperties
        );

        assertEquals(
                Set.of(proxyTvmId, dstTvmId),
                configurer.getDestinations()
        );
    }
}
