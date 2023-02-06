package ru.yandex.autotests.market.stat.beans.vendorclicks;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.beanio.annotation.Field;
import org.beanio.annotation.Record;
import ru.yandex.autotests.market.stat.beans.WithFilter;
import ru.yandex.autotests.market.stat.beans.WithPeriod;
import ru.yandex.autotests.market.stat.date.PeriodUtils;
import ru.yandex.autotests.market.stat.handlers.Handlers;

import java.time.LocalDateTime;

import static ru.yandex.autotests.market.stat.attribute.Fields.*;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 24.01.17.
 */
@Record()
@Builder
@Getter
@Setter
@ToString
public class VendorClicks implements WithPeriod, WithFilter {
    @Field(at = 0, name = ROWID)
    private String rowid;
    @Field(at = 1, name = EVENTTIME, handlerName = Handlers.EVENTTIME_HANDLER)
    private LocalDateTime eventtime;
    @Field(at = 2, name = URL)
    private String url;
    @Field(at = 3, name = REFERER)
    private String referer;
    @Field(at = 4, name = IP_)
    private String ip;
    @Field(at = 5, name = COOKIE)
    private String cookie;
    @Field(at = 6, name = SHOW_UID)
    private String showUid;
    @Field(at = 7, name = PP_)
    private Integer pp;
    @Field(at = 8, name = HYPER_ID)
    private Integer hyperId;
    @Field(at = 9, name = HYPER_CAT_ID)
    private Integer hyperCatId;
    @Field(at = 10, name = GEO_ID)
    private Integer geoId;
    @Field(at = 11, name = IP_GEO_ID)
    private Integer ipGeoId;
    @Field(at = 12, name = BRAND_ID)
    private Integer brandId;
    @Field(at = 13, name = VENDOR_PRICE)
    private Integer vendorPrice;
    @Field(at = 14, name = VENDOR_DS_ID)
    private Integer vendorDsId;
    @Field(at = 15, name = VC_BID)
    private Integer vcBid;
    @Field(at = 16, name = USER_TYPE)
    private Integer userType;
    @Field(at = 17, name = SHOW_COOKIE)
    private String showCookie;
    @Field(at = 18, name = IP6)
    private String ip6;
    @Field(at = 19, name = SHOW_TIME, handlerName = Handlers.EVENTTIME_HANDLER)
    private LocalDateTime showTime;
    @Field(at = 20, name = HOSTNAME)
    private String hostname;
    @Field(at = 21, name = POF_RAW)
    private String pofRaw;
    @Field(at = 22, name = FILTER)
    private Integer filter;
    @Field(at = 23, name = STATE)
    private Integer state;
    @Field(at = 24, name = POSITION)
    private Integer position;
    @Field(name = "opp")
    private Integer opp;
    @Field(name = "rgb")
    private String rgb;
    @Field(name = "supplier_type")
    private String supplier_type;
    @Field(name = "icookie")
    private String icookie;
    @Field(name = "pof")
    private Integer pof;
    @Field(name = CLID)
    private Integer clid;
    @Field(name = DISTR_TYPE)
    private Integer distrType;
    private String day;
    private String hour;

    @Override
    public String id() {
        return rowid;
    }

    @Override
    public LocalDateTime extractDayAndHour() {
        return PeriodUtils.truncateToHour(eventtime);
    }

}