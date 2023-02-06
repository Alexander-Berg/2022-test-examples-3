package ru.yandex.market.fulfillment.wrap.marschroute.service.order.request.factory.delivery.self;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.fulfillment.wrap.core.transformer.FulfillmentModelTransformer;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.DeliveryServiceMeta;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteCustomer;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschrouteDeliveryCityService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.delivery.DeliveryServiceMetaProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.fulfillment.wrap.marschroute.service.order.request.CreateOrderRequestContainer;
import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.wrap.marschroute.service.order.request.factory.delivery.self.CreateMarketDeliveryOrderRequestFactory.DEFAULT_FALLBACK_LOCATION_ID;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateMarketDeliveryOrderRequestFactoryTest {

    private static final String YANDEX_ID = "YANDEX_ID";
    private static final String DELIVERY_ID = "DELIVERY_ID";
    private static final String KLADR_ID = "kladrId";
    private static final String KLADR_ID_MARSCHROUTE = "kladrIdMarschroute";
    private static final String KLADR_ID_MARSCHROUTE_SHORT = KLADR_ID_MARSCHROUTE.substring(0, 11);

    @Mock
    private FulfillmentModelTransformer modelTransformer;

    @Mock
    private DeliveryServiceMetaProvider deliveryServiceMetaProvider;

    @Mock
    private MarschrouteDeliveryCityService deliveryCityService;

    @Mock
    private GeoInformationProvider geoInformationProvider;

    @InjectMocks
    private CreateMarketDeliveryOrderRequestFactory requestFactory;

    private final Order order = createOrder();
    private final MarschrouteCustomer marschrouteCustomer = new MarschrouteCustomer();
    private final MarschrouteItem[] items = new MarschrouteItem[]{new MarschrouteItem()};
    private final MarschrouteDate date = MarschrouteDate.create(LocalDate.now());
    private final DeliveryServiceMeta deliveryServiceMeta = createDeliveryServiceMeta();

    private MarschrouteOrder marschrouteOrder;

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                KLADR_ID,
                "",
                false,
                new GeoInformation(1L, "fiasid", KLADR_ID, "type", null)
            ),
            Arguments.of(
                KLADR_ID,
                "SOME_ID",
                true,
                new GeoInformation(1L, "fiasid", KLADR_ID, "type", null)
            ),
            Arguments.of(
                "SOME_ID",
                "SOME_ID",
                false,
                new GeoInformation(1L, "fiasid", KLADR_ID, "type", null)
            ),
            Arguments.of(
                KLADR_ID_MARSCHROUTE_SHORT,
                "",
                false,
                new GeoInformation(1L, "fiasid", KLADR_ID, KLADR_ID_MARSCHROUTE, null, "type", null)
            ),
            Arguments.of(
                KLADR_ID_MARSCHROUTE_SHORT,
                "SOME_ID",
                true,
                new GeoInformation(1L, "fiasid", KLADR_ID, KLADR_ID_MARSCHROUTE, null, "type", null)
            ),
            Arguments.of(
                "SOME_ID",
                "SOME_ID",
                false,
                new GeoInformation(1L, "fiasid", KLADR_ID, KLADR_ID_MARSCHROUTE, null, "type", null)
            ),
            Arguments.of(
                KLADR_ID,
                "",
                false,
                new GeoInformation(1L, "fiasid", KLADR_ID, "", null, "type", null)
            ),
            Arguments.of(
                KLADR_ID,
                "SOME_ID",
                true,
                new GeoInformation(1L, "fiasid", KLADR_ID, "", null, "type", null)
            ),
            Arguments.of(
                "SOME_ID",
                "SOME_ID",
                false,
                new GeoInformation(1L, "fiasid", KLADR_ID, "", null, "type", null)
            )
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void createMarschrouteRequest(
        String expectedKladr,
        String givenCityId,
        boolean cityUnknown,
        GeoInformation geoInformation
    ) {
        marschrouteOrder = getMarschrouteOrder(givenCityId);
        setUp(givenCityId, cityUnknown, geoInformation);

        CreateOrderRequestContainer marschrouteRequest = requestFactory.createMarschrouteRequest(order);
        assertEquals(expectedKladr, marschrouteRequest.getRequest().getOrder().getLocation().getCityId());

        tearDown(givenCityId, cityUnknown);
    }

    private void setUp(String givenCityId, boolean cityUnknown, GeoInformation geoInformation) {
        when(modelTransformer.transform(order, MarschrouteOrder.class)).thenReturn(marschrouteOrder);
        when(modelTransformer.transform(order, MarschrouteCustomer.class)).thenReturn(marschrouteCustomer);
        when(modelTransformer.transform(order.getItems(), MarschrouteItem[].class)).thenReturn(items);
        when(modelTransformer.transform(order.getShipmentDate(), MarschrouteDate.class)).thenReturn(date);
        when(deliveryServiceMetaProvider.provide(DELIVERY_ID)).thenReturn(deliveryServiceMeta);
        if (!Strings.isNullOrEmpty(givenCityId)) {
            when(deliveryCityService.isCityIdUnknown(givenCityId)).thenReturn(cityUnknown);
        }
        if ((Strings.isNullOrEmpty(givenCityId) || cityUnknown)) {
            when(geoInformationProvider.findWithKladr(DEFAULT_FALLBACK_LOCATION_ID)).thenReturn(Optional.of(geoInformation));
        }
    }

    private void tearDown(String givenCityId, boolean cityUnknown) {
        verify(modelTransformer, atLeastOnce()).transform(any(), any());
        verify(modelTransformer, atLeastOnce()).transform(order, MarschrouteCustomer.class);
        verify(modelTransformer, atLeastOnce()).transformFromListToList(order.getItems(), MarschrouteItem.class);
        verify(modelTransformer, atLeastOnce()).transform(order.getShipmentDate(), MarschrouteDate.class);
        verify(deliveryServiceMetaProvider, atLeastOnce()).provide(DELIVERY_ID);
        if (!Strings.isNullOrEmpty(givenCityId)) {
            verify(deliveryCityService, atLeastOnce()).isCityIdUnknown(givenCityId);
        }
        if ((Strings.isNullOrEmpty(givenCityId) || cityUnknown)) {
            verify(geoInformationProvider, atLeastOnce()).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);
        }
        Stream.of(modelTransformer, deliveryServiceMetaProvider, deliveryCityService, geoInformationProvider)
            .forEach(Mockito::verifyNoMoreInteractions);
    }

    private MarschrouteOrder getMarschrouteOrder(String cityId) {
        MarschrouteOrder marschrouteOrder = new MarschrouteOrder();
        marschrouteOrder.setLocation(new MarschrouteLocation());
        marschrouteOrder.getLocation().setCityId(cityId);
        return marschrouteOrder;
    }

    private Order createOrder() {
        ResourceId deliveryId = new ResourceId(DELIVERY_ID, null);
        ResourceId externalId = new ResourceId(YANDEX_ID, null);
        return new Order.OrderBuilder(
            null,
            null,
            Collections.emptyList(),
            null,
            null,
            null,
            null,
            new Delivery(deliveryId, null, null, null, null, null, null, null),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ).setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.now()))
            .setExternalId(externalId)
            .build();
    }

    private DeliveryServiceMeta createDeliveryServiceMeta() {
        DeliveryServiceMeta meta = new DeliveryServiceMeta();
        meta.setLegalEntityName("LEGAL");
        meta.setId("51");
        return meta;
    }
}
