package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.ResourceUtils;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.PusherToSolomonClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LastSkuSyncLagMonitoringJobTest extends AbstractContextualTest {

    @Autowired
    private LastSkuSyncLagMonitoringJob job;

    @MockBean
    private PusherToSolomonClient pusherToSolomonClient;

    /**
     * Проверка на конфиге ff-intervals без дефолтной настройки по FullSync (warehouseId: -1).
     * <p>
     * В ff-interval два склада: 147 (FullSync), 171 (PriorityFullSync).
     * В БД sku по 4-м складам: 147, 171, 172 (только sku), 666 (updatable=False)
     * <p>
     * Проверяем, что по 147 и 171 будут отданы метрики.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/health/sku_sync_lag/1.xml",
            "classpath:database/states/health/sku_sync_lag/skus.xml"
    })
    public void onDefinedInFFIntervalWarehouses() throws Exception {
        setActiveWarehouses(147, 171, 172);
        doNothing().when(pusherToSolomonClient).push(anyString());

        job.trigger();

        String expected = readExpectedAnswer("classpath:database/expected/service.health/sku_sync_lag/1.json");
        verify(pusherToSolomonClient, times(1)).push(expected);
    }

    /**
     * Проверка на конфиге ff-intervals c дефолтной настройкой по FullSync (warehouseId: -1).
     * <p>
     * Активные склады: 171, 172.
     * В ff-interval два склада: 147, 171, -1.
     * В БД sku по 4-м складам: 147, 171, 172 (только sku), 666 (updatable=False)
     * <p>
     * Проверяем, что по 171 и 172 будут отданы метрики.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/health/sku_sync_lag/2.xml",
            "classpath:database/states/health/sku_sync_lag/skus.xml"
    })
    public void onDefinedInFFIntervalWarehousesWithDefaultValue() throws Exception {
        setActiveWarehouses(171, 172);
        doNothing().when(pusherToSolomonClient).push(anyString());

        job.trigger();

        String expected = readExpectedAnswer("classpath:database/expected/service.health/sku_sync_lag/2.json");
        verify(pusherToSolomonClient, times(1)).push(expected);
    }

    /**
     * Проверка на конфиге ff-intervals c дефолтной настройкой по FullSync (warehouseId: -1).
     * <p>
     * Активные склады: 171, 172.
     * В ff-interval два склада: 147, 171, -1.
     * В БД sku по 4-м складам: 147, 171, 172 (только sku), 666 (updatable=False)
     * <p>
     * Проверяем, что по 171 и 172 будут отданы метрики.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/health/sku_sync_lag/2.xml",
            "classpath:database/states/health/sku_sync_lag/skus.xml"
    })
    public void onNonActiveWarehouses() throws Exception {
        setActiveWarehouses();
        doNothing().when(pusherToSolomonClient).push(anyString());

        job.trigger();

        verify(pusherToSolomonClient, times(1)).push("{\"ts\":1514754000}");
    }

    private String readExpectedAnswer(String pathToJson) throws IOException {
        File file = ResourceUtils.getFile(pathToJson);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

}
