package ru.yandex.market.hc.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.hc.entity.Check;
import ru.yandex.market.hc.entity.DegradationModes;
import ru.yandex.market.hc.entity.KeyEntity;
import ru.yandex.market.hc.entity.KeyType;
import ru.yandex.market.hc.entity.Status;
import ru.yandex.market.hc.stubs.MemcachedClientStub;

import static org.junit.Assert.assertEquals;

/**
 * Created by aproskriakov on 9/9/21
 */
public class MemcachedServiceTest {

    private MemcachedClientIF memcachedClient;

    private MemcachedService memcachedService;

    private final String key = "test_Antifraud.vs.service.net:9000/antifraud/pumpkin";

    @Before
    public void setUp() {
        memcachedClient = new MemcachedClientStub();
        initService();
    }

    private void initService() {
        Map <KeyEntity, Integer> degradationModesMap = new ConcurrentHashMap<>();
        KeyEntity keyEntity = KeyEntity.builder()
                .name(key)
                .type(KeyType.FIXED)
                .build();
        degradationModesMap.put(keyEntity, 0);
        DegradationModes degradationModes = new DegradationModes(degradationModesMap);
        memcachedService = new MemcachedService(memcachedClient, new ObjectMapper(), degradationModes);
    }

    @Test
    public void testSaveState() throws IOException {
        Status status = Status.OK;
        Check check = Check.builder()
                .status(Status.OK)
                .serviceName(key)
                .build();
        memcachedService.checkAndSaveState(check);

        assertEquals(memcachedService.getStateEntry(key).getStatus(), status);
        assertEquals(memcachedService.getStateEntry(key).getDegradationMode(), 0);
    }

    @Test
    @Ignore
    public void testSetState_InRealMemcache() throws IOException {
        memcachedClient = new MemcachedClient(new InetSocketAddress("hc-cache.tst.vs.market.yandex.net", 11246));
        initService();
        Status status = Status.OK;
        Check check = Check.builder()
                .status(Status.OK)
                .serviceName(key)
                .build();
        memcachedService.checkAndSaveState(check);

        assertEquals(memcachedService.getStateEntry(key).getStatus(), status);
    }
}
