package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerExternalParamsRepository;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(Parameterized.class)
@DirtiesContext
public class UpdateOrderDeliveryDateInDSTest extends AllMockContextualTest {

    @Parameter
    public String eventRequest;

    @Autowired
    private List<OrderEventsPoller> pollers;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private PartnerExternalParamsRepository partnerExternalParamsRepository;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"post_delivery_date_updated_in_ds.json"},
        });
    }

    @Test
    public void testDeliveryDateChangedInDeliveryService() throws Exception {
        when(partnerExternalParamsRepository.existsByTypeAndPartnerIdAndActiveTrue(any(), anyLong())).thenReturn(true);

        when(deliveryClient.getOrdersDeliveryDate(anyList(), any(Partner.class)))
            .thenReturn(Collections.singletonList(getOrderDeliveryDate()));
        MockRestServiceServer checkouterMockServer = MockRestServiceServer.bindTo(checkouterRestTemplate)
            .ignoreExpectOrder(true)
            .build(getExpectationManager(eventRequest));

        CountDownLatch countDownLatch = new CountDownLatch(2);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        pollers.get(8).poll();

        countDownLatch.await(2, TimeUnit.SECONDS);

        checkouterMockServer.verify();
        verify(deliveryClient, only())
            .getOrdersDeliveryDateAsync(anyList(), any(Partner.class), any(ClientRequestMeta.class));
        verify(partnerExternalParamsRepository).existsByTypeAndPartnerIdAndActiveTrue(any(), anyLong());

    }

    private String getBody(String filePath) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filePath);
        return IOUtils.toString(inputStream, UTF_8);
    }

    private OrderDeliveryDate getOrderDeliveryDate() {
        return new OrderDeliveryDate(
            new ResourceId.ResourceIdBuilder().setYandexId("2106833").build(),
            DateTime.fromLocalDateTime(LocalDateTime.parse("2018-02-12T00:00:00")),
            TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(14, 0)),
            null
        );
    }

    private UnorderedRequestExpectationManager getExpectationManager(String eventFilePath) throws IOException {
        UnorderedRequestExpectationManager manager = new UnorderedRequestExpectationManager();

        manager.expectRequest(
                ExpectedCount.once(),
                requestTo(StringContains.containsString("/orders/events"))
            )
            .andRespond(withSuccess(getBody("/data/events/" + eventFilePath), MediaType.APPLICATION_JSON_UTF8));

        manager.expectRequest(
                ExpectedCount.between(0, 1),
                requestTo(StringContains.containsString("/orders/2106833/edit-options?"))
            )
            .andExpect(content().json(getBody("/data/controller/response/delivery_options_edit_request.json")))
            .andRespond(withSuccess(
                getBody("/data/controller/response/delivery_options_response.json"),
                MediaType.APPLICATION_JSON_UTF8
            ));

        manager.expectRequest(
                ExpectedCount.between(0, 1),
                requestTo(StringContains.containsString("/orders/2106833?"))
            )
            .andRespond(withSuccess(
                getBody("/data/controller/response/checkouter_order_response.json"),
                MediaType.APPLICATION_JSON_UTF8
            ));

        manager.expectRequest(
                ExpectedCount.never(),
                requestTo(StringContains.containsString("/orders/2106833/edit?"))
            )
            .andExpect(content().json(getBody("/data/controller/response/delivery_dates_edit_request.json")))
            .andRespond(withSuccess());

        return manager;
    }
}
