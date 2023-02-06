package ru.yandex.market.abo.core.hiding.util.checker;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.hiding.util.model.FreshOfferWrapper;
import ru.yandex.market.abo.core.partner.info.PartnerInfoService;
import ru.yandex.market.common.report.indexer.DeliveryCalendarService;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 27.01.17.
 */
public class DeliveryDatesCheckerTest extends DeliveryCheckerTest {
    @Autowired
    @InjectMocks
    private DeliveryDatesChecker deliveryDatesChecker;
    @Mock
    private DeliveryCalendarService deliveryCalendarService;
    @Mock
    private PartnerInfoService partnerInfoService;
    private int dayTo = RND.nextInt(5);

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(partnerInfoService.getPartnerPrograms(anyLong())).thenReturn(List.of(PartnerPlacementProgramType.CPC));
    }

    @Test
    public void checkFailsNoOptsInFeed() throws Exception {
        var offer = initOfferWithDeliveryOpts(RND.nextInt(), nextBigDecimal());
        var feedOffer = new OfferDetails(new Date(), new Date(), RND.nextDouble(), false, RND.nextInt(), "foo");
        var details = deliveryDatesChecker.diff(offer, new FreshOfferWrapper(feedOffer), null);
        assertNotNull(details);
        assertOfferDelivery(details, offer);
    }

    @Test
    public void testCheckFailsBadDates() throws Exception {
        var offer = initOfferWithDeliveryOpts(dayTo, nextBigDecimal());
        var feed = initFeedWithDeliveryOpts(dayTo + 1, nextBigDecimal());
        var details = deliveryDatesChecker.diff(offer, feed, null);
        assertNotNull(details);
        assertOfferDelivery(details, offer);
        assertFeedDelivery(details, feed.getIdxOffer());
    }

    @Test
    public void testCheckPasses() throws Exception {
        assertNull(deliveryDatesChecker.diff(
                initOfferWithDeliveryOpts(dayTo, nextBigDecimal()),
                initFeedWithDeliveryOpts(dayTo, nextBigDecimal()),
                null
        ));
    }

    @Test
    public void testRejectDeliveryOptsInPICheck() throws Exception {
        testRejectDeliveryOptsInPICheck(deliveryDatesChecker);
    }
}
