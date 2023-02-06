package ru.yandex.market.core.outlet.db;

import java.math.BigDecimal;
import java.util.Collections;

import javax.annotation.Nonnull;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.delivery.PointType;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;

import static java.util.Arrays.asList;

/**
 * Предоставляет тестовые данные по аутлетам.
 *
 * @author ivmelnik
 * @since 11.07.18
 */
public class DbMarketDeliveryOutletInfoProvider {

    public static final long DELIVERY_SERVICE_ID = 106L;
    public static final PointType POINT_TYPE = PointType.OUTLET;
    public static final boolean IS_INLET = POINT_TYPE == PointType.INLET;
    public static final String DELIVERY_SERVICE_OUTLET_CODE1 = "11";
    public static final String DELIVERY_SERVICE_OUTLET_CODE2 = "22";
    public static final PhoneNumber PHONE1 = getPhoneNumber("321-4567");
    public static final PhoneNumber PHONE2 = getPhoneNumber("322-4567");
    public static final DeliveryRule DELIVERY_RULE1 = getDeliveryRule(100);
    public static final DeliveryRule DELIVERY_RULE2 = getDeliveryRule(200);
    public static final ScheduleLine SCHEDULE_LINE1 = new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 660, 660);
    public static final ScheduleLine SCHEDULE_LINE2 = new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 1, 540, 600);
    public static final ScheduleLine SCHEDULE_LINE3 = new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 1, 600, 480);

    @Nonnull
    public static OutletInfo getOutletWithNoChildData(String deliveryServiceOutletCode) {
        OutletInfo outletInfo = new OutletInfo(-1, -1, OutletType.DEPOT, "outlet1", true, null);
        //setting key values
        outletInfo.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        outletInfo.setDeliveryServiceOutletCode(deliveryServiceOutletCode);
        outletInfo.setInlet(IS_INLET);
        return outletInfo;
    }

    @Nonnull
    public static OutletInfo getOutletWithNullChildData(String deliveryServiceOutletCode) {
        OutletInfo outletInfo = getOutletWithNoChildData(deliveryServiceOutletCode);
        outletInfo.addPhone(null);
        outletInfo.addDeliveryRule(null);
        outletInfo.setSchedule(new Schedule(-1, Collections.singletonList(null)));
        return outletInfo;
    }

    @Nonnull
    public static OutletInfo getOutletWithChildData(String deliveryServiceOutletCode) {
        OutletInfo outletInfo = getOutletWithNoChildData(deliveryServiceOutletCode);
        outletInfo.setPhones(asList(
                PHONE1,
                PHONE2
        ));
        outletInfo.addDeliveryRules(asList(
                DELIVERY_RULE1,
                DELIVERY_RULE2
        ));
        outletInfo.setSchedule(new Schedule(-1, asList(
                SCHEDULE_LINE1,
                SCHEDULE_LINE2,
                SCHEDULE_LINE3
        )));
        return outletInfo;
    }

    @Nonnull
    public static OutletInfo getOutletWithChildDataAndNulls(String deliveryServiceOutletCode) {
        OutletInfo outletInfo = getOutletWithNoChildData(deliveryServiceOutletCode);
        outletInfo.setPhones(asList(
                PHONE1,
                PHONE2,
                null
        ));
        outletInfo.addDeliveryRules(asList(
                DELIVERY_RULE1,
                DELIVERY_RULE2,
                null
        ));
        outletInfo.setSchedule(new Schedule(-1, asList(
                SCHEDULE_LINE1,
                SCHEDULE_LINE2,
                SCHEDULE_LINE3,
                null
        )));
        return outletInfo;
    }

    @Nonnull
    private static PhoneNumber getPhoneNumber(String number) {
        PhoneNumber.Builder pnb = PhoneNumber.builder();
        pnb.setCountry("7");
        pnb.setCity("345");
        pnb.setNumber(number);
        pnb.setPhoneType(PhoneType.FAX);
        pnb.setComments("blablaphone");
        pnb.setExtension("55");
        return pnb.build();
    }

    @Nonnull
    private static DeliveryRule getDeliveryRule(int cost) {
        DeliveryRule deliveryRule = new DeliveryRule();
        deliveryRule.setCost(BigDecimal.valueOf(cost));
        deliveryRule.setCostCurrency(Currency.RUR);
        deliveryRule.setPriceFrom(BigDecimal.valueOf(1));
        deliveryRule.setPriceTo(BigDecimal.valueOf(1000));
        deliveryRule.setMinDeliveryDays(0);
        deliveryRule.setMaxDeliveryDays(30);
        deliveryRule.setWorkInHoliday(false);
        deliveryRule.setDateSwitchHour(19);
        deliveryRule.setUnspecifiedDeliveryInterval(true);
        deliveryRule.setDeliveryServiceInfo(
                new DeliveryServiceInfo(DELIVERY_SERVICE_ID, "Boxberry")
        );
        return deliveryRule;
    }

}
