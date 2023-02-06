package ru.yandex.market.delivery.mdbapp.configuration;

import java.net.URI;
import java.util.Map;

import javax.annotation.Nullable;

import org.assertj.core.util.Lists;
import org.junit.rules.ExternalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import steps.LocationSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.common.util.UrlBuilder;
import ru.yandex.market.delivery.dsmclient.response.TaskResponse;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.lms.LmsLogisticsPointClient;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Component
public class ExternalMocksInitRule extends ExternalResource {

    private static final long SHOP_ID = 231;

    private static final String GET_ORDERS_PATH = "/get-orders";
    private static final String GET_ORDERS_QUERY = "clientRole=SYSTEM&clientId=231";
    public static final String DSM_TASKS_PATH = "/tasks";

    private static final int PAGE_SIZE = 10;

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private GeoInfo geoInfo;

    @Autowired
    private LmsLogisticsPointClient lmsLogisticsPointClient;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    @Qualifier("dsmApiRestTemplate")
    private RestTemplate dsmApiRestTemplate;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    @Value("${dsm.api.host}")
    private String dsmUrl;

    private final Order order = OrderSteps.getFilledOrder();
    private final Shop shop = ShopSteps.getDefaultShop(SHOP_ID);
    private final LogisticsPoint outlet = LogisticPointSteps.getDefaultOutlet();
    private final Location location = LocationSteps.getLocation();

    @Override
    public void before() throws Throwable {
        when(healthManager.isHealthyEnough()).thenReturn(true);

        doReturn(new PagedOrders(Lists.newArrayList(order), Pager.atPage(0, PAGE_SIZE)))
            .when(checkouterRestTemplate)
            .postForObject(
                eq(buildUri(checkouterUrl, GET_ORDERS_PATH, GET_ORDERS_QUERY)),
                any(),
                eq(PagedOrders.class)
            );

        when(dsmApiRestTemplate.exchange(
            eq(dsmUrl + DSM_TASKS_PATH),
            eq(HttpMethod.POST),
            any(),
            eq(TaskResponse.class)
        ))
            .thenReturn(ResponseEntity.ok(new TaskResponse()));

        doReturn(outlet).when(mbiApiClient).getOutlet(anyLong(), anyBoolean());
        doReturn(outlet).when(mbiApiClient).getShop(anyLong());

        when(lmsLogisticsPointClient.getWarehousesByPartnerId(111L))
            .thenReturn(LogisticPointSteps.getWarehouseResponse());
        when(lmsLogisticsPointClient.getWarehousesByPartnerId(321L))
            .thenReturn(LogisticPointSteps.getWarehouseResponse());
        when(geoInfo.getLocation(anyInt())).thenReturn(location);
    }

    private URI buildUri(String url, String path, @Nullable String query) {
        return buildUri(url, path, query, null);
    }

    private URI buildUri(String url, String path, @Nullable String query, @Nullable Map<String, Object> params) {
        UrlBuilder builder = UrlBuilder.fromString(url).withPath(path);
        if (query != null) {
            builder = builder.withQuery(query);
        }

        if (params != null) {
            for (Map.Entry<String, Object> paramsEntry : params.entrySet()) {
                builder = builder.addParameter(paramsEntry.getKey(), paramsEntry.getValue());
            }
        }

        return builder.toUri();
    }

    public Order getOrder() {
        return order;
    }

    public Shop getShop() {
        return shop;
    }

    public LogisticsPoint getOutlet() {
        return outlet;
    }

}
