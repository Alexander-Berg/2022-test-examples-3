package ru.yandex.market.abo.cpa.cancelled.approve;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.abo.cpa.check_order.PushApiHelper;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.hiding.util.checker.CheckerUtil.getHour;
import static ru.yandex.market.abo.core.hiding.util.checker.DeliveryCheckerTest.initDeliveryOption;

/**
 * @author artemmz
 * created on 05.08.16.
 */
public class OrderDeliveryApproverTest {
    protected static final Random RND = new Random();

    private static int FEED_DAY_FROM = 0;
    private static int FEED_DAY_TO = 3;
    private static Date now = new Date();

    @Mock
    private Region region;
    @Mock
    private RegionService regionService;
    @Mock
    private PushApiHelper pushApiHelper;
    @Mock
    private RegionTree regionTree;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(region.getCustomAttributeValue(any())).thenReturn(String.valueOf(ZonedDateTime.now().getOffset().get(OFFSET_SECONDS)));
        when(regionTree.getRegion(anyInt())).thenReturn(region);
        when(regionService.getRegionTree()).thenReturn(regionTree);
    }

    @Test
    public void deliveryDatesPreciseNoDiff() throws Exception {
        deliveryDatesPreciseDiff(now, null, FEED_DAY_FROM, FEED_DAY_TO, false);
    }

    @Test
    public void deliveryDatesPreciseDiff() throws Exception {
        deliveryDatesPreciseDiff(now, null, FEED_DAY_FROM + 1, FEED_DAY_TO + 2, true);
    }

    @Test
    public void deliveryDatesPreciseDiff_orderBefore() throws Exception {
        deliveryDatesPreciseDiff(now, getHour(now), FEED_DAY_FROM, FEED_DAY_TO, true);
    }

    @Test
    public void deliveryDatesPreciseDiff_nextDayInRegion() throws Exception {
        Date regionTime = DateUtils.addDays(now, 1);
        deliveryDatesPreciseDiff(regionTime, null, FEED_DAY_FROM, FEED_DAY_TO, true);
    }

    @Test
    public void deliveryDatesPreciseNoDiff_previousDayInRegion_AfterOrderBefore() throws Exception {
        Date regionTime = DateUtils.addDays(now, -1);
        deliveryDatesPreciseDiff(regionTime, getHour(now), FEED_DAY_FROM, FEED_DAY_TO, false);
    }

    @Test
    public void deliveryDatesPreciseDiff_previousDayInRegion_BeforeOrderBefore() throws Exception {
        Date regionTime = DateUtils.addDays(now, -1);
        deliveryDatesPreciseDiff(regionTime, getHour(now) + 1, FEED_DAY_FROM, FEED_DAY_TO, true);
    }

    private void deliveryDatesPreciseDiff(Date regionTime, Integer orderBefore, int orderOptionDayFrom,
                                          int orderOptionDayTo, boolean diff) {
        deliveryDatesPreciseDiff(regionTime, orderBefore, orderOptionDayFrom, orderOptionDayTo, diff, DeliveryType.DELIVERY);
    }

    private void deliveryDatesPreciseDiff(Date regionTime, Integer orderBefore, int orderOptionDayFrom,
                                          int orderOptionDayTo, boolean diff, DeliveryType dType) {
        LocalDeliveryOption feedOption = initDeliveryOption(FEED_DAY_FROM, FEED_DAY_TO, orderBefore, BigDecimal.ZERO);
        Delivery orderOptions = initOrderOptions(orderOptionDayFrom, orderOptionDayTo, dType);
        assertTrue(OrderDeliveryDatesApprover.deliveryDatesPreciseDiff(orderOptions, feedOption, regionTime) == diff);
    }

    private static Delivery initOrderOptions(int dayFrom, int dayTo, DeliveryType deliveryType) {
        Delivery delivery = new Delivery();
        delivery.setType(deliveryType);
        delivery.setDeliveryDates(new DeliveryDates(DateUtils.addDays(now, dayFrom), DateUtils.addDays(now, dayTo)));
        return delivery;
    }
}
