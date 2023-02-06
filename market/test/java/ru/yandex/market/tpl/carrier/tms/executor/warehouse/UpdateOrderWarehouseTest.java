package ru.yandex.market.tpl.carrier.tms.executor.warehouse;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@TmsIntTest
public class UpdateOrderWarehouseTest {

    private final LMSClient lmsClient;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final UpdateOrderWarehouseExecutor orderWarehouseExecutor;

    private OrderWarehousePartner partner;

    @BeforeEach
    void setUp() {
        partner = orderWarehousePartnerRepository.save(new OrderWarehousePartner("123", null));
        Mockito.when(lmsClient.getPartner(123L)).thenReturn(Optional.of(PartnerResponse.newBuilder()
                .name("Dropship_Arsenal")
                .readableName("Арсенал")
                .build()));

    }

    @SneakyThrows
    @Test
    void shouldUpdateOrderWarehousePartnerName() {
        orderWarehouseExecutor.doRealJob(null);

        partner = orderWarehousePartnerRepository.findByIdOrThrow(this.partner.getId());
    }
}
