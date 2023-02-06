package ru.yandex.market.adv.promo.tms.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnersBusinessResponse;

import static org.mockito.Mockito.doReturn;

class PartnerBusinessServiceTest extends FunctionalTest {
    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private PartnerBusinessService partnerBusinessService;

    @Test
    public void getBusinessesForPartnersTest() {
        Set<Long> partnerIds = Set.of(11L, 12L, 13L);
        PartnersBusinessResponse response = new PartnersBusinessResponse(
                List.of(
                        new PartnerBusinessDTO(11, 101L),
                        new PartnerBusinessDTO(12, 102L)
                )
        );
        doReturn(response).when(mbiApiClient).getBusinessesForPartners(partnerIds);
        Map<Long, Long> businessesForPartners = partnerBusinessService.getBusinessesForPartners(partnerIds);
        Assertions.assertThat(businessesForPartners).containsAllEntriesOf(Map.of(11L, 101L, 12L, 102L));
    }
}
