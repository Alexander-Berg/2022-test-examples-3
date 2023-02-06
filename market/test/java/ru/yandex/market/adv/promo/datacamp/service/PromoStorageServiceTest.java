package ru.yandex.market.adv.promo.datacamp.service;

import java.util.Set;

import NMarket.Common.Promo.Promo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;

import static org.mockito.Mockito.*;

public class PromoStorageServiceTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;
    @Autowired
    private PromoStorageService promoStorageService;

    @Test
    public void testGetPromos() {
        long partnerId = 10;
        long businessId = 42;
        String promoId = partnerId + "_**";

        ArgumentCaptor<GetPromoBatchRequestWithFilters> argumentCapture = ArgumentCaptor.forClass(GetPromoBatchRequestWithFilters.class);

        promoStorageService.getPromos(partnerId, businessId, Set.of(promoId));

        verify(dataCampClient, times(1)).getPromos(argumentCapture.capture());
        GetPromoBatchRequestWithFilters arg = argumentCapture.getValue();

        Assertions.assertEquals(1, arg.getRequest().getEntriesCount());
        Assertions.assertEquals(businessId, arg.getRequest().getEntries(0).getBusinessId());
        Assertions.assertEquals(Promo.ESourceType.PARTNER_SOURCE, arg.getRequest().getEntries(0).getSource());
        Assertions.assertEquals(promoId, arg.getRequest().getEntries(0).getPromoId());
        Assertions.assertNull(arg.getEnabled());
        Assertions.assertNull(arg.getOnlyUnfinished());
        Assertions.assertNull(arg.getPromoType());
    }
}
