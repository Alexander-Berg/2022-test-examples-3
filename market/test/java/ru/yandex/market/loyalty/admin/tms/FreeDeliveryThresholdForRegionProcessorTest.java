package ru.yandex.market.loyalty.admin.tms;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.model.PropertyStateType;
import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 10.11.2020
 */
@TestFor(FreeDeliveryThresholdForRegionProcessor.class)
public class FreeDeliveryThresholdForRegionProcessorTest extends MarketLoyaltyAdminMockedDbTest {

    private final static String CSV_FILE_NAME = "freeThresholdRegionMARKETDISCOUNT3818.csv";
    private final static String MARKETDISCOUNT4180_TIER1 = "MARKETDISCOUNT4180_tier1.csv";
    private final static String MARKETDISCOUNT4180_TIER2 = "MARKETDISCOUNT4180_tier2.csv";
    private final static long CSV_FILE_SIZE = 1352;
    private final static long TIER2_FILE_SIZE = 5590;
    private final static long TIER1_FILE_SIZE = 1315;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private RegionSettingsService regionSettingsService;
    @Autowired
    private FreeDeliveryThresholdForRegionProcessor processor;

    @Test
    public void dummyTest() {
        processor.process();
        processor.processV2();

    }

    //    @Test
    public void checkCountCsvFileAndDBRows() throws IOException {
        URL resource = resourceLoader.getResource("classpath:" + CSV_FILE_NAME).getURL();
        assertNotNull(resource);
        CSVParser parser = CSVParser.parse(resource, StandardCharsets.UTF_8, CSVFormat.newFormat('\t'));
        List<CSVRecord> records = parser.getRecords();
        assertEquals(records.size(), CSV_FILE_SIZE);
        processor.process();
        regionSettingsService.reloadCache();
        long settingsWithThresholdSize = regionSettingsService.getAllWithEnabledThreshold().size();
        assertEquals(settingsWithThresholdSize, CSV_FILE_SIZE);
    }

    //    @Test
    public void checkCountCsvFileAndDBRowsMarketDiscount4180() throws IOException {
        regionSettingsService.saveOrUpdateRegionSettingsList(Collections.singletonList(RegionSettings.builder()
                .withRegionId(215307)
                .withYandexPlusThresholdEnabled(PropertyStateType.INHERITED)
                .withYandexPlusThresholdValue(null)
                .build()));
        regionSettingsService.reloadCache();
        Optional<RegionSettings> pushkino = regionSettingsService.getAllByRegionId(215307);
        assertTrue(pushkino.isPresent());
        assertEquals(pushkino.get().getYandexPlusThresholdEnabled(), PropertyStateType.INHERITED);
        assertNull(pushkino.get().getYandexPlusThresholdValue());
        URL resourceTier1 = resourceLoader.getResource("classpath:" + MARKETDISCOUNT4180_TIER1).getURL();
        URL resourceTier2 = resourceLoader.getResource("classpath:" + MARKETDISCOUNT4180_TIER2).getURL();
        assertNotNull(resourceTier1);
        assertNotNull(resourceTier2);
        CSVParser parserTier1 = CSVParser.parse(resourceTier1, StandardCharsets.UTF_8, CSVFormat.newFormat('\t'));
        CSVParser parserTier2 = CSVParser.parse(resourceTier2, StandardCharsets.UTF_8, CSVFormat.newFormat('\t'));
        List<CSVRecord> recordsTier1 = parserTier1.getRecords();
        List<CSVRecord> recordsTier2 = parserTier2.getRecords();
        assertEquals(recordsTier1.size(), TIER1_FILE_SIZE);
        assertEquals(recordsTier2.size(), TIER2_FILE_SIZE);
        processor.processV2();
        regionSettingsService.reloadCache();
        long settingsWithThresholdSize =
                regionSettingsService.getAllWithEnabledThreshold().size() + regionSettingsService.getAllWithDisabledThreshold().size();
        assertEquals(settingsWithThresholdSize, TIER1_FILE_SIZE + TIER2_FILE_SIZE);
        pushkino = regionSettingsService.getAllByRegionId(215307);
        assertTrue(pushkino.isPresent());
        assertEquals(pushkino.get().getYandexPlusThresholdEnabled(), PropertyStateType.ENABLED);
        assertEquals(pushkino.get().getYandexPlusThresholdValue(), BigDecimal.valueOf(699));
    }

}
