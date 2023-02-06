package ru.yandex.market.checkout.referee.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.referee.external.dealer.DealerApiClient;
import ru.yandex.market.checkout.referee.impl.AssetiveInputStream;
import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.shopinfo.SupplierInfo;
import ru.yandex.market.shopinfo.SupplierInfoService;

import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * @author kukabara
 */
public class MockFactory {
    private static final Integer TOTAL = 10;

    @Autowired
    private WebApplicationContext wac;
    private static CheckouterAPI checkouterClientMock;
    private static CheckouterOrderHistoryEventsApi orderHistoryEventsApi;

    public MockMvc getMockMvc() {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                // uncomment this for DEBUG
//            .alwaysDo(MockMvcResultHandlers.print())

//            .addFilter(getOutputCharsetFilter(), "/*")
//            .addFilter(getCharacterEncodingFilter(), "/*")
                .build();

        return mockMvc;
    }


//    private CharacterEncodingFilter getCharacterEncodingFilter() {
//        CharacterEncodingFilter f = new CharacterEncodingFilter();
//        f.setEncoding("UTF-8");
//        f.setForceEncoding(true);
//        return f;
//    }
//
//    private OutputFilter getOutputCharsetFilter() {
//        OutputFilter f = new OutputFilter();
//        f.charset = "UTF-8";
//        return f;
//    }

    private static void initCheckouterGetOrder(CheckouterAPI checkouterClientMock) {
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            long orderId = (long) arguments[0];

            if (orderId == BaseTest.ORDER_ID_NOT_FOUND) {
                throw new OrderNotFoundException(orderId);
            }

            ClientRole role = (ClientRole) arguments[1];
            Long uid;
            Long shopId;
            switch (role) {
                case USER:
                    uid = (Long) arguments[2];
                    shopId = newUID();
                    break;
                case SHOP:
                    uid = newUID();
                    shopId = (Long) arguments[2];
                    break;
                default:
                    uid = newUID();
                    shopId = newUID();
            }

            return BaseTest.getOrder(orderId, uid, shopId);
        }).when(checkouterClientMock).getOrder(anyLong(), any(), anyLong());
    }

    public static CheckouterAPI getCheckouterClientMock() {
        if (checkouterClientMock == null) {
            checkouterClientMock = mock(CheckouterAPI.class);
            orderHistoryEventsApi = mock(CheckouterOrderHistoryEventsApi.class);
            doReturn(new CheckResult(CheckResult.Level.OK, null)).when(checkouterClientMock).ping();
            doReturn(orderHistoryEventsApi).when(checkouterClientMock).orderHistoryEvents();

            initCheckouterGetOrder(checkouterClientMock);
            doAnswer(invocation -> {
                Object[] arguments = invocation.getArguments();
                long[] orderIds = (long[]) arguments[0];

                Collection<OrderHistoryEvent> content = new ArrayList<>();
                for (long orderId : orderIds) {
                    OrderHistoryEvent e = new OrderHistoryEvent();
                    Order orderWithProcessing = new Order();
                    orderWithProcessing.setStatus(OrderStatus.PROCESSING);
                    orderWithProcessing.setId(orderId);
                    e.setOrderAfter(orderWithProcessing);
                    e.setFromDate(new Date());
                    content.add(e);
                }
                return new OrderHistoryEvents(content);
            }).when(orderHistoryEventsApi).getOrdersHistoryEvents(any(), any(), any());

            // get-orders

            doAnswer(invocation -> {
                OrderSearchRequest request = (OrderSearchRequest) invocation.getArguments()[0];
                Long shopId = request.shopId != null ? request.shopId : BaseTest.newUID();
                Long userId = request.userId != null ? request.userId : BaseTest.newUID();
                List<Long> orderIds = request.orderIds;

                int page = request.pageInfo != null ? request.pageInfo.getCurrentPage() : 1;
                int pageSize = request.pageInfo != null ? request.pageInfo.getPageSize() : 10;
                int total = orderIds != null && !orderIds.isEmpty() ? orderIds.size() : TOTAL;

                List<Order> orders = new ArrayList<>();
                if (orderIds != null && !orderIds.isEmpty()) {
                    for (Long orderId : orderIds) {
                        orders.add(BaseTest.getOrder(orderId, shopId, userId));
                    }
                } else {
                    long orderId = BaseTest.newUID();
                    for (int i = 0; i < total; i++) {
                        orders.add(BaseTest.getOrder(orderId, shopId, userId));
                    }
                }
                return new PagedOrders(orders, getPager(page, pageSize, total));
            }).when(checkouterClientMock).getOrders(any(), any(), anyLong());
        }
        return checkouterClientMock;
    }

    private static Pager getPager(Integer pageNumber, Integer pageSize, int total) {
        int currentPage = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int realPageSize = pageSize == null ? 50 : pageSize;

        // set pages count
        int pagesCount = total / realPageSize + (total % realPageSize > 0 ? 1 : 0);
        if (pagesCount == 0) {
            pagesCount = 1;
        }

        // set from, to
        currentPage = Math.max(Math.min(currentPage, pagesCount), 1);
        int from = Math.min((currentPage - 1) * realPageSize, total);
        int to = Math.min(currentPage * realPageSize, total);
        return new Pager(total, from, to, pageSize, pagesCount, currentPage);
    }


    public MemCachedAgent getMemCachedAgentMock() {
        MemCachedAgent result = mock(MemCachedAgent.class);
        initMemCachedAgentMock(result);
        return result;
    }

    public synchronized void initMemCachedAgentMock(MemCachedAgent result) {
        final Map<String, Object> cache = Collections.synchronizedMap(new HashMap<>());

        when(result.addInCache(anyString(), anyObject(), anyObject())).thenAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return true;
        });

        when(result.getFromCache(anyCollection())).thenAnswer(invocation -> {
            Map<String, Object> map = new HashMap<>();
            Collection<String> keys = (Collection<String>) invocation.getArguments()[0];
            for (String next : keys) {
                if (cache.containsKey(next)) {
                    map.put(next, cache.get(next));
                }
            }
            return map;
        });

        when(result.getFromCache(anyString())).thenAnswer(invocation ->
                cache.get(invocation.getArguments()[0]));

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = cache.get(key);
            Long incValue = value == null ? 1 : Long.valueOf((String) value) + 1;
            cache.put(key, String.valueOf(incValue));
            return incValue;
        }).when(result).incrementInCache(anyString(), anyLong(), any());

        doAnswer(invocation -> {
            cache.remove(invocation.getArguments()[0]);
            return null;
        }).when(result).deleteFromCache(anyString());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return true;
        }).when(result).putInCache(anyString(), anyObject(), anyObject());

        doAnswer(invocation -> {
            String key = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            cache.put(key, value);
            return true;
        }).when(result).putInCache(anyString(), anyObject());
    }

    public MdsS3Client getMdsS3Client() throws IOException {
        final Map<String, byte[]> cache = Collections.synchronizedMap(new HashMap<>());

        MdsS3Client client = mock(MdsS3Client.class);
        doAnswer(
                invocation -> {
                    ResourceLocation location = (ResourceLocation) invocation.getArguments()[0];
                    ContentProvider provider = (ContentProvider) invocation.getArguments()[1];
                    AssetiveInputStream is = (AssetiveInputStream) provider.getInputStream();
                    /**
                     * вычитываем, чтобы заполнить {@link AssetiveInputStream#read}
                     */
                    byte[] body = IOUtils.readInputStreamToBytes(is);
                    cache.put(location.getKey(), body);
                    return 1; // что угодно отличное от -1
                }
        ).when(client).upload(anyObject(), anyObject());

        doAnswer(invocation -> {
            ResourceLocation location = (ResourceLocation) invocation.getArguments()[0];
            return ((ContentConsumer) invocation.getArguments()[1]).consume(new ByteArrayInputStream(cache.get(location.getKey())));
        }).when(client).download(anyObject(), anyObject());
        return client;
    }

    public SupplierInfoService getSupplierInfoService() {
        SupplierInfoService client = mock(SupplierInfoService.class);
        SupplierInfo supplier = new SupplierInfo();
        supplier.setSupplierName("Supplier-test-name");
        doReturn(Optional.of(supplier)).when(client).getShopInfo(anyLong());
        return client;
    }

    public static MbiApiClient getMbiApiClientMock() {
        return mock(MbiApiClient.class);
    }

    public static DealerApiClient getDealerApiClientMock() {
        return mock(DealerApiClient.class);
    }
}
