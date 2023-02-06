package ru.yandex.travel.orders.repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderCreateResult;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderRef;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderStatus;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotOrderSubStatus;
import ru.yandex.travel.orders.entities.AeroflotLastRequest;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class AeroflotLastRequestRepositoryTest {
    @Autowired
    private AeroflotLastRequestRepository aeroflotLastRequestRepository;

    @Test
    public void createLastRequest() {
        var lastRequest = Instant.now();
        var orderId = UUID.randomUUID();
        var result = new AeroflotOrderCreateResult();
        result.setStatusCode(AeroflotOrderStatus.PAID_TICKETED);
        result.setSubStatusCode(AeroflotOrderSubStatus.PAYMENT_EXPIRED);
        result.setCouponStatusCodes(new HashMap<>());
        var orderRef = new AeroflotOrderRef();
        orderRef.setOrderId(orderId.toString());
        result.setOrderRef(orderRef);

        createEntity(orderId, lastRequest, result);

        var item = aeroflotLastRequestRepository.getOne(orderId);
        assertThat(item.getLastRequestAt()).isEqualTo(lastRequest);
        assertThat(item.getOrderId()).isEqualTo(orderId);
        assertThat(item.getResult()).isEqualTo(result);
    }

    private void createEntity(UUID orderId, Instant lastRequest, AeroflotOrderCreateResult result) {
        var entity = new AeroflotLastRequest();
        entity.setOrderId(orderId);
        entity.setLastRequestAt(lastRequest);
        entity.setResult(result);
        aeroflotLastRequestRepository.saveAndFlush(entity);
    }
}
