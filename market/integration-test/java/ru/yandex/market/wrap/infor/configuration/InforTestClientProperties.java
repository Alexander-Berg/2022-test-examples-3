package ru.yandex.market.wrap.infor.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InforTestClientProperties {

    @Value("${fulfillment.wrap.infor.wms.client.enterpriseKey}")
    private String enterpriseKey;
    @Value("${fulfillment.wrap.infor.wms.client.warehouseKey}")
    private String warehouseKey;
    @Value("${fulfillment.wrap.infor.wms.client.readTimeoutMillis}")
    private int readTimeoutMillis;
    @Value("${fulfillment.wrap.infor.wms.client.connectTimeoutMillis}")
    private int connectTimeoutMillis;
    @Value("${fulfillment.wrap.infor.wms.client.username}")
    private String username;
    @Value("${fulfillment.wrap.infor.wms.client.password}")
    private String password;
    @Value("${fulfillment.wrap.infor.wms.client.url}")
    private String url;

    public static InforTestClientProperties of(String enterpriseKey, String warehouseKey, int readTimeoutMillis,
                                               int connectTimeoutMillis, String username, String password, String url) {
        InforTestClientProperties properties = new InforTestClientProperties();
        properties.enterpriseKey = enterpriseKey;
        properties.warehouseKey = warehouseKey;
        properties.readTimeoutMillis = readTimeoutMillis;
        properties.connectTimeoutMillis = connectTimeoutMillis;
        properties.username = username;
        properties.password = password;
        properties.url = url;
        return properties;
    }

    public String getEnterpriseKey() {
        return enterpriseKey;
    }

    public String getWarehouseKey() {
        return warehouseKey;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }
}
