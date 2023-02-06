package ru.yandex.market.checkout.checkouter.service.combinator.postponedelivery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorDeliveryType;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorPaymentMethod;
import ru.yandex.market.checkout.checkouter.service.combinator.DeliverySubtype;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DateDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DeliveryDestinationDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DeliveryOptionDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.GpsCoordinatesDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.IntervalDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.OfferDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.OrderItemDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.TimeDto;

public final class PostponeDeliveryDummyUtils {

    private PostponeDeliveryDummyUtils() {
    }

    public static String getRequestAsJson() throws IOException {
        return IOUtils.readInputStream(Objects.requireNonNull(PostponeDeliveryDummyUtils.class
                .getResourceAsStream("/json/postponeDeliveryRequest.json")));
    }

    public static PostponeDeliveryRequest getRequest() {
        var items = new ArrayList<OrderItemDto>();
        var item = new OrderItemDto();
        var offer = new OfferDto();
        item.setRequiredCount(2);
        item.setWeight(10000L);
        item.setDimensions(new Long[]{15L, 20L, 20L});
        offer.setAvailableCount(2);
        item.setAvailableOffers(List.of(offer));
        items.add(item);

        item = new OrderItemDto();
        offer = new OfferDto();
        item.setRequiredCount(4);
        item.setWeight(25000L);
        item.setDimensions(new Long[]{30L, 50L, 50L});
        item.setCargoTypes(new Integer[]{300, 310});
        offer.setAvailableCount(4);
        item.setAvailableOffers(List.of(offer));
        items.add(item);

        var request = new PostponeDeliveryRequest();
        request.setItems(items);
        request.setDestination(getDestination());
        request.setDeliveryServiceId(322L);
        request.setDateFrom(new DateDto(11, 11, 2021));
        request.setDateTo(new DateDto(13, 11, 2021));
        request.setInterval(new IntervalDto(
                new TimeDto(10, 0),
                new TimeDto(14, 0))
        );
        request.setRearrFactors("use_some_feature=1;use_other_feature=0");
        request.setDeliveryType(CombinatorDeliveryType.COURIER);
        request.setDeliverySubtype(DeliverySubtype.DEFERRED_COURIER);
        request.setOrderId("1234567");
        request.setRouteString("{\"this\":\"is\",\"route\":111}");
        return request;
    }

    public static String getResponseAsJson() throws IOException {
        return IOUtils.readInputStream(Objects.requireNonNull(PostponeDeliveryDummyUtils.class
                .getResourceAsStream("/json/postponeDeliveryResponse.json")));
    }

    public static PostponeDeliveryResponse getResponse() {
        var options = new ArrayList<DeliveryOptionDto>();
        var option = new DeliveryOptionDto();
        option.setDeliveryServiceId(322L);
        option.setDeliveryType(CombinatorDeliveryType.COURIER);
        option.setDeliverySubtype(DeliverySubtype.DEFERRED_COURIER);
        option.setPaymentMethods(List.of(
                CombinatorPaymentMethod.CASH,
                CombinatorPaymentMethod.CARD,
                CombinatorPaymentMethod.PREPAYMENT)
        );
        option.setDateFrom(new DateDto(12, 11, 2021));
        option.setDateTo(new DateDto(14, 11, 2021));
        option.setInterval(new IntervalDto(
                new TimeDto(10, 1),
                new TimeDto(14, 1))
        );
        options.add(option);

        option = new DeliveryOptionDto();
        option.setDeliveryServiceId(32055L);
        option.setDeliveryType(CombinatorDeliveryType.COURIER);
        option.setDeliverySubtype(DeliverySubtype.ON_DEMAND);
        option.setPaymentMethods(List.of(
                CombinatorPaymentMethod.CASH,
                CombinatorPaymentMethod.PREPAYMENT)
        );
        option.setDateFrom(new DateDto(1, 12, 2021));
        option.setDateTo(new DateDto(31, 12, 2021));
        option.setInterval(new IntervalDto(
                new TimeDto(11, 20),
                new TimeDto(15, 20))
        );
        options.add(option);

        option = new DeliveryOptionDto();
        option.setDeliveryServiceId(512L);
        option.setDeliveryType(CombinatorDeliveryType.PICKUP);
        option.setDateFrom(new DateDto(1, 1, 2022));
        option.setDateTo(new DateDto(15, 1, 2022));
        option.setInterval(new IntervalDto(
                new TimeDto(10, 3),
                new TimeDto(14, 59))
        );
        options.add(option);

        option = new DeliveryOptionDto();
        option.setDeliveryServiceId(999L);
        option.setDeliveryType(CombinatorDeliveryType.POST);
        option.setPaymentMethods(List.of(
                CombinatorPaymentMethod.PREPAYMENT)
        );
        option.setDateFrom(new DateDto(13, 11, 2021));
        option.setDateTo(new DateDto(15, 11, 2021));
        option.setInterval(new IntervalDto(
                new TimeDto(11, null),
                new TimeDto(15, null))
        );
        options.add(option);

        var response = new PostponeDeliveryResponse();
        response.setDestination(getDestination());
        response.setOptions(options);
        return response;
    }

    private static DeliveryDestinationDto getDestination() {
        var destination = new DeliveryDestinationDto();
        destination.setLogisticPointId("10000123123");
        destination.setRegionId(120539L);
        destination.setGpsCoords(new GpsCoordinatesDto(55.70417927289191, 37.90647953857668));
        return destination;
    }
}
