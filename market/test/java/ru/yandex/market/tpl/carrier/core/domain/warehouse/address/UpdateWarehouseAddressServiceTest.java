package ru.yandex.market.tpl.carrier.core.domain.warehouse.address;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseRepository;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@CoreTestV2
class UpdateWarehouseAddressServiceTest {

    private final OrderWarehouseRepository orderWarehouseRepository;
    private final UpdateWarehouseAddressService updateWarehouseAddressService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final AddressGenerator addressGenerator;

    @Test
    void shouldNotUpdateWithNullCoordinates() {
        OrderWarehouse warehouse = orderWarehouseRepository.saveAndFlush(orderWarehouseGenerator.generateWarehouse());

        OrderWarehouseAddress address = addressGenerator.generateWarehouseAddress(
                AddressGenerator.AddressGenerateParam.builder().build()
        );
        address.setLatitude(null);
        address.setLongitude(null);


        UpdateWarehouseAddressPayload payload = new UpdateWarehouseAddressPayload(
                "",
                CarrierSource.SYSTEM,
                warehouse.getId(),
                null,
                warehouse.getIncorporation(),
                address,
                warehouse.getPhones(),
                warehouse.getContact(),
                warehouse.getRegionId(),
                warehouse.getTimezone()
        );

        updateWarehouseAddressService.processPayload(payload);

        OrderWarehouse updated = orderWarehouseRepository.findByIdOrThrow(warehouse.getId());
        Assertions.assertThat(updated.getAddress().getLatitude()).isNotNull();
        Assertions.assertThat(updated.getAddress().getLongitude()).isNotNull();


    }

}
