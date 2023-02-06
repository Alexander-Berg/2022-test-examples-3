package ru.yandex.market.deliverycalculator.indexer.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.util.PooledIdGenerator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link YaDeliverySenderSettingsService}.
 */
class YaDeliverySenderSettingsServiceTest extends FunctionalTest {

    private static final String BUCKET_URL = "http://some.url.here";

    @Autowired
    private YaDeliverySenderSettingsService tested;
    @Autowired
    private MdsS3Client mockedMdsClient;
    @Autowired
    private ResourceLocationFactory mockedLocationFactory;
    @Autowired
    private PooledIdGenerator mockedModifierIdGenerator;

    @BeforeEach
    void init() {
        mockModifierIdGenerator();
    }

    /**
     * Настраивает моки.
     *
     * @throws MalformedURLException - в случае если парс тестовой урлы прошел неуспешно
     */
    @BeforeEach
    void setUpMocks() throws MalformedURLException {
        ResourceLocation resourceLocation = ResourceLocation.create("bucket", "key");
        doReturn(resourceLocation).when(mockedLocationFactory).createLocation(any());
        doReturn(new URL(BUCKET_URL)).when(mockedMdsClient).getUrl(resourceLocation);
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который также размещается на маркете. Настройки содержат и
     * модификаторы, и список служб доставки, с которыми магазин сотрудничает.
     */
    @Test
    @DbUnitDataSet(before = "exportMarketShopSettingsWithModifiers.before.csv",
            after = "exportMarketShopSettingsWithModifiers.after.csv")
    void testExportSenderSettings_carrierLinksAndModifiers_marketShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, times(1)).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который также размещается на маркете. Настройки содержат и
     * модификаторы, и список служб доставки, с которыми магазин сотрудничает, и средние габариты.
     */
    @Test
    @DbUnitDataSet(before = "exportMarketShopSettingsWithModifiersAndWithAverageOfferWeightDimensions.before.csv",
            after = "exportMarketShopSettingsWithModifiersAndWithAverageOfferWeightDimensions.after.csv")
    void testExportSenderSettings_carrierLinksAndModifiersAndAverageOfferWeightDimensions_marketShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, times(1)).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который также размещается на маркете. Настройки содержат только
     * список служб доставки, с которыми магазин сотрудничает. Модификаторов магазин не настраивал.
     */
    @Test
    @DbUnitDataSet(before = "exportMarketShopSettingsWithoutModifiers.before.csv",
            after = "exportMarketShopSettingsWithoutModifiers.after.csv")
    void testExportSenderSettings_onlyCarrierLinks_marketShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, never()).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который также размещается на маркете. Настройки сендера были удалены.
     */
    @Test
    @DbUnitDataSet(before = "exportMarketShopDeletedSettings.before.csv",
            after = "exportMarketShopDeletedSettings.after.csv")
    void testExportSenderSettings_deletedSettings_marketShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, never()).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который не размещается на маркете. Настройки содержат и
     * модификаторы и список служб доставки, с которыми магазин сотрудничает.
     */
    @Test
    @DbUnitDataSet(before = "exportYaDoShopSettingsWithModifiers.before.csv",
            after = "exportYaDoShopSettingsWithModifiers.after.csv")
    void testExportSenderSettings_allSettings_yaDoShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, times(1)).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который не размещается на маркете. Настройки содержат только
     * список служб доставки, с которыми магазин сотрудничает. Модификаторов магазин не настраивал.
     */
    @Test
    @DbUnitDataSet(before = "exportYaDoShopSettingsWithoutModifiers.before.csv",
            after = "exportYaDoShopSettingsWithoutModifiers.after.csv")
    void testExportSenderSettings_onlyCarrierLinks_yaDoShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, never()).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который не размещается на маркете. Настройки содержат и
     * модификаторы, и маппинг служб доставки на регионы, в которых магазин с ними сотрудничает.
     */
    @Test
    @DbUnitDataSet(before = "exportYaDoShopSettingsWithCarrierRegionMapping.before.csv",
            after = "exportYaDoShopSettingsWithCarrierRegionMapping.after.csv")
    void testExportSenderSettings_carrierToRegionsMapping_yaDoShop() throws Exception {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, times(1)).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: экспортируем настройки сендера, который не размещается на маркете. Настройки сендера были удалены.
     */
    @Test
    @DbUnitDataSet(before = "exportYaDoShopDeletedSettings.before.csv",
            after = "exportYaDoShopDeletedSettings.after.csv")
    void testExportSenderSettings_deletedSettings_yaDoShop() throws IOException {
        tested.exportSenderSettings(1L, new Generation(4L, 4L));
        verify(mockedMdsClient, never()).upload(any(), any());
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsService#exportSenderSettings(long, Generation)}.
     * Случай: передали несуществующего сендера.
     */
    @Test
    @DbUnitDataSet(before = "exportYaDoShopDeletedSettings.before.csv")
    void testExportSenderSettings_senderNotExisting() {
        assertThrows(IllegalArgumentException.class, () -> tested.exportSenderSettings(8L, new Generation(4L, 4L)));
    }

    private void mockModifierIdGenerator() {
        when(mockedModifierIdGenerator.generate()).thenAnswer(new Answer<>() {
            private long count = 0;

            @Override
            public Object answer(InvocationOnMock invocation) {
                return count++;
            }
        });
    }
}
