package ru.yandex.market.ocrm.module.order;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.antifraud.orders.client.MstatAntifraudCrmClient;
import ru.yandex.market.crm.external.report.ReportClient;
import ru.yandex.market.jmf.module.comment.test.ModuleCommentTestConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.geo.impl.suggest.GeoSuggestClient;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.module.xiva.ModuleXivaTestConfiguration;
import ru.yandex.market.ocrm.module.checkouter.ModuleCheckouterTestConfiguration;
import ru.yandex.market.ocrm.module.order.impl.region.RegionNameSource;
import ru.yandex.market.ocrm.module.tpl.MarketTplClient;

@Configuration
@Import({
        ModuleOrderConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        ModuleCheckouterTestConfiguration.class,
        ModuleXivaTestConfiguration.class,
        ModuleTicketTestConfiguration.class,
        ModuleCommentTestConfiguration.class
})
@ComponentScan("ru.yandex.market.ocrm.module.order.test")
public class ModuleOrderTestConfiguration {

    @Bean
    @Primary
    public OrderPaymentPartitionsService orderPaymentPartitionsServiceMock() {
        return Mockito.mock(OrderPaymentPartitionsService.class);
    }

    @Bean
    @Primary
    public OrderReceiptsSource orderReceiptsSource() {
        return Mockito.mock(OrderReceiptsSource.class);
    }

    @Bean
    @Primary
    public OrderReturnSource orderReturnSource() {
        return Mockito.mock(OrderReturnSource.class);
    }

    @Bean
    @Primary
    public OrderChangeRequestService orderChangeRequestService() {
        return Mockito.mock(OrderChangeRequestService.class);
    }

    @Bean
    @Primary
    public RegionNameSource regionNameSource() {
        return Mockito.mock(RegionNameSource.class);
    }

    @Bean
    public ReportClient reportClient() {
        return Mockito.mock(ReportClient.class);
    }

    @Bean
    @Primary
    public MstatAntifraudCrmClient antifraudCrmClient() {
        return Mockito.mock(MstatAntifraudCrmClient.class);
    }

    @Bean
    @Primary
    public MarketTplClient testMarketTplClient() {
        return Mockito.mock(MarketTplClient.class);
    }

    @Bean
    @Primary
    public GeoSuggestClient testGeoSuggestClient() {
        return Mockito.mock(GeoSuggestClient.class);
    }
}
