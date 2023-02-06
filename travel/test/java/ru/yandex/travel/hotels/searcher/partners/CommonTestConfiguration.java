package ru.yandex.travel.hotels.searcher.partners;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Import;
import ru.yandex.travel.commons.health.HealthCheckedSupplier;
import ru.yandex.travel.commons.messaging.KeyValueStorage;
import ru.yandex.travel.commons.messaging.MessageBus;
import ru.yandex.travel.hotels.common.encryption.EncryptionService;
import ru.yandex.travel.hotels.common.partners.bronevik.utils.timezone.BronevikHotelTimezone;
import ru.yandex.travel.hotels.common.partners.bronevik.utils.timezone.TestBronevikHotelTimezone;
import ru.yandex.travel.hotels.common.token.TokenCodec;
import ru.yandex.travel.hotels.searcher.HttpClientsConfiguration;
import ru.yandex.travel.hotels.searcher.PropertyConvertersConfiguration;
import ru.yandex.travel.hotels.searcher.SearcherApplicationProperties;
import ru.yandex.travel.hotels.searcher.cold.ColdConfigurationProperties;
import ru.yandex.travel.hotels.searcher.cold.ColdService;
import ru.yandex.travel.hotels.searcher.services.GrpcExchangeRateService;
import ru.yandex.travel.hotels.searcher.services.TravelTokenService;
import ru.yandex.travel.hotels.searcher.services.entities.ExchangeRate;
import ru.yandex.travel.infrastructure.RetryHelperAutoConfiguration;
import ru.yandex.travel.tracing.JaegerTracerConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestConfiguration
@EnableConfigurationProperties({
        SearcherApplicationProperties.class,
        BookingPartnerTaskHandlerProperties.class,
        ColdConfigurationProperties.class
})
@Import({
        FakeClockConfiguration.class,
        HttpClientsConfiguration.class,
        JaegerTracerConfiguration.class,
        PropertyConvertersConfiguration.class,
        RetryHelperAutoConfiguration.class,
})
public class CommonTestConfiguration {
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    }

    @Bean("messageBus")
    public MessageBus messageBus() {
        MessageBus messageBus = Mockito.mock(MessageBus.class);
        when(messageBus.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        return messageBus;
    }

    @Bean("keyValueStorage")
    public KeyValueStorage kvStorage() {
        KeyValueStorage storage = Mockito.mock(KeyValueStorage.class);
        when(storage.put(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(storage.isHealthy()).thenReturn(true);
        return storage;
    }

    @Bean("searchFlowOfferDataStorage")
    public KeyValueStorage searchFlowOfferDataStorage() {
        KeyValueStorage storage = Mockito.mock(KeyValueStorage.class);
        when(storage.put(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(storage.isHealthy()).thenReturn(true);
        return storage;
    }

    @Bean("healthCheckedKeyValueStorageSupplier")
    public HealthCheckedSupplier<KeyValueStorage> storageSupplier(@Qualifier("keyValueStorage") KeyValueStorage kvStorage) {
        return new HealthCheckedSupplier<>(kvStorage, "testKvStorage");
    }

    @Bean("healthCheckedSearchFlowOfferDataStorageSupplier")
    public HealthCheckedSupplier<KeyValueStorage> searchFlowOfferDataStorageSupplier(@Qualifier("searchFlowOfferDataStorage") KeyValueStorage kvStorage) {
        return new HealthCheckedSupplier<>(kvStorage, "testSearchFlowOfferDataStorage");
    }

    @Bean
    public GrpcExchangeRateService exchangeRateService() {
        GrpcExchangeRateService exchangeRateService = Mockito.mock(GrpcExchangeRateService.class);
        ExchangeRate rate = ExchangeRate.builder()
                .rateValue(66.00f)
                .rateValidUntil(Instant.now().plus(20, ChronoUnit.MINUTES))
                .build();
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(Optional.of(rate));
        return exchangeRateService;
    }

    @Bean
    public ColdService coldService(ColdConfigurationProperties properties) {
        return new ColdService(properties);
    }

    @Bean
    public EncryptionService encryptionService() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return new EncryptionService("foo");
    }

    @Bean
    public TokenCodec tokenEncoder(EncryptionService encryptionService) {
        return new TokenCodec(encryptionService);
    }

    @Bean
    public TravelTokenService travelTokenService(TokenCodec tokenEncoder) {
        return new TravelTokenService(tokenEncoder);
    }

    @Bean
    BronevikHotelTimezone bronevikHotelTimezone() {
        return new TestBronevikHotelTimezone();
    }
}
