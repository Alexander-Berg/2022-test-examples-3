package ru.yandex.market.antifraud.filter;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.lang.reflect.Field;
import java.util.Set;

@Data
@ToString
public class TestShow {
    private static final Set<String> sysFields =
            ImmutableSet.of("filter", "fieldToCol", "sysFields");
    public static final BiMap<String, String> fieldToCol;

    static {
        ImmutableBiMap.Builder<String, String> b = ImmutableBiMap.builder();
        for(Field f: TestShow.class.getDeclaredFields()) {
            if(!sysFields.contains(f.getName())) {
                b.put(f.getName(), convertFieldToColName(f.getName()));
            }
        }
        fieldToCol = b.build();
    }

    @SneakyThrows
    public static Object getFieldValueByCol(TestShow testShow, String column) {
        if(!fieldToCol.inverse().containsKey(column)) {
            throw new IllegalArgumentException("No such column in TestShow.class " + column);
        }
        Field f = TestShow.class.getDeclaredField(fieldToCol.inverse().get(column));
        f.setAccessible(true);
        if(column.equals("event_time")) {
            return ((DateTime) f.get(testShow)).getMillis()/1000;
        }
        return f.get(testShow);
    }

    private static String convertFieldToColName(String f) {
        switch (f) {
            case "eventtime": return "event_time";
            default: return upperToUnderscore(f);
        }
    }

    public static String upperToUnderscore(String f) {
        StringBuilder fixed = new StringBuilder(f.length() * 2);
        for(int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if(i > 0 && Character.isUpperCase(c)) {
                fixed.append('_');
                fixed.append(Character.toLowerCase(c));
            }
            else {
                fixed.append(c);
            }
        }

        return fixed.toString();
    }

    private String rowid;
    private DateTime eventtime;
    private String ip;
    private String cookie;
    private String showBlockId;
    private String showUid;
    private String goodsCount;
    private String url;
    private String goodsTitle;
    private Integer categId;
    private String discount;
    private Integer pp;
    private Integer geoId;
    private Integer shopId;
    private String priceClick;
    private Integer pof;
    private Integer state;
    private FilterConstants filter;
    private Integer hyperId;
    private Integer onstock;
    private Integer bid;
    private Integer autobrokerEnabled;
    private String wareId;
    private String ctx;
    private String generation;
    private String hostname;
    private String offerPrice;
    private String testTag;
    private String vclusterId;
    private String context;
    private String wareMd5;
    private String fuid;
    private String cpa;
    private String testBuckets;
    private String linkId;
    private String reqId;
    private String wprid;
    private String userType;
    private String oldPrice;
    private Integer ipGeoId;
    private String utmSource;
    private String utmMedium;
    private String utmTerm;
    private String utmCampaign;
    private Boolean touch;
    private Integer sbid;
    private Integer homeRegion;
    private String subRequestId;
    private String bsBlockId;
    private String position;
    private String mnCtr;
    private String uuid;
    private Integer navCatId;
    private String bestDeal;
    private String cbid;
    private String rankedWith;
    private String isPriceFrom;
    private String pofRaw;
    private String vid;
    private String clid;
    private String distrType;
    private String typeId;
    private String minBid;
    private String normalizedByDnormQuery;
    private String normalizedToLowerQuery;
    private String normalizedToLowerAndSortedQuery;
    private String normalizedBySynnormQuery;
    private Integer ppOi;
    private Long fee;
    private Long shopFee;
    private Long minFee;
    private Integer urlType;
    private String phoneClickRatio;
    private String phoneThreshold;
    private Long feedId;
    private String vendorDsId;
    private String vendorPrice;
    private String vcBid;
    private String urlHash;
    private String originalQuery;
    private String yandexUid;
    private Integer recordType;
}
