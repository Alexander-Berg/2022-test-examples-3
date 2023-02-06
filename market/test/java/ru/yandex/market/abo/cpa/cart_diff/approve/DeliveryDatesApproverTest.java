package ru.yandex.market.abo.cpa.cart_diff.approve;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.hiding.util.checker.CheckerUtil;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffPushApiResponseWrapper;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.hiding.util.checker.CheckerUtil.getHour;
import static ru.yandex.market.abo.core.hiding.util.checker.DeliveryCheckerTest.initDeliveryOption;

/**
 * @author artemmz
 * created on 05.08.16.
 */
public class DeliveryDatesApproverTest {
    protected static final Random RND = new Random();
    private static final int MAX_DAYS = 30;
    private static final int REPORT_DAY_TO = 0;

    @InjectMocks
    private DeliveryDatesApprover deliveryDatesApprover;
    @Mock
    private IdxAPI idxApiService;
    @Mock
    private Region region;
    @Mock
    private RegionService regionService;
    @Mock
    private RegionTree regionTree;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(region.getCustomAttributeValue(any())).thenReturn(String.valueOf(ZonedDateTime.now().getOffset().get(OFFSET_SECONDS)));
        when(regionTree.getRegion(anyInt())).thenReturn(region);
        when(regionService.getRegionTree()).thenReturn(regionTree);

        doNothing().when(idxApiService).applyDeliveryCalendar(any(), anyLong(), any());
    }

    @Test
    public void testHasDiff() throws Exception {
        runCheck(RND.nextInt(MAX_DAYS) + 1, true);
    }

    @Test
    public void testNoDiff() throws Exception {
        runCheck(-RND.nextInt(MAX_DAYS), false);
    }

    @Test
    public void testNoDiffAllOffsets() throws Exception {
        for (int i = -18; i < 19; i++) {
            when(region.getCustomAttributeValue(any())).thenReturn(String.valueOf(i * 3600));
            runCheck(-2, false);
        }
    }

    @Test
    public void testDiffAllOffsets() throws Exception {
        for (int i = -18; i < 19; i++) {
            when(region.getCustomAttributeValue(any())).thenReturn(String.valueOf(i * 3600));
            runCheck(2, true);
        }
    }

    @Test
    public void testDiffTransient() throws Exception {
        final int CART_OFFSET = 1;
        for (int i = -24; i < 25; i++) {
            when(region.getCustomAttributeValue(any())).thenReturn(String.valueOf(i * 3600));
            int currentOffsetDiff = CheckerUtil.offsetDiffInSeconds(region);

            if (currentOffsetDiff > secondsToMidnight()) { // должны передвинуть репорт на +1 день
                assertFalse(diffBinary(CART_OFFSET, null));
            } else {
                assertTrue(diffBinary(CART_OFFSET, null));
            }
        }
    }

    @Test
    public void testOrderBefore() throws Exception {
        assertFalse(diffBinary(0, getHour(new Date())));
    }

    @Test
    public void testHidingDetails() throws Exception {
        int CART_DAY_TO = 5;
        HidingDetails details = diff(CART_DAY_TO, null);
        assertNotNull(details);
        assertEquals(details.getDeliveryComparison().getMarketParam().getDayTo().intValue(), REPORT_DAY_TO);
        assertEquals(details.getDeliveryComparison().getShopParam().getDayTo().intValue(), CART_DAY_TO);
    }

    private int secondsToMidnight() {
        Date now = new Date();
        Date tomorrow = DateUtils.truncate(DateUtils.addDays(now, 1), Calendar.DATE);
        return (int) (tomorrow.getTime() - now.getTime()) / 1000;
    }

    private void runCheck(int cartOffsetInDays, boolean hasDiff) {
        assertTrue(diffBinary(cartOffsetInDays, null) == hasDiff);
    }

    private boolean diffBinary(int cartOffsetInDays, Integer orderBefore) {
        return diff(cartOffsetInDays, orderBefore) != null;
    }

    private HidingDetails diff(int cartOffsetInDays, Integer orderBefore) {
        CartResponse cartResponse = new CartResponse();
        CartDiffPushApiResponseWrapper responseWrapper = new CartDiffPushApiResponseWrapper(cartResponse);
        cartResponse.setDeliveryOptions(Collections.singletonList(DeliveryCostApproverTest.initCartOption(null,
                DateUtils.addDays(new Date(), REPORT_DAY_TO + cartOffsetInDays))));

        List<LocalDeliveryOption> deliveryOptions = Collections.singletonList(
                initDeliveryOption(REPORT_DAY_TO, orderBefore, BigDecimal.ZERO));
        Offer offer = new Offer();
        offer.setLocalDelivery(deliveryOptions);
        return deliveryDatesApprover.approveDiff(offer, responseWrapper, RND.nextLong());
    }

    @Test
    public void calendarDaysBetween() throws Exception {
        Date now = new Date();
        Date today1MinPastMidnight = DateUtils.addMinutes(DateUtils.truncate(now, Calendar.DATE), 1);
        Date today1MinBeforeMidnight = DateUtils.addMinutes(DateUtils.truncate(now, Calendar.DATE), -1);
        assertEquals(1, DeliveryDatesApprover.calendarDaysBetween(today1MinPastMidnight, today1MinBeforeMidnight).intValue());
        assertEquals(0, DeliveryDatesApprover.calendarDaysBetween(today1MinPastMidnight, today1MinPastMidnight).intValue());
    }
}
