package ru.yandex.market.notifier.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.referee.CheckoutReferee;
import ru.yandex.market.checkout.referee.CheckoutRefereeClient;
import ru.yandex.market.common.zk.ZooClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.notifier.MarketNotifierClient;
import ru.yandex.market.notifier.ff4shops.client.FF4ShopsClient;
import ru.yandex.market.notifier.service.ShopMeta;
import ru.yandex.market.notifier.service.ShopMetaService;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationTransportType;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotifierTestMockFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NotifierTestMockFactory.class);

    public static final long INVALID_SHOP_ID = 1337L;

    public CheckouterClient getCheckouterClient() {
        return mock(CheckouterClient.class);
    }

    public CheckoutReferee getCheckoutRefereeClient() {
        return mock(CheckoutRefereeClient.class);
    }

    public MarketNotifierClient getMarketNotifierClient() {
        return mock(MarketNotifierClient.class);
    }

    public ZooClient getZooClient() {
        return mock(ZooClient.class);
    }

    public PersNotifyClient getPersNotifyClient() {
        return mock(PersNotifyClient.class);
    }

    public RestTemplate getPersNotifyRestTemplate() {
        return mock(RestTemplate.class);
    }

    public FF4ShopsClient getFF4ShopsClient() {
        return mock(FF4ShopsClient.class);
    }

    public RestTemplate getMbiRestTemplate() {
        final RestTemplate restTemplate = mock(RestTemplate.class);
        try {
            initMbiRestTemplate(restTemplate);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return restTemplate;
    }

    public void initMbiRestTemplate(RestTemplate restTemplate) throws IOException {
        ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
        when(restTemplate.getRequestFactory())
                .thenReturn(requestFactory);
        ClientHttpRequest request = mock(ClientHttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(requestFactory.createRequest(any(), eq(HttpMethod.POST)))
                .thenReturn(request);
        when(request.getBody())
                .thenReturn(new ByteArrayOutputStream());
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(request.execute())
                .thenReturn(response);
        when(response.getRawStatusCode())
                .thenReturn(200);
    }

    public RestTemplate getRedRestTemplate() {
        final RestTemplate restTemplate = mock(RestTemplate.class);
        try {
            initRedRestTemplate(restTemplate);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return restTemplate;
    }

    public void initRedRestTemplate(RestTemplate restTemplate) throws IOException {
        ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
        when(restTemplate.getRequestFactory())
                .thenReturn(requestFactory);

        ClientHttpRequest request = mock(ClientHttpRequest.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);

        when(requestFactory.createRequest(any(), eq(HttpMethod.POST)))
                .thenReturn(request);

        OutputStream requestBody = new ByteArrayOutputStream();
        when(request.getBody())
                .thenReturn(requestBody);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(request.execute())
                .thenReturn(response);
        when(response.getRawStatusCode())
                .thenReturn(200);
    }

    public void mockPersNotifyClientToThrowForPush(PersNotifyClient persNotifyClient) throws PersNotifyClientException {
        Mockito.doThrow(PersNotifyClientException.class)
                .when(persNotifyClient)
                .createEvent(ArgumentMatchers.argThat(it ->
                        it.getNotificationSubtype().getTransportType() == NotificationTransportType.PUSH));
    }

    public void mockPushApiWithBadRequestRespOnOrderStatus(PushApi pushApi) {
        Mockito.doThrow(new ErrorCodeException("bad_request", "error", HttpStatus.BAD_REQUEST.value()))
                .when(pushApi)
                .orderStatus(anyLong(), any(), anyBoolean(), any(), any(), any());
    }

    public ShopMetaService getShopMetaService() {
        ShopMetaService shopMetaService = mock(ShopMetaService.class);
        initShopMetaService(shopMetaService);
        return shopMetaService;
    }

    private ShopMeta generateShopMeta() {
        ShopMeta shopMeta = new ShopMeta();
        shopMeta.setShopId(0);
        shopMeta.setShopName("");
        shopMeta.setShopPhone("");
        return shopMeta;
    }

    public void initShopMetaService(ShopMetaService shopMetaService) {
        when(shopMetaService.needsRefresh()).thenReturn(false);
        when(shopMetaService.getShopMeta(ArgumentMatchers.eq(INVALID_SHOP_ID)))
                .thenReturn(null);
        when(shopMetaService.getShopMeta(AdditionalMatchers.not(ArgumentMatchers.eq(INVALID_SHOP_ID))))
                .thenReturn(generateShopMeta());
    }

    public RegionService getRegionService() {
        RegionService regionService = mock(RegionService.class);
        initRegionService(regionService);
        return regionService;
    }

    public void initRegionService(RegionService regionService) {
        Region region = mock(Region.class);
        when(region.getCustomAttributeValue(eq(CustomRegionAttribute.TIMEZONE_OFFSET))).thenReturn("0");

        RegionTree regionTree = mock(RegionTree.class);
        when(regionTree.getRegion(anyInt())).thenReturn(region);

        when(regionService.getRegionTree()).thenReturn(regionTree);
    }

    public MbiApiClient getMbiApiClient() {
        MbiApiClient mbiApiClient = mock(MbiApiClient.class);
        when(mbiApiClient.sendMessageToSupplier(anyLong(), anyInt(), anyString()))
                .thenAnswer((Answer<SendNotificationResponse>) invocation -> {
                    LOG.info(
                            "sendMessageToSupplier invocation {} {} {}",
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2)
                    );
                    return new SendNotificationResponse();
                });
        return mbiApiClient;
    }

    public PartnerNotificationClient getPartnerNotificationClient() {
        return mock(PartnerNotificationClient.class);
    }

}
