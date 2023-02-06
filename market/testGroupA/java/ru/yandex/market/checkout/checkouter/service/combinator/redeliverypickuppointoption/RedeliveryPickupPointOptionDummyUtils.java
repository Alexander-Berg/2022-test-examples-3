package ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption;

import java.io.IOException;
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
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.TimeDto;

public final class RedeliveryPickupPointOptionDummyUtils {

    private RedeliveryPickupPointOptionDummyUtils() {
    }

    public static String getRequestAsJson() throws IOException {
        return IOUtils.readInputStream(Objects.requireNonNull(RedeliveryPickupPointOptionDummyUtils.class
                .getResourceAsStream("/json/redeliveryPickupPointOptionRequest.json")));
    }

    public static RedeliveryPickupPointOptionRequest getRequest() {
        return new RedeliveryPickupPointOptionRequest("100500", "200400", "factor1=value1;factor2=value2");
    }

    public static String getResponseAsJson() throws IOException {
        return IOUtils.readInputStream(Objects.requireNonNull(RedeliveryPickupPointOptionDummyUtils.class
                .getResourceAsStream("/json/redeliveryPickupPointOptionResponse.json")));
    }

    public static RedeliveryPickupPointOptionResponse getResponse() {
        var option = new DeliveryOptionDto();
        option.setDateFrom(new DateDto(27, 4, 2022));
        option.setDateTo(new DateDto(27, 4, 2022));
        option.setInterval(new IntervalDto(
                new TimeDto(9, 1),
                new TimeDto(22, 1))
        );
        option.setDeliveryServiceId(1005432L);
        option.setPaymentMethods(List.of(
                CombinatorPaymentMethod.PREPAYMENT,
                CombinatorPaymentMethod.CASH,
                CombinatorPaymentMethod.CARD)
        );
        option.setDeliverySubtype(DeliverySubtype.ORDINARY);
        option.setDeliveryType(CombinatorDeliveryType.PICKUP);

        return new RedeliveryPickupPointOptionResponse(
                getDestination(),
                List.of(option)
        );
    }

    private static DeliveryDestinationDto getDestination() {
        var destination = new DeliveryDestinationDto();
        destination.setLogisticPointId("200400");
        destination.setRegionId(10958L);
        destination.setGpsCoords(new GpsCoordinatesDto(48.687437, 43.540408));
        return destination;
    }
}
