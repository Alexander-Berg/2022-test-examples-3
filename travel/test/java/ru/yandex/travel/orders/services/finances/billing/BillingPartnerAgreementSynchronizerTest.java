package ru.yandex.travel.orders.services.finances.billing;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.integration.balance.BillingApiClient;
import ru.yandex.travel.integration.balance.BillingClientContract;
import ru.yandex.travel.orders.cache.HotelAgreementDictionary;
import ru.yandex.travel.orders.entities.finances.ProcessingTasksInfo;
import ru.yandex.travel.orders.entities.partners.BillingPartnerConfig;
import ru.yandex.travel.orders.repository.BillingPartnerConfigRepository;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BillingPartnerAgreementSynchronizerTest {
    private BillingPartnerAgreementSynchronizerProperties properties;
    private BillingPartnerConfigRepository partnerConfigRepository;
    private BillingApiClient billingApiClient;
    private SettableClock testClock;
    private BillingPartnerAgreementSynchronizer synchronizer;

    @Before
    public void init() {
        properties = BillingPartnerAgreementSynchronizerProperties.builder()
                .synchronizationInterval(Duration.ofHours(1))
                .build();
        partnerConfigRepository = mock(BillingPartnerConfigRepository.class);
        billingApiClient = mock(BillingApiClient.class);
        testClock = new SettableClock();
        synchronizer = new BillingPartnerAgreementSynchronizer(
                properties, partnerConfigRepository, billingApiClient, ClockService.create(testClock),
                mock(HotelAgreementDictionary.class)
        );
    }

    @Test
    public void getReadyTasks() {
        testClock.setCurrentTime(Instant.parse("2020-01-24T18:00:00Z"));
        Instant maxSynchronizedAt = Instant.parse("2020-01-24T17:00:00Z");
        when(partnerConfigRepository.findIdsForAgreementSynchronization(eq(maxSynchronizedAt), any(), any()))
                .thenReturn(List.of(10L));
        assertThat(synchronizer.getReadyTasks(Set.of(), 5)).isEqualTo(List.of(10L));
    }

    @Test
    public void countCountTasks() {
        testClock.setCurrentTime(Instant.parse("2020-01-24T18:00:00Z"));
        Instant maxSynchronizedAt = Instant.parse("2020-01-24T17:00:00Z");
        when(partnerConfigRepository.countIdsForAgreementSynchronization(eq(maxSynchronizedAt), any())).thenReturn(20L);
        assertThat(synchronizer.countCountTasks(Set.of())).isEqualTo(20L);
    }

    @Test
    public void processTask_ok() {
        BillingPartnerConfig config = new BillingPartnerConfig();
        when(partnerConfigRepository.getOne(eq(30L))).thenReturn(config);
        when(billingApiClient.getClientContracts(30L)).thenReturn(List.of(
                BillingClientContract.builder().active(true).build()
        ));

        assertThat(config.isAgreementActive()).isFalse();
        assertThat(config.getSynchronizedAt()).isNull();

        synchronizer.processTask(30L);
        assertThat(config.isAgreementActive()).isTrue();
        assertThat(config.getSynchronizedAt()).isNotNull();
    }

    @Test
    public void processTask_multipleActiveAgreements() {
        BillingPartnerConfig config = new BillingPartnerConfig();
        when(partnerConfigRepository.getOne(eq(40L))).thenReturn(config);
        when(billingApiClient.getClientContracts(40L)).thenReturn(List.of(
                BillingClientContract.builder().active(true).build(),
                BillingClientContract.builder().active(true).build()
        ));

        assertThatThrownBy(() -> synchronizer.processTask(40L))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple active contracts detected");
    }

    @Test
    public void getCurrentProcessingDelay() {
        when(partnerConfigRepository.findOldestTimestampForAgreementSynchronization(any()))
                .thenReturn(new ProcessingTasksInfo(null, 0L));
        assertThat(synchronizer.getCurrentProcessingDelay()).isEqualTo(Duration.ZERO);

        when(partnerConfigRepository.findOldestTimestampForAgreementSynchronization(any()))
                .thenReturn(new ProcessingTasksInfo(null, 1L));
        assertThat(synchronizer.getCurrentProcessingDelay()).isEqualTo(Duration.ofHours(24));

        testClock.setCurrentTime(Instant.parse("2020-01-27T10:00:00Z"));
        when(partnerConfigRepository.findOldestTimestampForAgreementSynchronization(any()))
                .thenReturn(new ProcessingTasksInfo(Instant.parse("2020-01-27T08:35:00Z"), 1L));
        // the startSynchronization filter will be at 09:00
        assertThat(synchronizer.getCurrentProcessingDelay()).isEqualTo(Duration.ofMinutes(25));
    }
}
