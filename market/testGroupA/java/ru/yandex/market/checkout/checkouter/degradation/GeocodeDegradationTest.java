package ru.yandex.market.checkout.checkouter.degradation;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryAddressMultiCartFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderDeliveryAddressMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.MultiCartFlowFactory;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableGeocoderClientDecorator;
import ru.yandex.market.checkout.checkouter.degradation.strategy.GeocodeDegradationStrategy;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.geo.GeobaseService;
import ru.yandex.market.checkout.checkouter.geo.GeocodeMemCacheStorageService;
import ru.yandex.market.checkout.checkouter.geo.GeocodeProvider;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.transliterate.TransliterateService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.degradation.DegradationStage.CHECKOUT;

@ContextConfiguration(classes = {
        GeocodeProvider.class,
        DeliveryAddressMultiCartFetcher.class,
        OrderDeliveryAddressMutation.class,
        GeocodeDegradationStrategy.class
})
@MockBean(classes = {
        GeobaseService.class,
        GeoRegionService.class,
        TransliterateService.class,
        GeocodeMemCacheStorageService.class,
        PersonalDataService.class
})
@TestPropertySource(properties = {
        "market.checkout.managed-degradation.geocode.find.timeout=1000"
})
public class GeocodeDegradationTest extends AbstractDegradationTest {

    @Autowired
    private DegradationContextFactory factory;
    @Autowired
    private GeocodeDegradationStrategy strategy;
    @Autowired
    private CheckouterProperties properties;
    @Autowired
    private DegradableGeocoderClientDecorator geoClient;
    @Autowired
    private DeliveryAddressMultiCartFetcher addressFetcher;
    @Autowired
    private OrderDeliveryAddressMutation addressMutation;
    @Autowired
    private PersonalDataService personalDataService;

    @BeforeEach
    void init() {
        log.addAppender(appender);
        appender.clear();
        appender.start();
        when(properties.getEnableGeocodeDegradationStrategy()).thenReturn(true);
        when(properties.getSplitAddressAndRegionInGeocoderRequest()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(appender);
    }

    @Test
    void onErrorDeliveryAddressTest() {
        AddressImpl address = new AddressImpl();
        address.setApartment("1");
        address.setRecipientEmail("a@a.ru");
        address.setRecipientPerson(new RecipientPerson("firstname", "middlename", "lastname"));

        Order order = OrderProvider.orderBuilder()
                .item(OrderItemProvider.defaultOrderItem())
                .delivery(DeliveryProvider.deliveryBuilder()
                        .buyerAddress(address)
                        .build())
                .build();

        GeoClient client = geoClient.getGeoClient();
        when(client.find(any(String.class), any(GeoSearchParams.class)))
                .thenThrow(new RuntimeException("some exception"));
        PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());
        when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

        when(personalDataService.getPersAddress(any())).thenReturn(persAddress);

        DegradationContextHolder.setStage(CHECKOUT);

        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());

        MultiCart multiCart = new MultiCart();
        multiCart.setCarts(List.of(order));

        MultiCartFetchingContext fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);

        assertDoesNotThrow(() -> MultiCartFlowFactory.fetch(addressFetcher)
                .apply(fetchingContext));

        assertOnErrorLog(strategy.getCallName());
    }

    @Test
    void onTimeoutDeliveryAddressTest() {
        AddressImpl address = new AddressImpl();
        address.setApartment("1");
        address.setRecipientEmail("a@a.ru");
        address.setRecipientPerson(new RecipientPerson("firstname", "middlename", "lastname"));

        Order order = OrderProvider.orderBuilder()
                .item(OrderItemProvider.defaultOrderItem())
                .delivery(DeliveryProvider.deliveryBuilder()
                        .buyerAddress(address)
                        .build())
                .build();

        PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());
        when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

        when(personalDataService.getPersAddress(any())).thenReturn(persAddress);

        GeoClient client = geoClient.getGeoClient();
        when(client.find(any(String.class), any(GeoSearchParams.class))).then(invocation -> {
            Thread.sleep(2000);
            return null;
        });

        DegradationContextHolder.setStage(CHECKOUT);

        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());

        MultiCart multiCart = new MultiCart();
        multiCart.setCarts(List.of(order));

        MultiCartFetchingContext fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);

        assertDoesNotThrow(() -> MultiCartFlowFactory.fetch(addressFetcher)
                .apply(fetchingContext));

        assertOnTimeoutLog(strategy.getCallName());
    }
}
