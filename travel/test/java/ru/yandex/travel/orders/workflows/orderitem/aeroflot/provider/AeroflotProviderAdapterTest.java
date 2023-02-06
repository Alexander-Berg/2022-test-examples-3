package ru.yandex.travel.orders.workflows.orderitem.aeroflot.provider;

import java.util.List;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.avia.booking.partners.gateways.BookingTestingScenarios;
import ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotGateway;
import ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotProviderProperties;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotAnonymousTraveller;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotServicePayload;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.availability.VariantNotAvailableException;
import ru.yandex.travel.orders.mocks.AeroflotMocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AeroflotProviderAdapterTest {
    private final AeroflotGateway gateway = Mockito.mock(AeroflotGateway.class);
    private final AeroflotProviderProperties properties = new AeroflotProviderProperties();
    private final AeroflotServiceAdapter adapter = new AeroflotServiceAdapter(gateway, properties);

    @Before
    public void initTest() {
        properties.setEnableTestingScenarios(true);
    }

    @Test
    public void checkAvailabilityPriceChanged() {
        Money sameTotal = Money.of(20, "RUB");
        when(gateway.checkAvailabilitySingle(any())).thenReturn(AeroflotVariant.builder()
                .travellers(List.of(new AeroflotAnonymousTraveller("T1", "ADT", 1)))
                .offer(AeroflotTotalOffer.builder().totalPrice(sameTotal).build())
                .build());
        assertThatExceptionOfType(AeroflotOfferPriceChangedException.class).isThrownBy(() -> {
            AeroflotServicePayload payload = testingScenarioPayload(BookingTestingScenarios.BOOKING_NEW_PRICE);
            assertThat(payload.getVariant().getOffer().getTotalPrice()).isNotEqualTo(sameTotal);
            adapter.checkAvailability(UUID.randomUUID(), payload, null);
        }).satisfies(e -> assertThat(e.getNewOffer().getTotalPrice()).isEqualTo(sameTotal));
    }

    @Test
    public void createOrderAndStartPaymentNotFound() {
        assertThatExceptionOfType(VariantNotAvailableException.class).isThrownBy(() -> {
            AeroflotServicePayload payload = testingScenarioPayload(BookingTestingScenarios.BOOKING_NOT_FOUND);
            adapter.createOrderAndStartPayment(null, payload, null, null);
        });
    }

    @Test
    public void createOrderAndStartPaymentUnhandled() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
            AeroflotServicePayload payload = testingScenarioPayload(BookingTestingScenarios.BOOKING_NEW_UNHANDLED);
            adapter.createOrderAndStartPayment(null, payload, null, null);
        }).withMessage("Testing Scenario: unhandled");
    }

    @Test
    public void createOrderAndStartPaymentNoTestingScenarios() {
        properties.setEnableTestingScenarios(false);
        // mock impl should work without testing exceptions
        AeroflotServicePayload payload = testingScenarioPayload(BookingTestingScenarios.BOOKING_NEW_PRICE);
        adapter.createOrderAndStartPayment(null, payload, null, null);
    }

    private AeroflotServicePayload testingScenarioPayload(String testType) {
        AeroflotServicePayload payload = AeroflotMocks.testPayload();
        payload.getTravellers().get(0).setFirstName(testType);
        return payload;
    }
}
