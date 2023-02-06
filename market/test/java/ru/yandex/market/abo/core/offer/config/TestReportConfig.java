package ru.yandex.market.abo.core.offer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ReportHelper;
import ru.yandex.market.abo.core.offer.report.ReportOfferService;
import ru.yandex.market.abo.test.MockFactory;
import ru.yandex.market.common.report.DefaultAsyncMarketReportService;

/**
 * @author artemmz
 * @date 20.02.2020
 */
@Configuration
@RequiredArgsConstructor
public class TestReportConfig {
    private final ReportHelper reportHelper = new ReportHelper(MockFactory.getShopInfoServiceMock(true));

    @Bean
    public OfferService offerService(DefaultAsyncMarketReportService marketReportService,
                                     DefaultAsyncMarketReportService testMarketReportService) {
        return new ReportOfferService(
                marketReportService,
                testMarketReportService,
                MockFactory.getShopInfoServiceMock(true),
                reportHelper
        );
    }

    @Bean
    public OfferService psOfferService(DefaultAsyncMarketReportService marketReportService,
                                       DefaultAsyncMarketReportService testMarketReportService) {
        return new ReportOfferService(
                marketReportService,
                testMarketReportService,
                MockFactory.getShopInfoServiceMock(false),
                reportHelper
        );
    }

}
