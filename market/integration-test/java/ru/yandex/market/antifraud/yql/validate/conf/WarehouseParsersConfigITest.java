package ru.yandex.market.antifraud.yql.validate.conf;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.pp.PpStateService;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.pp.reader.PpReader;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class WarehouseParsersConfigITest {
    @Autowired
    private PpStateService ppLoaderService;

    @Autowired
    private WarehouseParsersConfig config;

    @Test
    public void smokeTest() {
        PpReader.PpSetup state = ppLoaderService.getState(null);
        assertTrue(state.ignoredFor("shows").size() > 0);
        assertTrue(state.validFor("shows").size() > 0);
    }
}
