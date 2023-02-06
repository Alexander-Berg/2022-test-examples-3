package ru.yandex.market.bidding.model.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import ru.yandex.market.bidding.model.validator.ValidationConfig;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.BidLimitsService;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"ru.yandex.market.bidding"})
@Import({TestPlaceholderConfig.class,
        ValidationConfig.class})
public class TestConfig {
    @Value("${bid.max}")
    private int maxBid;
    @Value("${bid.min}")
    private int minBid;

    @Bean(initMethod = "afterPropertiesSet")
    public BidLimits bidLimits() {
        BidLimitsService bidLimitsService = new BidLimitsService();
        bidLimitsService.setDefaultMinBid(minBid);
        bidLimitsService.setDefaultMaxBid(maxBid);
        bidLimitsService.setMinBids(Collections.EMPTY_MAP);
        bidLimitsService.setMaxBids(Collections.EMPTY_MAP);
        return bidLimitsService;
    }
}
