package ru.yandex.market.delivery.transport_manager.util.matcher;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import lombok.Value;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.logistics.calendaring.client.dto.GetAvailableLimitRequest;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;

@Value
public class GetAvailableLimitRequestArgumentMatcher implements ArgumentMatcher<GetAvailableLimitRequest> {
    long warehouseId;
    BookingType bookingType;
    LocalDate date;

    @Override
    public boolean matches(GetAvailableLimitRequest argument) {
        return argument != null &&
            argument.getWarehouseId() == warehouseId &&
            argument.getBookingType() == bookingType &&
            argument.getSupplierType() == SupplierType.FIRST_PARTY &&
            Objects.equals(argument.getDates(), List.of(date));
    }
}
