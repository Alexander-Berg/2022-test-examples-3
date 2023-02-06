package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;

@Value
@Builder
public class Cis {

    public static int CIS_CARGO_TYPE_UNKNOWN = 0;
    public static int CIS_CARGO_TYPE_REQUIRED = 980;
    public static int CIS_CARGO_TYPE_OPTIONAL = 990;

    String gtin;
    String withoutCryptoPart;
    String full;

    public static Cis of(final String gtin) {
        final Pair<String, String> generatedCis = RandomUtil.generateCis(gtin);
        return Cis.builder()
                .gtin(gtin)
                .withoutCryptoPart(generatedCis.getLeft())
                .full(generatedCis.getRight())
                .build();
    }
}
