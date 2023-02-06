package ru.yandex.market.loyalty.admin.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.loyalty.admin.mds.ContentExistsConsumer;
import ru.yandex.market.loyalty.admin.mds.DirectoryEntry;
import ru.yandex.market.loyalty.admin.mds.TimestampContentConsumer;
import ru.yandex.market.loyalty.admin.monitoring.MdsMonitorService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.time.temporal.ChronoUnit;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.monitoring.MdsMonitorService.MINUTES_WITHIN_SNAPSHOT_IS_ACTUAL;
import static ru.yandex.market.loyalty.core.utils.MonitorHelper.assertMonitor;

@TestFor(MdsMonitorService.class)
public class MdsMonitorServiceTest extends MarketLoyaltyAdminMockedDbTest {
    @Value("${market.loyalty.mds.prefix}")
    private String mdsPrefix;
    @Autowired
    private MdsMonitorService mdsMonitorService;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Test
    public void testMdsMonitorAnythingNotOpen() {
        when(mdsS3Client.download(argThat(hasProperty("key",
                equalTo(mdsPrefix + DirectoryEntry.TIMESTAMP.getFileName()))),
                argThat(isA(TimestampContentConsumer.class))))
                .thenReturn(OptionalLong.empty());
        for (DirectoryEntry directoryEntry : DirectoryEntry.values()) {
            when(mdsS3Client.download(argThat(hasProperty("key", equalTo(mdsPrefix + directoryEntry.getFileName()))),
                    argThat(isA(ContentExistsConsumer.class))))
                    .thenReturn(false);
        }

        ComplicatedMonitoring.Result check = mdsMonitorService.check();
        assertMonitor(MonitoringStatus.CRITICAL, check);
        assertEquals("Key " + mdsPrefix + "timestamp is unavailable.;", check.getMessage());
    }

    @Test
    public void testMdsMonitorCurrentPointToNotCorrectFormat() {
        when(mdsS3Client.download(argThat(hasProperty("key",
                equalTo(mdsPrefix + DirectoryEntry.TIMESTAMP.getFileName()))),
                argThat(isA(TimestampContentConsumer.class))))
                .thenReturn(OptionalLong.empty());
        for (DirectoryEntry directoryEntry : DirectoryEntry.values()) {
            when(mdsS3Client.download(argThat(hasProperty("key", equalTo(mdsPrefix + directoryEntry.getFileName()))),
                    argThat(isA(ContentExistsConsumer.class))))
                    .thenReturn(true);
        }

        ComplicatedMonitoring.Result check = mdsMonitorService.check();
        assertMonitor(MonitoringStatus.CRITICAL, check);
        assertEquals("Key " + mdsPrefix + "timestamp is stale (created more then " + MINUTES_WITHIN_SNAPSHOT_IS_ACTUAL + " minutes).;", check.getMessage());
    }

    @Test
    public void testMdsMonitorCurrentPointToNotActual() {
        when(mdsS3Client.download(argThat(hasProperty("key",
                equalTo(mdsPrefix + DirectoryEntry.TIMESTAMP.getFileName()))),
                argThat(isA(TimestampContentConsumer.class))))
                .thenReturn(OptionalLong.of(TimeUnit.MILLISECONDS.toSeconds(clock.millis())));
        for (DirectoryEntry directoryEntry : DirectoryEntry.values()) {
            when(mdsS3Client.download(argThat(hasProperty("key", equalTo(mdsPrefix + directoryEntry.getFileName()))),
                    argThat(isA(ContentExistsConsumer.class))))
                    .thenReturn(true);
        }

        clock.spendTime(MINUTES_WITHIN_SNAPSHOT_IS_ACTUAL + 1, ChronoUnit.MINUTES);

        ComplicatedMonitoring.Result check = mdsMonitorService.check();
        assertMonitor(MonitoringStatus.CRITICAL, check);
        assertEquals("Key " + mdsPrefix + "timestamp is stale (created more then " + MINUTES_WITHIN_SNAPSHOT_IS_ACTUAL + " minutes).;", check.getMessage());
    }

    @Test
    public void testMdsMonitorCurrentPointToOk() {
        when(mdsS3Client.download(argThat(hasProperty("key",
                equalTo(mdsPrefix + DirectoryEntry.TIMESTAMP.getFileName()))),
                argThat(isA(TimestampContentConsumer.class))))
                .thenReturn(OptionalLong.of(TimeUnit.MILLISECONDS.toSeconds(clock.millis())));
        for (DirectoryEntry directoryEntry : DirectoryEntry.values()) {
            when(mdsS3Client.download(argThat(hasProperty("key", equalTo(mdsPrefix + directoryEntry.getFileName()))),
                    argThat(isA(ContentExistsConsumer.class))))
                    .thenReturn(true);
        }

        ComplicatedMonitoring.Result check = mdsMonitorService.check();
        assertMonitor(MonitoringStatus.OK, check);
    }

}
