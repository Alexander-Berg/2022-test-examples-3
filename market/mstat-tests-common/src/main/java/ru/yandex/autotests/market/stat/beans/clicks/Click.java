package ru.yandex.autotests.market.stat.beans.clicks;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.beanio.annotation.Field;
import org.beanio.annotation.Record;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.date.PeriodUtils;
import ru.yandex.autotests.market.stat.beans.IMstGetterBean;
import ru.yandex.autotests.market.stat.beans.WithFilter;
import ru.yandex.autotests.market.stat.beans.WithPlacement;
import ru.yandex.autotests.market.stat.handlers.Handlers;

import static ru.yandex.autotests.market.stat.attribute.Fields.*;


/**
 * Created by stille on 06.05.14.
 * <p>
 * Click bean
 */
@Record()
@Builder
@Getter
@Setter
@ToString
public class Click implements IMstGetterBean, WithFilter, WithPlacement {

    @Field(at = 0, name = ROWID)
    private String rowId;

    @Field(at = 1, name = EVENTTIME, handlerName = Handlers.EVENTTIME_HANDLER)
    private LocalDateTime eventTime;

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

    @Field(at = 7, name = CATEG_ID)
    private Integer categId;

    @Field(at = 8, name = DISCOUNT)
    private Boolean discount;

    @Field(at = 9, name = PP_)
    private Integer pp;

    @Field(at = 10, name = PRICE)
    private Integer price;

    @Field(at = 11, name = FILTER)
    private Integer filter;

    @Field(at = 12, name = GEO_ID)
    private Integer geoId;

    @Field(at = 13, name = SHOP_ID)
    private Integer shopId;

    @Field(at = 14, name = BLOCK_ID)
    private String blockId;

    @Field(at = 15, name = POF)
    private Integer pof;

    @Field(at = 16, name = STATE)
    private Integer state;

    @Field(at = 17, name = HYPER_ID)
    private Integer hyperId;

    @Field(at = 18, name = HYPER_CAT_ID)
    private Integer hyperCatId;

    @Field(at = 19, name = ONSTOCK)
    private Boolean onStock;

    @Field(at = 20, name = BID)
    private Integer bid;

    @Field(at = 21, name = AUTOBROKER_ENABLED)
    private Boolean autobrokerEnabled;

    @Field(at = 22, name = WARE_ID)
    private String wareId;

    @Field(at = 23, name = LINK_ID)
    private String linkId;

    @Field(at = 24, name = IP_GEO_ID)
    private Integer ipGeoId;

    @Field(at = 25, name = OFFER_PRICE)
    private Integer offerPrice;

    @Field(at = 26, name = TEST_TAG)
    private String testTag;

    @Field(at = 27, name = UAH)
    private String uah;

    @Field(at = 28, name = VCLUSTER_ID)
    private Integer vclusterId;

    @Field(at = 29, name = WARE_MD5)
    private String wareMd5;

    @Field(at = 30, name = FUID)
    private String fuid;

    @Field(at = 31, name = TEST_BUCKETS)
    private String testBuckets;

    @Field(at = 32, name = CPA)
    private Boolean cpa;

    @Field(at = 33, name = REQ_ID)
    private String reqId;

    @Field(at = 34, name = WPRID)
    private String wprid;

    @Field(at = 35, name = USER_TYPE)
    private Integer userType;

    @Field(at = 36, name = UTM_SOURCE)
    private String utmSource;

    @Field(at = 37, name = UTM_MEDIUM)
    private String utmMedium;

    @Field(at = 38, name = UTM_TERM)
    private String utmTerm;

    @Field(at = 39, name = UTM_CAMPAIGN)
    private String utmCampaign;

    @Field(at = 40, name = TOUCH)
    private Boolean touch;

    @Field(at = 41, name = SHOW_COOKIE)
    private String showCookie;

    @Field(at = 42, name = IP6)
    private String ip6;

    @Field(at = 43, name = SBID)
    private Integer sbid;

    @Field(at = 44, name = SUB_REQUEST_ID)
    private String subRequestId;

    @Field(at = 45, name = BS_BLOCK_ID)
    private String bsBlockId;

    @Field(at = 46, name = POSITION)
    private Integer position;

    @Field(at = 47, name = SHOW_TIME, handlerName = Handlers.EVENTTIME_HANDLER)
    private LocalDateTime showTime;

    @Field(at = 48, name = NAV_CAT_ID)
    private Integer navCatId;

    @Field(at = 49, name = UUID)
    private String uuid;

    @Field(at = 50, name = BEST_DEAL)
    private Boolean bestDeal;

    @Field(at = 51, name = HOSTNAME)
    private String hostname;

    @Field(at = 52, name = CP_VND)
    private Integer cpVnd;

    @Field(at = 53, name = CB_VND)
    private Integer cbVnd;

    @Field(at = 54, name = VND_ID)
    private Integer vndId;

    @Field(at = 55, name = DTSRC_ID)
    private Integer dtsrcId;

    @Field(at = 56, name = TYPE_ID)
    private Integer typeId;

    @Field(at = 57, name = PHONE_CLICK_RATIO)
    private Double phoneClickRatio;

    @Field(at = 58, name = IS_PRICE_FROM)
    private Boolean isPriceFrom;

    @Field(at = 59, name = VID)
    private Integer vid;

    @Field(at = 60, name = CLID)
    private Integer clid;

    @Field(at = 61, name = DISTR_TYPE)
    private Integer distrType;

    @Field(at = 62, name = POF_RAW)
    private String pofRaw;

    @Field(at = 63, name = MIN_BID)
    private Integer minBid;

    @Field(name = PP_OI, at = 64)
    private Integer ppOi;

    @Field(name = BID_TYPE, at = 65)
    private String bidType;

    @Field(name = FEED_ID, at = 66)
    private String feedId;

    @Field(name = OFFER_ID, at = 67)
    private String offerId;

    @Field(name = URL_TYPE, at = 68)
    private Integer urlType;

    @Field(name = PROMO_TYPE, at = 69)
    private Integer promoType;

    @Field(name = "opp", at = 70)
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
        return rowId;
    }

    @Override
    public LocalDateTime extractDayAndHour() {
        return PeriodUtils.truncateToHour(eventTime);
    }

}