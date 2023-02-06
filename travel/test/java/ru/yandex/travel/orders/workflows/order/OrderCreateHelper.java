package ru.yandex.travel.orders.workflows.order;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Value;
import wiremock.org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.TJson;
import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.orders.PrettyIdHelper;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.entities.AeroflotOrder;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.FxRate;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.grpc.helpers.OrderCreator;
import ru.yandex.travel.orders.workflow.hotels.expedia.proto.EExpediaItemState;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.orders.workflow.order.aeroflot.proto.EAeroflotOrderState;

public class OrderCreateHelper {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static HotelOrder createTestHotelOrder() {
        UUID uuid = UUID.randomUUID();
        HotelOrder hotelOrder = new HotelOrder();
        hotelOrder.setDisplayType(EDisplayOrderType.DT_HOTEL);
        hotelOrder.setId(uuid);
        hotelOrder.setPrettyId(PrettyIdHelper.makePrettyId(uuid.getMostSignificantBits()));
        hotelOrder.setDeduplicationKey(uuid);
        hotelOrder.setState(EHotelOrderState.OS_NEW);
        hotelOrder.setCurrency(ProtoCurrencyUnit.RUB);
        return hotelOrder;
    }

    public static AeroflotOrder createTestAeroflotOrder(EAeroflotOrderState state) {
        UUID uuid = UUID.randomUUID();
        AeroflotOrder aeroflotOrder = new AeroflotOrder();
        aeroflotOrder.setDisplayType(EDisplayOrderType.DT_AVIA);
        aeroflotOrder.setId(uuid);
        aeroflotOrder.setPrettyId(PrettyIdHelper.makePrettyId(uuid.getMostSignificantBits()));
        aeroflotOrder.setDeduplicationKey(uuid);
        aeroflotOrder.setState(state);
        return aeroflotOrder;
    }

    public static HotelOrder createTestHotelOrder(EHotelOrderState orderWorkflowState, UUID serviceId,
                                                  EExpediaItemState serviceState) {
        return createTestHotelOrder(orderWorkflowState, new ServiceDefinition(serviceId, serviceState));
    }

    public static HotelOrder createTestHotelOrder(EHotelOrderState orderWorkflowState,
                                                  ServiceDefinition... serviceDefinitions) {
        HotelOrder testOrder = createTestHotelOrder();
        testOrder.setCurrency(ProtoCurrencyUnit.RUB);
        FxRate fxRate = new FxRate();
        fxRate.putIfAbsent(ECurrency.C_USD, BigDecimal.valueOf(5000, 2));
        testOrder.setFxRate(fxRate);
        TJson json = createRandomJsonProto(10);
        for (ServiceDefinition definition : serviceDefinitions) {
            ExpediaOrderItem orderItem = (ExpediaOrderItem) OrderCreator.addOrderItem(testOrder,
                    EServiceType.PT_EXPEDIA_HOTEL, json, false);
            orderItem.setState(definition.getServiceState());
            orderItem.setId(definition.getServiceId());
        }
        testOrder.setState(orderWorkflowState);
        return testOrder;
    }

    public static Object createRandomExpediaItinerary(int numberOfCharacters) {
        ExpediaHotelItinerary itinerary = new ExpediaHotelItinerary();
        itinerary.setCustomerSessionId(RandomStringUtils.random(numberOfCharacters));
        itinerary.setExpediaReservationToken(RandomStringUtils.random(numberOfCharacters));
        return itinerary;
    }

    public static JsonNode createRandomJson(int numberOfFields) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        for (int i = numberOfFields; i > 0; i--) {
            objectNode.put(RandomStringUtils.random(10), RandomStringUtils.random(10));
        }
        return objectNode;
    }

    public static TJson createRandomJsonProto(int numberOfCharacters) {
        try {
            return TJson.newBuilder()
                    .setValue(objectMapper.writer().writeValueAsString(createRandomExpediaItinerary(numberOfCharacters))).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Value
    public static final class ServiceDefinition {
        private final UUID serviceId;
        private EExpediaItemState serviceState;
    }
}
