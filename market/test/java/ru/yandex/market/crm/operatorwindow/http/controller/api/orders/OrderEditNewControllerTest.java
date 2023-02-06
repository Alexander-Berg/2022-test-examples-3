package ru.yandex.market.crm.operatorwindow.http.controller.api.orders;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.common.security.SecurityEventsLogService;
import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.http.test.matcher.PathMatcher;
import ru.yandex.market.jmf.security.Auth;
import ru.yandex.market.jmf.utils.SerializationUtils;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.ocrm.module.checkouter.InteractiveCheckouterService;
import ru.yandex.market.ocrm.module.tpl.HttpMarketTplClient;

@ExtendWith(MockitoExtension.class)
public class OrderEditNewControllerTest {

    private static final ObjectSerializeService serializerService = SerializationUtils.defaultObjectSerializeService();

    @Mock
    private HttpClientFactory factory;
    @Mock
    private HttpClient httpClient;
    @Mock
    private InteractiveCheckouterService checkouter;
    @Mock
    private SecurityEventsLogService securityEventsLogService;
    @Mock
    private OrderControllerHelper orderControllerHelper;

    private OrderEditNewController controller;

    @BeforeEach
    public void setup() {
        Mockito.when(factory.create(Mockito.any())).thenReturn(httpClient);
        controller = new OrderEditNewController(
                checkouter,
                securityEventsLogService,
                orderControllerHelper,
                new HttpMarketTplClient(factory, serializerService)
        );
    }

    // В задаче OCRM-10018 возвращаемся на изменение дат через чекаутер
    // если остановимся на этой ручке, то удалить этот тест
    // @Test
    public void getRescheduleDates() {
        var orderId = 14543723L;
        var path = String.format("internal/partner/v3/orders/%s/rescheduleDates", orderId);
        var rawResponse = ResourceHelpers.getResource("rescheduleDates.json");

        Mockito.when(httpClient.execute(Mockito.argThat(new PathMatcher(path))))
                .thenReturn(ResponseBuilder.newBuilder().body(rawResponse).build());

        var result = controller.getEditOptions(orderId, Auth.robot()).getDeliveryDateOptions();

        Assertions.assertEquals(5, result.size());
        var dates = result.get(0);
        Assertions.assertEquals(LocalDate.of(2021, 11, 29), dates.getFrom());
        Assertions.assertEquals(LocalDate.of(2021, 11, 29), dates.getTo());
        var timeIntervals = dates.getTimeIntervals();
        Assertions.assertEquals(2, timeIntervals.size());
        var interval = timeIntervals.get(0);
        Assertions.assertEquals(LocalTime.of(6, 0), interval.getTimeFrom());
        Assertions.assertEquals(LocalTime.of(15, 0), interval.getTimeTo());
        interval = timeIntervals.get(1);
        Assertions.assertEquals(LocalTime.of(7, 0), interval.getTimeFrom());
        Assertions.assertEquals(LocalTime.of(12, 0), interval.getTimeTo());
    }
}
