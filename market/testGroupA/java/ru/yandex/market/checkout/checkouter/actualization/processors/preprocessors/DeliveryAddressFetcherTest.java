package ru.yandex.market.checkout.checkouter.actualization.processors.preprocessors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryAddressFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryAddressMultiCartFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderDeliveryAddressMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.MultiCartFlowFactory;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableGeocoderClientDecorator;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.geo.GeocodeMemCacheStorageService;
import ru.yandex.market.checkout.checkouter.geo.GeocodeProvider;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataStoreResult;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.transliterate.TransliterateService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author zagidullinri
 * @date 30.07.2021
 */
@ExtendWith(MockitoExtension.class)
@MockBean(classes = {DegradableGeocoderClientDecorator.class,
        GeocodeMemCacheStorageService.class,
        TransliterateService.class,
        CheckouterProperties.class})
public class DeliveryAddressFetcherTest {


    @Mock
    private DegradableGeocoderClientDecorator geoClient;
    @Mock
    private GeocodeMemCacheStorageService geocodeMemCacheStorageService;
    @Mock
    private TransliterateService transliterateService;
    @Mock
    private CheckouterProperties checkouterProperties;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @InjectMocks
    private GeocodeProvider geocodeProvider;
    private DeliveryAddressMultiCartFetcher deliveryAddressMultiCartFetcher;
    private DeliveryAddressFetcher deliveryAddressFetcher;
    private OrderDeliveryAddressMutation addressMutation;
    @Mock
    private PersonalDataService personalDataService;

    @ParameterizedTest
    @CsvSource({"true,EXACT",
            "true,NUMBER",
            "true,NEAR",
            "false,RANGE",
            "false,STREET",
            "false,OTHER",
            "false,UNKNOWN",
            "false,ALL"})
    public void shouldUpdateGpsWhenPrecisionIsOk(boolean shouldOverrideGps, Precision precision) {
        deliveryAddressMultiCartFetcher = new DeliveryAddressMultiCartFetcher(
                geocodeProvider,
                null,
                null,
                checkouterFeatureReader,
                Executors.newSingleThreadExecutor(),
                personalDataService);
        deliveryAddressFetcher = new DeliveryAddressFetcher(personalDataService);

        addressMutation = new OrderDeliveryAddressMutation(transliterateService, personalDataService);

        when(checkouterProperties.getSplitAddressAndRegionInGeocoderRequest()).thenReturn(true);
        String point = "38.175903 56.04690174";
        List<GeoObject> geoObjects = Collections.singletonList(buildGeoObject(precision, point));
        when(geoClient.find(anyString(), any())).thenReturn(geoObjects);
        Order order = OrderProvider.getBlueOrder();

        PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());
        when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null,
                        persAddress, null));

        when(personalDataService.getPersAddress(any())).thenReturn(persAddress);

        if (shouldOverrideGps) {
            when(personalDataService.store(any()))
                    .thenReturn(new PersonalDataStoreResult(null, null, null, null, null));
        }
        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());

        MultiCart multiCart = new MultiCart();
        multiCart.setCarts(List.of(order));

        MultiCartFetchingContext fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);

        MultiCartFlowFactory.fetch(deliveryAddressMultiCartFetcher)
                .apply(fetchingContext);

        fetchingContext.getMultiCartContext().setDeliveryAddressRegionPreciseStage(deliveryAddressMultiCartFetcher);

        ImmutableMultiCartContext immutableMultiCartContext = ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.single(order));

        ActualizationContext actualizationContext =
                ActualizationContext.builder()
                        .withImmutableMulticartContext(immutableMultiCartContext)
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(order.getBuyerCurrency())
                        .build();

        assertNull(order.getDelivery().getBuyerAddress().getGps());
        addressMutation.prepareDeliveryDates(order, deliveryAddressFetcher.fetch(
                ImmutableActualizationContext.of(actualizationContext)));
        assertEquals(shouldOverrideGps, order.getDelivery().getBuyerAddress().getGps() != null);
    }

    private GeoObject buildGeoObject(Precision precision, String gps) {
        AddressInfo addressInfo = AddressInfo.newBuilder()
                .withAreaInfo(AreaInfo.newBuilder().build())
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .withCountryInfo(CountryInfo.newBuilder().build())
                .build();
        ToponymInfo toponymInfo = ToponymInfo.newBuilder()
                .withPoint(gps)
                .withPrecision(precision)
                .build();
        SimpleGeoObject.Builder builder = SimpleGeoObject.newBuilder()
                .withToponymInfo(toponymInfo)
                .withBoundary(Boundary.newBuilder().build())
                .withAddressInfo(addressInfo);
        return builder.build();
    }

}
