package ru.yandex.autotests.market.stat.beans.clicks;

import org.apache.commons.lang.math.RandomUtils;
import ru.yandex.autotests.market.stat.attribute.BlockId;
import ru.yandex.autotests.market.stat.attribute.BsBlockId;
import ru.yandex.autotests.market.stat.attribute.CategId;
import ru.yandex.autotests.market.stat.attribute.Cookie;
import ru.yandex.autotests.market.stat.attribute.Defaults;
import ru.yandex.autotests.market.stat.attribute.Fuid;
import ru.yandex.autotests.market.stat.attribute.Ids;
import ru.yandex.autotests.market.stat.attribute.PP;
import ru.yandex.autotests.market.stat.attribute.Pof;
import ru.yandex.autotests.market.stat.attribute.Referrer;
import ru.yandex.autotests.market.stat.attribute.ShopId;
import ru.yandex.autotests.market.stat.attribute.ShowUid;
import ru.yandex.autotests.market.stat.attribute.UTM;
import ru.yandex.autotests.market.stat.attribute.Url;
import ru.yandex.autotests.market.stat.attribute.UrlType;
import ru.yandex.autotests.market.stat.attribute.UserType;
import ru.yandex.autotests.market.stat.attribute.Values;
import ru.yandex.autotests.market.stat.attribute.ip.IP;

import java.time.LocalDateTime;

public class ClickBuilder implements Cloneable {

    /**
     * use this builder to generate click
     */
    public static Click.ClickBuilder unique(LocalDateTime clickTime) {
        return uniqueClick(clickTime)
            .fuid(Fuid.generateFuidForClickTime(clickTime));
    }

    /**
     * @param clickTime
     * @return ClickBuilder
     */
    public static Click.ClickBuilder uniqueClick(LocalDateTime clickTime) {
        String cookie = Cookie.generateCookieForClickTime(clickTime);
        String ip = IP.generateValidNoYandexIPv4();
        String pp = PP.getRandomMarketNoCpaPP();
        String blockId = BlockId.generate(clickTime);
        String bsBlockId = RandomUtils.nextBoolean() ? BsBlockId.generate(clickTime) : "";
        boolean usingPofAsJson = RandomUtils.nextBoolean();
        Integer opp = usingPofAsJson ? 999 : null;
        String offerIdd = RandomUtils.nextBoolean() ? Values.string(44) : Values.longNumber(5);
        String pof = Pof.getPof(pp);
        Integer clid = usingPofAsJson ? RandomUtils.nextInt(50) + 1501 : null;
        Integer distrType = usingPofAsJson ? 1 : null;
        String pofRaw = usingPofAsJson ? "{\"mclid\": null, \"clid\": [\"" + pof + "\", \"" + clid + "\"], " +
            "\"distr_type\": \"" + distrType + "\", \"opp\": \"" + opp + "\"}" : pof;

        return Click.builder()
            .offerId(offerIdd)
            .rowId(Ids.uniqueRowId())
            .eventTime(clickTime)
            .url(Url.generateRandomUrl())
            .referer(Referrer.generate())
            .ip(IP.atonIPv4(ip).toString())
            .cookie(cookie)
            .showUid(ShowUid.generate(blockId))
            .categId(Integer.valueOf(CategId.generate()))
            .discount(false)
            .pp(Integer.valueOf(pp))
            .price((Integer) Defaults.PRICE.mask())
            .filter(0)
            .geoId((Integer) Defaults.GEO_ID.mask())
            .shopId(ShopId.generate())
            .blockId(blockId)
            .pof(Integer.valueOf(pof))
            .state(1)
            .hyperId(Ids.generateHyperId())
            .hyperCatId((Integer) Defaults.HYPER_CAT_ID.mask())
            .onStock(true)
            .bid((Integer) Defaults.BID.mask())
            .autobrokerEnabled(true)
            .wareId((String) Defaults.EMPTY.mask())
            .linkId(ShowUid.generate(blockId))
            .ipGeoId((Integer) Defaults.GEO_ID.mask())
            .offerPrice((Integer) Defaults.OFFER_PRICE.mask())
            .testTag((String) Defaults.TEST_TAG.mask())
            .uah("uah")
            .vclusterId(Ids.generateVclusterId())
            .wareMd5(Ids.generateWareMD5())
            .fuid(null)
            .testBuckets((String) Defaults.TEST_BUCKETS.mask())
            .cpa(false)
            .reqId(Values.string("req_id_", 8))
            .wprid((String) Defaults.WPRID.mask())
            .userType(UserType.EXTERNAL.mask())
            .utmSource(UTM.SOURCE.mask())
            .utmMedium(UTM.MEDIUM.mask())
            .utmTerm(UTM.TERM.mask())
            .utmCampaign(UTM.CAMPAIGN.mask())
            .touch((Boolean) Defaults.TOUCH.mask())
            .showCookie(cookie)
            .ip6(IP.getIPv6FromIPv4(ip))
            .sbid((Integer) Defaults.SBID.mask())
            .subRequestId("")
            .bsBlockId(bsBlockId)
            .position(RandomUtils.nextInt(500))
            .showTime(clickTime.minusMinutes(5))
            .navCatId((Integer) Defaults.NAV_CAT_ID.mask())
            .uuid(Ids.generateUuid())
            .bestDeal(RandomUtils.nextBoolean())
            .hostname((String) Defaults.HOST.mask())
            .cbVnd(0)
            .cpVnd(0)
            .vndId(0)
            .dtsrcId(0)
            .typeId(0)
            .phoneClickRatio((Double) Defaults.CLICK_PHONE_RATIO.mask())
            .isPriceFrom(false)
            .pofRaw(pofRaw)
            .vid(null)
            .clid(clid)
            .distrType(distrType)
            .opp(opp)
            .minBid(0)
            .ppOi(Integer.valueOf(PP.getRandomPPOi()))
            .bidType((String) Defaults.BID_TYPE.mask())
            .feedId(Values.longNumber(5))
            .urlType(UrlType.getRandomValue())
            .promoType(1)
            .supplier_type("")
            .rgb("GREEN")
            .icookie(cookie)
            ;

    }
}