package ru.yandex.market.application.properties.etcd.grpc;

import org.junit.Assert;
import org.junit.Test;
/**
 * @author s-ermakov
 */
public class EtcdClientImplConfigTest {

    @Test
    public void getAuthorityIfOnlyEndpointsAreSet() {
        EtcdClientImplConfig config = new EtcdClientImplConfig();
        config.setEndpoints("https://etcd01ht.market.yandex.net:3379", "https://etcd02ht.market.yandex.net:3379");

        Assert.assertEquals("etcd01ht.market.yandex.net", config.getAuthority());
    }

    @Test
    public void getAuthorityIfEndpointsAndAuthorityIsSet() {
        EtcdClientImplConfig config = new EtcdClientImplConfig();
        config.setEndpoints("https://etcd01ht.market.yandex.net:3379", "https://etcd02ht.market.yandex.net:3379");
        config.setAuthority("authority.yandex.net");

        Assert.assertEquals("authority.yandex.net", config.getAuthority());
    }
}
