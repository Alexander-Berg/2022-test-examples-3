package ru.yandex.market.api.partner.controllers.price;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mboc.http.MboMappingsService;

@ParametersAreNonnullByDefault
@Configuration
public class OfferPriceControllerTestConfig {

    @Nonnull
    @Bean
    public Clock offerPriceControllerClock() {
        return new TestClock(OffsetDateTime.parse("2017-12-04T01:15:01.000+03:00").toInstant());
    }

    @Bean(autowire = Autowire.BY_NAME)
    public MboMappingsService impatientMboMappingsService() {
        return Mockito.mock(MboMappingsService.class);
    }

    public static class TestClock extends Clock {
        private final Instant initialTime;
        private Instant currentTime;

        TestClock(Instant initialTime) {
            this.initialTime = initialTime;
            this.currentTime = initialTime;
        }

        @Override
        public ZoneId getZone() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Instant instant() {
            Instant result = currentTime;
            currentTime = currentTime.plus(Duration.ofSeconds(1));
            return result;
        }

        public void restart() {
            currentTime = initialTime;
        }
    }
}
