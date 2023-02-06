package ru.yandex.autotests.market.stat.beans.vendorclicks;

import org.apache.commons.lang.math.RandomUtils;
import ru.yandex.autotests.market.stat.attribute.BlockId;
import ru.yandex.autotests.market.stat.attribute.Cookie;
import ru.yandex.autotests.market.stat.attribute.Defaults;
import ru.yandex.autotests.market.stat.attribute.Ids;
import ru.yandex.autotests.market.stat.attribute.PP;
import ru.yandex.autotests.market.stat.attribute.Pof;
import ru.yandex.autotests.market.stat.attribute.Referrer;
import ru.yandex.autotests.market.stat.attribute.ShowUid;
import ru.yandex.autotests.market.stat.attribute.Url;
import ru.yandex.autotests.market.stat.attribute.UserType;
import ru.yandex.autotests.market.stat.attribute.ip.IP;

import java.time.LocalDateTime;

public class VendorClicksBuilder {


    public static VendorClicks.VendorClicksBuilder unique(LocalDateTime clickTime) {
        String cookie = Cookie.generateCookieForClickTime(clickTime);
        String ip = IP.generateValidNoYandexIPv4();
        String blockId = BlockId.generate(clickTime);
        String pp = PP.getRandomVendorClicks();
        String pof = Pof.getPof(pp);
        boolean usingPofAsJson = RandomUtils.nextBoolean();
        Integer clid = usingPofAsJson ? RandomUtils.nextInt(50) + 1501 : null;
        Integer distrType = usingPofAsJson ? 1 : null;
        Integer opp = usingPofAsJson ? 999 : null;
        String pofRaw = usingPofAsJson ? "{\"mclid\": null, \"clid\": [\"" + pof + "\", \"" + clid + "\"], \"distr_type\": " +
            distrType + ", \"opp\": " + opp + "}" : pof;

        return VendorClicks.builder()
            .rowid(Ids.uniqueRowId())
            .eventtime(clickTime)
            .referer(Referrer.generate())
            .url(Url.generateRandomUrl())
            .ip(IP.atonIPv4(ip).toString())
            .showUid(ShowUid.generate(blockId))
            .pp(Integer.valueOf(pp))
            .cookie(cookie)
            .geoId((Integer) Defaults.GEO_ID.mask())
            .hyperId(Ids.generateHyperId())
            .hyperCatId((Integer) Defaults.HYPER_CAT_ID.mask())
            .ipGeoId((Integer) Defaults.GEO_ID.mask())
            .userType(UserType.EXTERNAL.mask())
            .showCookie(cookie)
            .ip6(IP.getIPv6FromIPv4(ip))
            .showTime(clickTime.minusMinutes(5))
            .hostname(Url.generateRandomDomain())
            .filter(0)
            .brandId((Integer) Defaults.BRAND_ID.mask())
            .vendorDsId((Integer) Defaults.VENDOR_DS_ID.mask())
            .vendorPrice((Integer) Defaults.VENDOR_PRICE.mask())
            .vcBid((Integer) Defaults.VC_BID.mask())
            .pofRaw(pofRaw)
            .position(2)
            .supplier_type("")
            .rgb("")
            .state(1)
            .icookie("")
            .clid(clid)
            .distrType(distrType)
            .pof(Integer.valueOf(pof))
            ;
    }
}
