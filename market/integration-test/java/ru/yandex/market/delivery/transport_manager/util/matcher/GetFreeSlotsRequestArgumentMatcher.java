package ru.yandex.market.delivery.transport_manager.util.matcher;

import java.util.Collection;

import lombok.Value;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;

@Value
public class GetFreeSlotsRequestArgumentMatcher implements ArgumentMatcher<GetFreeSlotsRequest> {
    BookingType bookingType;
    SupplierType supplierType;
    Collection<Long> warehouseIds;

    @Override
    public boolean matches(GetFreeSlotsRequest argument) {
        return argument != null &&
            argument.getBookingType() == bookingType &&
            argument.getSupplierType() == supplierType &&
            argument.getWarehouseIds().equals(warehouseIds);
    }
}
