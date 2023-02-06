package ru.yandex.market.billing.agency_reward.program.purchase.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.agency.program.purchase.model.PartnerAgencyPair;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PartnerAgencyOrderInfoMapTest {

    @Test
    public void testOrderInfo() {
        PartnerAgencyOrderInfoMap info = new PartnerAgencyOrderInfoMap();
        info.addOrderCount(new PartnerAgencyPair(1, 1), 5L);
        info.addOrderCount(new PartnerAgencyPair(2, 1), 10L);
        info.addOrderCount(new PartnerAgencyPair(3, 2), 7L);

        assertThat(info.getOrderCountByPartnerAndAgency(1, 1)).isEqualTo(5);
        assertThat(info.getOrderCountByPartnerAndAgency(2, 1)).isEqualTo(10);
        assertThat(info.getOrderCountByPartnerAndAgency(3, 2)).isEqualTo(7);
        assertThat(info.getOrderCountByPartnerAndAgency(4, 2)).isEqualTo(0); //0, если нет записи
    }
}
