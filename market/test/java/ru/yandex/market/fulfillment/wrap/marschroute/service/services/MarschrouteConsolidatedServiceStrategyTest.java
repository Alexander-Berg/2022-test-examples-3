package ru.yandex.market.fulfillment.wrap.marschroute.service.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.api.ServicesClient;
import ru.yandex.market.fulfillment.wrap.marschroute.api.request.services.ConsolidatedServicesRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.services.ConsolidatedService;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.services.ConsolidatedServicesResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceKey;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.MarschrouteServiceRepository;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

import static org.mockito.Matchers.any;

@ExtendWith(MockitoExtension.class)
class MarschrouteConsolidatedServiceStrategyTest extends SoftAssertionSupport {
    @Mock
    MarschrouteServiceRepository serviceRepository;
    @Mock
    ServicesClient servicesClient;

    @InjectMocks
    MarschrouteConsolidatedServiceStrategy serviceStrategy;



    private static final List<ConsolidatedService> SERVICES = Arrays.asList(
        new ConsolidatedService(1L, "1","1","2011","10","Serice1","Service1",BigDecimal.TEN,new BigDecimal(100)),
        new ConsolidatedService(1L, "1","1","2011","10","Serice1","Service1",new BigDecimal(20),new BigDecimal(300)),
        new ConsolidatedService(1L, "1","1","2011","5","Serice1","Service1",new BigDecimal(5),new BigDecimal(50))
    );

    @Test
    void getActualServices() {

        ConsolidatedServicesResponse response = new ConsolidatedServicesResponse();
        response.setData(SERVICES);

        Mockito.when(servicesClient.getConsolidatedServices(any(ConsolidatedServicesRequest.class)))
            .thenReturn(response);

        Map<MarschrouteServiceKey, ConsolidatedService> actualServices = serviceStrategy.getActualServices(LocalDate.now());

        softly.assertThat(actualServices).hasSize(1);
        ConsolidatedService next = actualServices.values().iterator().next();
        Map<String, BigDecimal> consolidatedRate = next.getConsolidatedRate();
        softly.assertThat(consolidatedRate.get("10")).isEqualByComparingTo(new BigDecimal(30));
        softly.assertThat(consolidatedRate.get("5")).isEqualByComparingTo(new BigDecimal(5));
        softly.assertThat(next.getSumNds()).isEqualByComparingTo(new BigDecimal(450));

    }
}
