package ru.yandex.market.delivery.transport_manager.util.matcher;

import lombok.Value;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.logistics.calendaring.client.dto.UpdateQuotaRequest;

@Value
public class UpdateQuotaRequestArgumentMatcher implements ArgumentMatcher<UpdateQuotaRequest> {
    long bookingId;
    long pallets;
    long items;
    boolean decrease;

    @Override
    public boolean matches(UpdateQuotaRequest argument) {
        return argument != null &&
            argument.getBookingId() == bookingId &&
            argument.getTakenPallets() == pallets &&
            argument.getTakenItems() == items &&
            argument.getDecrease() == decrease;
    }
}
