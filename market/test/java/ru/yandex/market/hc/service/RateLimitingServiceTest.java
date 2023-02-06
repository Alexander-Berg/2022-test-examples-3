package ru.yandex.market.hc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.hc.entity.DegradationModes;
import ru.yandex.market.hc.entity.KeyEntity;
import ru.yandex.market.hc.entity.KeyType;

import static org.junit.Assert.assertEquals;

/**
 * Created by aproskriakov on 9/9/21
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitingServiceTest {

    @Test
    public void testTenPercentToPumpkin() {
        testPercentage(10, 10, 1, 9);
        testPercentage(10, 20, 2, 18);
        testPercentage(10, 30, 3, 27);
    }

    @Test
    public void testFiftyPercentToPumpkin() {
        testPercentage(50, 10, 5, 5);
        testPercentage(50, 20, 10, 10);
        testPercentage(50, 30, 15, 15);
    }

    @Test
    public void testNinetyPercentToPumpkin() {
        testPercentage(90, 10, 9, 1);
        testPercentage(90, 20, 18, 2);
        testPercentage(90, 30, 27, 3);
    }

    @Test
    public void testNinetyPercentToPumpkinWithRegexpMatch() {
        String dmKey = "someaddres:8080/path/\\d+/path";
        String hostAndEndpoint = "someaddres:8080/path/190283/path";
        testPercentage(dmKey, KeyType.PATTERN, hostAndEndpoint,90, 10, 9, 1);
        testPercentage(dmKey, KeyType.PATTERN, hostAndEndpoint,90, 20, 18, 2);
        testPercentage(dmKey, KeyType.PATTERN, hostAndEndpoint,90, 30, 27, 3);
    }

    @Test
    public void testNinetyPercentToPumpkinWithRegexpNotMatch() {
        String dmKey = "someaddres:8080/path/\\d+/path";
        String hostAndEndpoint = "someaddres:8080/path/190283/path/path";
        testPercentage(dmKey, KeyType.FIXED, hostAndEndpoint,90, 10, 0, 10);
        testPercentage(dmKey, KeyType.FIXED, hostAndEndpoint,90, 20, 0, 20);
        testPercentage(dmKey, KeyType.FIXED, hostAndEndpoint,90, 30, 0, 30);
    }

    private void testPercentage(int percent, int totalRequests, int expectedPumpkin, int expectedNonPumpkin) {
        testPercentage("any", KeyType.FIXED, "any", percent, totalRequests, expectedPumpkin, expectedNonPumpkin);
    }

    private void testPercentage(String dmKey, KeyType keyType, String hostAndEndpoint, int percent, int totalRequests, int expectedPumpkin, int expectedNonPumpkin) {
        Map <KeyEntity, Integer> degradationModesMap = new ConcurrentHashMap<>();
        KeyEntity keyEntity = KeyEntity.builder()
                .name(dmKey)
                .type(keyType)
                .build();
        degradationModesMap.put(keyEntity, percent);
        DegradationModes degradationModes = new DegradationModes(degradationModesMap);
        RateLimitingService rateLimitingService = new RateLimitingService(new ConcurrentHashMap<>(), degradationModes);
        List<Boolean> results = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            results.add(rateLimitingService.shouldPumpkinDirectly(hostAndEndpoint));
        }
        long nonPumpkinCount = results.stream().filter(r -> r.equals(false)).count();
        long pumpkinCount = results.stream().filter(r -> r.equals(true)).count();

        assertEquals(expectedPumpkin, pumpkinCount);
        assertEquals(expectedNonPumpkin, nonPumpkinCount);
    }
}
