package ru.yandex.travel.orders.repository;

import java.time.Instant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.AeroflotOrderItem;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class AeroflotOrderItemRepositoryTest {
    @Autowired
    private AeroflotOrderItemRepository aeroflotOrderItemRepository;

    @Test
    public void testFindByAviaPnr() {
        AeroflotOrderItem item1 = orderItem("PNR1", "2007-12-03T10:15:30.00Z");
        assertThat(aeroflotOrderItemRepository.findFirstByAviaPnrOrderByCreatedAtDesc("PNR1")).isEqualTo(item1);

        AeroflotOrderItem item2 = orderItem("PNR1", "2007-12-03T10:16:30.00Z");
        assertThat(aeroflotOrderItemRepository.findFirstByAviaPnrOrderByCreatedAtDesc("PNR1")).isEqualTo(item2);

        orderItem("PNR1", "2007-12-03T10:15:45.00Z");
        assertThat(aeroflotOrderItemRepository.findFirstByAviaPnrOrderByCreatedAtDesc("PNR1")).isEqualTo(item2);

        AeroflotOrderItem item4 = orderItem("PNR2", "2007-12-03T10:18:45.00Z");
        assertThat(aeroflotOrderItemRepository.findFirstByAviaPnrOrderByCreatedAtDesc("PNR1")).isEqualTo(item2);
        assertThat(aeroflotOrderItemRepository.findFirstByAviaPnrOrderByCreatedAtDesc("PNR2")).isEqualTo(item4);
    }

    private AeroflotOrderItem orderItem(String pnr, String date) {
        AeroflotOrderItem orderItem = new AeroflotOrderItem();
        orderItem.setAviaPnr(pnr);
        orderItem = aeroflotOrderItemRepository.saveAndFlush(orderItem);
        // overriding the default create_at timestamp
        orderItem.setCreatedAt(Instant.parse(date));
        orderItem = aeroflotOrderItemRepository.saveAndFlush(orderItem);
        return orderItem;
    }
}
