package ru.yandex.market.fintech.banksint.config;

import java.util.concurrent.ExecutorService;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.yandex.market.fintech.banksint.service.datacamp.OffersDataCampService;
import ru.yandex.market.fintech.banksint.service.installment.offer.OffersMdsS3Service;

@Configuration
@Profile("functionalTest")
public class OffersConfig {

    @Bean
    public OffersDataCampService mockDataCampService() {
        return Mockito.mock(OffersDataCampService.class);
    }

    @Bean
    public OffersMdsS3Service mockOffersMdsService() {
        return Mockito.mock(OffersMdsS3Service.class);
    }

    @Bean
    public ExecutorService mockOffersExecutorService() {
        return Mockito.mock(ExecutorService.class);
    }
}
