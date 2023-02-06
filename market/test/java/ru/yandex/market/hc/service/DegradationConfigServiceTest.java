package ru.yandex.market.hc.service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.hc.dao.DegradationDescriptionDao;
import ru.yandex.market.hc.entity.DegradationConfig;
import ru.yandex.market.hc.entity.DegradationDescription;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 10/28/21
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class DegradationConfigServiceTest {

    @MockBean
    private DegradationDescriptionDao degradationDescriptionDao;
    private DegradationConfigService degradationConfigService;
    private DegradationConfig defaultDegradationConfig;

    @Before
    public void setUp() {
        defaultDegradationConfig = DegradationConfig.builder()
                .degradationModes(Collections.singleton(12))
                .updatePeriod(20)
                .build();
        degradationConfigService = new DegradationConfigService(degradationDescriptionDao, defaultDegradationConfig);
    }

    @Test
    public void testGetOrDefault_Invalid() {
        String testName = "test";
        DegradationConfig invalidConfig = DegradationConfig.builder()
                .updatePeriod(100)
                .degradationModes(Stream.of(120, 50).collect(Collectors.toSet()))
                .build();
        DegradationDescription invalidDescription = DegradationDescription.builder()
                .name(testName)
                .config(invalidConfig)
                .build();

        when(degradationDescriptionDao.getDegradationDescriptionByName(testName)).thenReturn(Optional.of(invalidDescription));

        assertEquals(defaultDegradationConfig, degradationConfigService.getOrDefault(testName));
    }

    @Test
    public void testGetOrDefault_Valid() {
        String testName = "test";
        DegradationConfig validConfig = DegradationConfig.builder()
                .updatePeriod(100)
                .degradationModes(Stream.of(12, 50).collect(Collectors.toSet()))
                .build();
        DegradationDescription validDescription = DegradationDescription.builder()
                .name(testName)
                .config(validConfig)
                .build();

        when(degradationDescriptionDao.getDegradationDescriptionByName(testName)).thenReturn(Optional.of(validDescription));

        assertEquals(validConfig, degradationConfigService.getOrDefault(testName));
    }
}
