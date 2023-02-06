package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfCodesSearcherTest extends BaseTest {
    private static final Long MONTH_FOR_SEARCH = 1L;
    private static final Long CHECKOUTER_ID = 0L;
    private static final Long ORDER_DELIVERY_SERVICE_ID = 123L;
    private static final Long ORDER_ID = 234L;
    private static final Double ORDER_TOTAL = 123.00;
    private static final List<String> CODES_TO_SEARCH = Arrays.asList("test", "111021", "test2");

    @InjectMocks
    private DbfCodesSearcher dbfCodesSearcher;

    @Mock
    private CheckouterClient checkouterClient;

    private static Stream<Arguments> getParameters() {
        return Stream.of(
            Arguments.of(List.of("test1", "test2", "test3")), //partial
            Arguments.of(CODES_TO_SEARCH), // true
            Arguments.of(List.of("testdh", "testgd", "testsd")) // false
        );
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Class<?> dbfCodesSearcherClass = dbfCodesSearcher.getClass();
        Field searchForMonths = dbfCodesSearcherClass.getDeclaredField("searchForMonths");
        Field checkouterClientId = dbfCodesSearcherClass.getDeclaredField("checkouterClientId");
        searchForMonths.setAccessible(true);
        checkouterClientId.setAccessible(true);
        searchForMonths.set(dbfCodesSearcher, MONTH_FOR_SEARCH);
        checkouterClientId.set(dbfCodesSearcher, CHECKOUTER_ID);
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void search(List<String> searchedCode) {
        when(checkouterClient.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
            .thenReturn(getPagedOrders(searchedCode));
        List<DbfCode> codes = dbfCodesSearcher.search(getCodes());

        Set<String> list = CollectionUtils.intersect(searchedCode, CODES_TO_SEARCH);

        // создаём один заказ, внутри может быть несколько кодов в нём и...
        // если их ищем, то и должны обрабатывать
        softly.assertThat(codes).hasSize(list.size());

        if (!list.isEmpty()) {
            codes.forEach(c -> {
                softly.assertThat(c.getOrderId()).isEqualTo(ORDER_ID.toString());
                softly.assertThat(c.getServiceId()).isEqualTo(ORDER_DELIVERY_SERVICE_ID.intValue());
                softly.assertThat(c.getOrderSum()).isEqualTo(ORDER_TOTAL);
                softly.assertThat(searchedCode.contains(c.getCode())).isTrue();
            });
        }
    }

    private List<DbfCode> getCodes() {
        return CODES_TO_SEARCH.stream().map(c -> {
            DbfCode code = new DbfCode();
            code.setCode(c);
            return code;
        }).collect(Collectors.toList());
    }

    private PagedOrders getPagedOrders(List<String> searchedCode) {
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(Collections.singletonList(getOrder(searchedCode)));
        return pagedOrders;
    }

    private Order getOrder(List<String> searchedCode) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setTotal(BigDecimal.valueOf(ORDER_TOTAL));

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(ORDER_DELIVERY_SERVICE_ID);
        Parcel parcel = new Parcel();

        parcel.setTracks(
            searchedCode
                .stream()
                .map(s -> new Track(s, ORDER_DELIVERY_SERVICE_ID))
                .collect(Collectors.toList())
        );
        delivery.setParcels(Collections.singletonList(parcel));
        order.setDelivery(delivery);
        return order;
    }
}
