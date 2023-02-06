package ru.yandex.autotests.market.stat.beans.cpaclicks;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.beanio.annotation.Field;
import org.beanio.annotation.Record;
import ru.yandex.autotests.market.stat.date.PeriodUtils;
import ru.yandex.autotests.market.stat.beans.IMstGetterBean;
import ru.yandex.autotests.market.stat.beans.WithFilter;
import ru.yandex.autotests.market.stat.beans.WithPlacement;
import ru.yandex.autotests.market.stat.handlers.Handlers;

import java.time.LocalDateTime;

import static ru.yandex.autotests.market.stat.attribute.Fields.*;


/**
 * Created by stille on 23.05.14.
 */
@Record()
@Builder
@Getter
@Setter
@ToString
public class CpaClick implements IMstGetterBean, WithPlacement, WithFilter {

    @Field(name = ROWID, at = 0)
    private String rowid;

    @Field(at = 1, name = EVENTTIME, handlerName = Handlers.EVENTTIME_HANDLER)
    private LocalDateTime eventtime;

    @Field(name = URL, at = 2)
    private String url;

    @Field(name = REFERER, at = 3)
    private String referer;

    @Field(name = IP_, at = 4)
    private String ip;

    @Field(name = COOKIE, at = 5)
    private String cookie;

    @Field(name = SHOW_UID, at = 6)
    private String showUid;

    @Field(name = PP_, at = 7)
    private Integer pp;

    @Field(name = POF, at = 8)
    private Integer pof;

    @Field(name = FEE, at = 9)
    private Double fee;

    @Field(name = FUID, at = 10)
    private String fuid;

    @Field(name = HYPER_ID, at = 11)
    private Integer hyperId;

    @Field(name = HYPER_CAT_ID, at = 12)
    private Integer hyperCatId;

    @Field(name = GEO_ID, at = 13)
    private Integer geoId;

    @Field(name = SHOP_ID, at = 14)
    private Integer shopId;

    @Field(name = ONSTOCK, at = 15)
    private Boolean onstock;

    @Field(name = IP_GEO_ID, at = 16)
    private Integer ipGeoId;

    @Field(name = OFFER_PRICE, at = 17)
    private Integer offerPrice;

    @Field(name = CATEG_ID, at = 18)
    private Integer categId;

    @Field(name = VCLUSTER_ID, at = 19)
    private Integer vclusterId;

    @Field(name = LINK_ID, at = 20)
    private String linkId;

    @Field(name = WARE_MD5, at = 21)
    private String wareMd5;

    @Field(name = USER_TYPE, at = 22)
    private Integer userType;

    @Field(name = WPRID, at = 23)
    private String wprid;

    @Field(name = REQ_ID, at = 24)
    private String reqId;

    @Field(name = TEST_BUCKETS, at = 25)
    private String testBuckets;

    @Field(name = TOUCH, at = 26)
    private Boolean touch;

    @Field(name = SHOW_COOKIE, at = 27)
    private String showCookie;

    @Field(name = IP6, at = 28)
    private String ip6;

    @Field(name = SBID, at = 29)
    private Integer sbid;

    @Field(at = 30, name = SHOW_TIME, handlerName = Handlers.EVENTTIME_HANDLER)
    private LocalDateTime showTime;

    @Field(name = NAV_CAT_ID, at = 31)
    private Integer navCatId;

    @Field(name = UUID, at = 32)
    private String uuid;

    @Field(name = HOSTNAME, at = 33)
    private String hostname;

    @Field(at = 34, name = TYPE_ID)
    private Integer typeId;

    @Field(at = 35, name = VID)
    private Integer vid;

    @Field(at = 36, name = CLID)
    private Integer clid;

    @Field(at = 37, name = DISTR_TYPE)
    private Integer distrType;

    @Field(at = 38, name = POF_RAW)
    private String pofRaw;

    @Field(at = 39, name = FILTER)
    private Integer filter;

    @Field(name = PP_OI, at = 40)
    private Integer ppOi;

    @Field(name = DISCOUNT, at = 41)
    private Boolean discount;

    @Field(name = PRICE, at = 42)
    private Integer price;

    @Field(name = BLOCK_ID, at = 43)
    private String blockId;

    @Field(name = STATE, at = 44)
    private Integer state;

    @Field(name = BID, at = 45)
    private Integer bid;

    @Field(name = AUTOBROKER_ENABLED, at = 46)
    private Boolean autobrokerEnabled;

    @Field(name = WARE_ID, at = 47)
    private String wareId;

    @Field(name = TEST_TAG, at = 48)
    private String testTag;

    @Field(name = UAH, at = 49)
    private String uah;

    @Field(name = CPA, at = 50)
    private Boolean cpa;

    @Field(name = UTM_SOURCE, at = 51)
    private String utmSource;

    @Field(name = UTM_MEDIUM, at = 52)
    private String utmMedium;

    @Field(name = UTM_TERM, at = 53)
    private String utmTerm;

    @Field(name = UTM_CAMPAIGN, at = 54)
    private String utmCampaign;

    @Field(name = SUB_REQUEST_ID, at = 55)
    private String subRequestId;

    @Field(name = BS_BLOCK_ID, at = 56)
    private String bsBlockId;

    @Field(name = POSITION, at = 57)
    private Integer position;

    @Field(name = BEST_DEAL, at = 58)
    private Boolean bestDeal;

    @Field(name = CP_VND, at = 59)
    private Integer cpVnd;

    @Field(name = CB_VND, at = 60)
    private Integer cbVnd;

    @Field(name = VND_ID, at = 61)
    private Integer vndId;

    @Field(name = DTSRC_ID, at = 62)
    private Integer dtsrcId;

    @Field(name = PHONE_CLICK_RATIO, at = 63)
    private Double phoneClickRatio;

    @Field(name = IS_PRICE_FROM, at = 64)
    private Boolean isPriceFrom;

    @Field(name = MIN_BID, at = 65)
    private Integer minBid;

    @Field(name = SHOP_FEE, at = 66)
    private Integer shopFee;

    @Field(name = MIN_FEE, at = 67)
    private Integer minFee;

    @Field(name = BID_TYPE, at = 68)
    private String bidType;

    @Field(name = FEED_ID, at = 69)
    private String feedId;

    @Field(name = OFFER_ID, at = 70)
    private String offerId;

    @Field(name = URL_TYPE, at = 71)
    private Integer urlType;

    @Field(name = PROMO_TYPE, at = 72)
    private Integer promoType;

    @Field(name = "opp", at = 73)
    private Integer opp;

    @Field(name = "rgb")
    private String rgb;

    @Field(name = "supplier_type")
    private String supplier_type;

    @Field(name = "icookie")
    private String icookie;

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
