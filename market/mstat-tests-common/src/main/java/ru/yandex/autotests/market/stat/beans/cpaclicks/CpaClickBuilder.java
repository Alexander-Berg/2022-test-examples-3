package ru.yandex.autotests.market.stat.beans.cpaclicks;

import org.apache.commons.lang.math.RandomUtils;
import ru.yandex.autotests.market.stat.attribute.BlockId;
import ru.yandex.autotests.market.stat.attribute.BsBlockId;
import ru.yandex.autotests.market.stat.attribute.CategId;
import ru.yandex.autotests.market.stat.attribute.Cookie;
import ru.yandex.autotests.market.stat.attribute.Defaults;
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
import java.util.Arrays;

/**
 * The {@link CpaClickBuilder} is a Builder for {@link CpaClick} objects.
 */
public class CpaClickBuilder implements Cloneable {

    public static CpaClick.CpaClickBuilder uniqueOffercardClick(LocalDateTime clickTime) {
        String blockId = BlockId.generate(clickTime);
        String bsBlockId = RandomUtils.nextBoolean() ? BsBlockId.generate(clickTime) : "";
        return uniqueCpaClick(clickTime)
            .offerId("")
            .discount(false)
            .price((Integer) Defaults.PRICE.mask())
            .blockId(blockId)
            .state(1)
            .bid((Integer) Defaults.BID.mask())
            .autobrokerEnabled(true)
            .wareId("")
            .testTag((String) Defaults.TEST_TAG.mask())
            .uah("uah")
            .cpa((Boolean) Defaults.CPA.mask())
            .utmSource(UTM.SOURCE.mask())
            .utmMedium(UTM.MEDIUM.mask())
            .utmTerm(UTM.TERM.mask())
            .utmCampaign(UTM.CAMPAIGN.mask())
            .subRequestId("")
            .bsBlockId(bsBlockId)
            .position(RandomUtils.nextInt(500))
            .bestDeal(RandomUtils.nextBoolean())
            .cbVnd(0)
            .cpVnd(0)
            .vndId(-1)
            .dtsrcId(0)
            .typeId(2)
            .phoneClickRatio((Double) Defaults.CLICK_PHONE_RATIO.mask())
            .isPriceFrom(false)
            .minBid(null)
            ;
    }

    public static CpaClick.CpaClickBuilder uniqueCpaClick(LocalDateTime clickTime) {
        String cookie = Cookie.generateCookieForClickTime(clickTime);
        String ip = IP.generateValidNoYandexIPv4();
        String blockId = BlockId.generateSmallFormat(clickTime);
        String showUid = ShowUid.generate(clickTime);
        String pp = PP.getRandomMarketPP();
        while (Arrays.asList(141, 142).contains(Integer.parseInt(pp))) {
            pp = PP.getRandomMarketPP();
        }
        boolean usingPofAsJson = RandomUtils.nextBoolean();
        String pof = Pof.getPof(pp);
        Integer clid = usingPofAsJson ? RandomUtils.nextInt(50) + 1501 : null;
        Integer distrType = usingPofAsJson ? 1 : null;
        Integer opp = usingPofAsJson ? 999 : null;
        String pofRaw = usingPofAsJson ? "{\"mclid\": null, \"clid\": [\"" + pof + "\", \"" + clid + "\"], \"distr_type\": " +
            distrType + ", \"opp\": " + opp + "}" : pof;
        String offerId = RandomUtils.nextBoolean() ? Values.string(44) : Values.longNumber(5);
        return CpaClick.builder()
            .rowid(Ids.uniqueRowId())
            .eventtime(clickTime)
            .url(Url.generateRandomUrl())
            .referer(Referrer.generate())
            .ip(IP.atonIPv4(ip).toString())
            .cookie(cookie)
            .showUid(showUid)
            .categId(Integer.valueOf(CategId.generate()))
            .pp(Integer.valueOf(pp))
            .fee(Double.valueOf(Values.generateValidFee()))
            .geoId((Integer) Defaults.GEO_ID.mask())
            .shopId(ShopId.generate())
            .pof(Integer.valueOf(pof))
            .hyperId(Ids.generateHyperId())
            .hyperCatId((Integer) Defaults.HYPER_CAT_ID.mask())
            .onstock(true)
            .linkId(showUid)
            .ipGeoId((Integer) Defaults.GEO_ID.mask())
            .offerPrice((Integer) Defaults.OFFER_PRICE.mask())
            .vclusterId(Ids.generateVclusterId())
            .wareMd5(Ids.generateWareMD5())
            .fuid("")
            .testBuckets((String) Defaults.TEST_BUCKETS.mask())
            .reqId(Values.string("req_id_", 8))
            .wprid((String) Defaults.WPRID.mask())
            .userType(UserType.EXTERNAL.mask())
            .touch((Boolean) Defaults.TOUCH.mask())
            .showCookie(cookie)
            .ip6(IP.getIPv6FromIPv4(ip))
            .sbid((Integer) Defaults.SBID.mask())
            .showTime(clickTime.minusMinutes(5))
            .navCatId((Integer) Defaults.NAV_CAT_ID.mask())
            .uuid(Ids.generateUuid())
            .hostname((String) Defaults.HOST.mask())
            .pofRaw(pofRaw)
            .clid(clid)
            .distrType(distrType)
            .opp(opp)
            .filter(0)
            .ppOi(Integer.valueOf(PP.getRandomPPOi()))
            .state(1)
            .bestDeal(false)
            .bidType((String) Defaults.BID_TYPE.mask())
            .offerId(offerId)
            .feedId(Values.longNumber(5))
            .shopFee(Integer.valueOf(Values.longNumber(3)))
            .minFee(Integer.valueOf(Values.longNumber(2)))
            .urlType(UrlType.getRandomValue())
            .promoType(1)
            .supplier_type("")
            .rgb("GREEN")
            .icookie(cookie)
            ;
    }

}