package ru.yandex.market.pvz.core.domain.delivery_service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryServiceCommandServiceTest {

    private static final int DS_ID = 100123;
    private static final String DS_NAME = "DS NAME";

    private final DeliveryServiceCommandService commandService;

    @Test
    void testCreateDS() {
        DeliveryService deliveryService = commandService.getOrCreateDeliveryService(DS_ID, DS_NAME);
        assertThat(deliveryService).isNotNull();
        assertThat(deliveryService.getId()).isEqualTo(DS_ID);
        assertThat(deliveryService.getName()).isEqualTo(DS_NAME);
        assertThat(deliveryService.getToken()).isNotEmpty();
    }

    @Test
    void testNotDuplicateDS() {
        testCreateDS();
        DeliveryService deliveryService = commandService.getOrCreateDeliveryService(DS_ID, null);
        assertThat(deliveryService.getName()).isEqualTo(DS_NAME);
    }

}
