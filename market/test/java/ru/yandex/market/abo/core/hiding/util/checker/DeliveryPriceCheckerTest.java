package ru.yandex.market.abo.core.hiding.util.checker;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.partner.info.PartnerInfoService;
import ru.yandex.market.abo.core.shop.CommonShopInfoService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 27.01.17.
 */
public class DeliveryPriceCheckerTest extends DeliveryCheckerTest {
    @InjectMocks
    private DeliveryPriceChecker deliveryPriceChecker;

    @Mock
    CommonShopInfoService commonShopInfoService;

    @Mock
    private PartnerInfoService partnerInfoService;

    private BigDecimal cost = nextBigDecimal();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(commonShopInfoService.deliveryOptionsSetInPI(anyLong())).thenReturn(false);
        when(partnerInfoService.getPartnerPrograms(anyLong())).thenReturn(List.of(PartnerPlacementProgramType.CPC));
    }

    @Test
    public void testCheckFails() throws Exception {
        var offer = initOfferWithDeliveryOpts(null, cost);
        var feed = initFeedWithDeliveryOpts(null, BigDecimal.valueOf(cost.doubleValue() + 100));
        var details = deliveryPriceChecker.diff(offer, feed, null);
        assertNotNull(details);
        assertOfferDelivery(details, offer);
        assertFeedDelivery(details, feed.getIdxOffer());
    }

    @Test
    public void testCheckPasses() throws Exception {
        assertNull(deliveryPriceChecker.diff(
                initOfferWithDeliveryOpts(null, cost),
                initFeedWithDeliveryOpts(null, cost),
                null
        ));
    }

    @Test
    public void testRejectDeliveryOptsInPICheck() throws Exception {
        when(commonShopInfoService.deliveryOptionsSetInPI(anyLong())).thenReturn(true);
        testRejectDeliveryOptsInPICheck(deliveryPriceChecker);
    }

}
