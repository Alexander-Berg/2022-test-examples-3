package ru.yandex.travel.orders.mocks;

import java.time.LocalDateTime;
import java.util.UUID;

import org.javamoney.moneta.Money;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotServicePayload;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.booking.ClientInfo;
import ru.yandex.avia.booking.partners.gateways.model.booking.TravellerInfo;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.AeroflotOrder;
import ru.yandex.travel.orders.entities.AeroflotOrderItem;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Feel free to fill more test data or add more complex test data builders.
 * Current mock contain minimal data required for the using code.
 */
public class AeroflotMocks {
    public static AeroflotServicePayload testPayload() {
        return AeroflotServicePayload.builder()
                .variant(new AeroflotVariant().setOffer(AeroflotTotalOffer.builder()
                        .id("total_offer_id")
                        .totalPrice(Money.of(10, "RUB"))
                        .disclosureUrl("some url")
                        .categoryOffers(emptyList())
                        .segmentRefs(emptyList())
                        .build()))
                .partnerId("partnerId")
                .travellers(singletonList(TravellerInfo.builder().build()))
                .preliminaryCost(Money.of(10, ProtoCurrencyUnit.RUB))
                .clientInfo(ClientInfo.builder()
                        .phone("phone")
                        .email("email")
                        .userIp("ip")
                        .userAgent("user-agent")
                        .build())
                .build();
    }

    public static AeroflotOrderItem testOrderItem() {
        AeroflotOrderItem item = new AeroflotOrderItem() {
            @Override
            public LocalDateTime getServicedAt() {
                // meaningless prop
                return LocalDateTime.now();
            }
        };
        item.setPayload(AeroflotMocks.testPayload());
        return item;
    }

    public static AeroflotOrder testOrder() {
        AeroflotOrder order = new AeroflotOrder();
        order.setId(UUID.randomUUID());
        order.addOrderItem(testOrderItem());
        order.setEmail(order.getAeroflotOrderItem().getPayload().getClientInfo().getEmail());
        return order;
    }
}
