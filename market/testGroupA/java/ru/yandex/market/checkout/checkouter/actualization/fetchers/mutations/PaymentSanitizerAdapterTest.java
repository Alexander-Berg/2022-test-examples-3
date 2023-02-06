package ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.actualization.services.PaymentOptionsService;
import ru.yandex.market.checkout.checkouter.actualization.services.PostpaidMlDeciderService;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.shop.SupplierMetaDataService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.market.checkout.checkouter.mocks.Mocks.createMock;

/**
 * @author asafev
 * on 08/09/2017
 */
public class PaymentSanitizerAdapterTest {

    private PaymentOptionsMutation mutation;
    @Autowired
    private PaymentOptionsService paymentOptionsService;
    @Autowired
    private CheckouterProperties checkouterProperties;
    @Autowired
    private CheckouterFeatureReader checkouterFeatureReader;
    @Autowired
    private GeoRegionService geoRegionService;
    @Autowired
    private ColorConfig colorConfig;
    @Autowired
    private PostpaidMlDeciderService postpaidMlDeciderService;
    @Autowired
    private NoSideEffectUserService noSideEffectUserService;

    public static Stream<Arguments> parameterizedTestData() {
        return Arrays.stream(new Object[][]{
                {100L, false, Collections.singletonList(createItem("1", 100L)), true},
                {101L, false, Collections.singletonList(createItem("1", 100L)), false},
                {102L, false, Collections.singletonList(createItem("1", 100L)), false},
                {100L, true, Collections.singletonList(createItem("1", 100L)), true},
                {100L, true, Collections.singletonList(createItem("1", 101L)), false},
                {100L, true, Arrays.asList(createItem("1", 100L), createItem("1", 101L)), false},
                {100L, true, Arrays.asList(createItem("1", 102L), createItem("1", 101L)), false}
        }).map(Arguments::of);
    }

    @BeforeEach
    public void initActualizer() {
        ShopService shopService = createMock(ShopService.class);
        mutation = new PaymentOptionsMutation(new SupplierMetaDataService(shopService, 0),
                shopService,
                colorConfig,
                checkouterFeatureReader,
                paymentOptionsService,
                checkouterProperties,
                geoRegionService,
                postpaidMlDeciderService,
                noSideEffectUserService
        );
        Mockito.doReturn(ShopMetaDataBuilder.createTestDefault()
                        .withCampaiginId(1L)
                        .withClientId(1L)
                        .withSandboxClass(PaymentClass.YANDEX)
                        .withProdClass(PaymentClass.YANDEX)
                        .withPrepayType(PrepayType.YANDEX_MARKET)
                        .build())
                .when(shopService)
                .getMeta(100L, ShopMetaData.DEFAULT);
        Mockito.doReturn(ShopMetaDataBuilder.createTestDefault()
                        .withCampaiginId(1L)
                        .withClientId(1L)
                        .withSandboxClass(PaymentClass.SHOP)
                        .withProdClass(PaymentClass.SHOP)
                        .withPrepayType(PrepayType.UNKNOWN)
                        .build())
                .when(shopService)
                .getMeta(101L, ShopMetaData.DEFAULT);
        Mockito.doReturn(ShopMetaData.DEFAULT)
                .when(shopService)
                .getMeta(102L, ShopMetaData.DEFAULT);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void isYandexMarketPrepayEnabledTest(Long shopId, boolean isFulfilment, List<OrderItem> items,
                                                boolean actualizerResult) {
        Order order = new Order();
        order.setFulfilment(isFulfilment);
        order.setShopId(shopId);
        order.setItems(items);

        assertThat(mutation.isYandexMarketPrepayEnabled(order), equalTo(actualizerResult));
    }

    private static OrderItem createItem(String id, Long ffShopId) {
        OrderItem item = new OrderItem();
        item.setSupplierId(ffShopId);
        item.setFeedOfferId(new FeedOfferId(id, 1L));
        return item;
    }
}
