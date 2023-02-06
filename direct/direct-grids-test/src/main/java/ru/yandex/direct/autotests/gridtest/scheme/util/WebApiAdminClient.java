package ru.yandex.direct.autotests.gridtest.scheme.util;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import ru.yandex.autotests.irt.jersey.logging.ClientLoggingFilter;

public class WebApiAdminClient {
    private String host;

    private Gson gson = new Gson();

    public WebApiAdminClient(String host) {
        this.host = host;
    }

    public JsonObject getSchema() {
        return gson.fromJson(getBaseTarget()
                        .queryParam("action", "graphql_schema")
                        .queryParam("schema", "grid")
                        .request(MediaType.TEXT_PLAIN)
                        .method(HttpMethod.GET, null, String.class),
                JsonObject.class);
    }

    public JsonObject getVersion() {
        return gson.fromJson(getBaseTarget()
                        .queryParam("action", "version")
                        .request(MediaType.TEXT_PLAIN)
                        .method(HttpMethod.GET, null, String.class),
                JsonObject.class);
    }

    private WebTarget getBaseTarget() {
        return ClientBuilder
                .newClient(new ClientConfig()
                        .property(ApacheClientProperties.CONNECTION_MANAGER,
                                new PoolingHttpClientConnectionManager())
                        .property(ClientProperties.CONNECT_TIMEOUT, 15000)
                        .property(ClientProperties.READ_TIMEOUT, 15000)
                        .property(ClientProperties.READ_TIMEOUT, 15000)
                        .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true))
                .register(new ClientLoggingFilter("GraphQLClient"))
                .target(host)
                .path("web-api")
                .path("admin");
    }
}