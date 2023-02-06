package ru.yandex.market.ff4shops.lgw;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.entity.PartnerFulfillmentId;
import ru.yandex.market.ff4shops.repository.PartnerFulfillmentRepository;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.request.RequestState;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogisticApiRequestsClientServiceTest extends FunctionalTest {
    private static final long PARTNER_ID = 1;
    private static final long SERVICE_ID = 2;

    @Autowired
    private PartnerFulfillmentRepository partnerFulfillmentRepository;

    private LogisticApiRequestsClientService logisticApiRequestsClientService;
    private final HttpTemplate httpTemplate = mock(HttpTemplate.class);
    private final LMSClient lmsClient = mock(LMSClient.class);
    private final HttpTemplateByPartnerIdFactory tvmHttpTemplateByPartnerIdFactory =
        mock(HttpTemplateByPartnerIdFactory.class);

    @BeforeEach
    void init() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
        retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());

        logisticApiRequestsClientService = new LogisticApiRequestsClientService(
            0L,
            0L,
            lmsClient,
            partnerFulfillmentRepository,
            retryTemplate,
            tvmHttpTemplateByPartnerIdFactory
        );
        doReturn(httpTemplate)
            .when(tvmHttpTemplateByPartnerIdFactory)
            .create(eq(SERVICE_ID));
        reset(lmsClient);
        reset(httpTemplate);
    }

    @Test
    @DbUnitDataSet
    void testPushStockWithoutLink() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();

        when(lmsClient.getPartnerApiSettings(SERVICE_ID)).thenReturn(
                SettingsApiDto.newBuilder().partnerId(SERVICE_ID).token("token").build()
        );
        logisticApiRequestsClientService.pushStocks(new PartnerFulfillmentId(PARTNER_ID, SERVICE_ID),
                itemStocks(PARTNER_ID, SERVICE_ID));
        verify(httpTemplate, times(1))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
        verify(lmsClient).getPartnerApiSettings(eq(SERVICE_ID));
    }

    @Test
    @DbUnitDataSet(before = "LogisticApiRequestsClientServiceTest.before.csv")
    void testPushStockWith() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        logisticApiRequestsClientService.pushStocks(new PartnerFulfillmentId(PARTNER_ID, SERVICE_ID),
                itemStocks(PARTNER_ID, SERVICE_ID));
        verify(httpTemplate, times(1))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
        verify(lmsClient, never()).getPartnerApiSettings(any());
    }

    @Test
    @DbUnitDataSet(before = "LogisticApiRequestsClientServiceTest.before.csv",
            after = "LogisticApiRequestsClientServiceTest.token.after.csv")
    void testPushStockChangeToken() {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        ResponseWrapper wrapper = mock(ResponseWrapper.class);
        doThrow(new FulfillmentApiException(new ErrorPair(ErrorCode.INVALID_AUTHORIZATION_TOKEN, "token")))
                .doReturn(wrapper)
                .when(httpTemplate).executePost(any(),
                any(Class.class),
                anyString(),
                anyString());
        doReturn(new RequestState())
                .when(wrapper).getRequestState();
        when(lmsClient.getPartnerApiSettings(SERVICE_ID)).thenReturn(
                SettingsApiDto.newBuilder().partnerId(SERVICE_ID).token("token").build()
        );
        logisticApiRequestsClientService.pushStocks(new PartnerFulfillmentId(PARTNER_ID,SERVICE_ID),
                itemStocks(PARTNER_ID, SERVICE_ID));
        verify(httpTemplate, times(2))
                .executePost(captor.capture(), any(Class.class), anyString(), anyString());
        verify(lmsClient).getPartnerApiSettings(eq(SERVICE_ID));
    }



    private static List<ItemStocks> itemStocks(long partnerId, long serviceId) {
        DateTime dateTime = DateTime.fromLocalDateTime(LocalDateTime.of(2021, 1, 1, 0, 0));
        return List.of(
                new ItemStocks(new UnitId("1", partnerId, "1"), new ResourceId(null, String.valueOf(serviceId)),
                        List.of(new Stock(StockType.FIT, 1, dateTime))),
                new ItemStocks(new UnitId("2", partnerId, "2"), new ResourceId(null, String.valueOf(serviceId)),
                        List.of(new Stock(StockType.FIT, 2, dateTime))),
                new ItemStocks(new UnitId("3", partnerId, "3"), new ResourceId(null, String.valueOf(serviceId)),
                        List.of(new Stock(StockType.FIT, 3, dateTime)))
        );
    }
}
