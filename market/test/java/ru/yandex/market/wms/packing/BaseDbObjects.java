package ru.yandex.market.wms.packing;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.packing.pojo.Carrier;
import ru.yandex.market.wms.packing.pojo.Sku;

public class BaseDbObjects {

    public static final String SKU_DESCRIPTION = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";

    public static final Sku SKU101 = new Sku("STORER1", "SKU101", "ALL", SKU_DESCRIPTION, 1.01, true,
            newSet("EAN101-1", "EAN101-2"), 10, 10, 10, 10);
    public static final Sku SKU102 = new Sku("STORER1", "SKU102", "ALL", SKU_DESCRIPTION, 1.02, true,
            newSet("EAN102-1", "EAN102-2"), 20, 20, 20, 15);
    public static final Sku SKU201 = new Sku("STORER2", "SKU201", "ALL", SKU_DESCRIPTION, 2.01, true,
            newSet("EAN201-1", "EAN201-2"), 10, 10, 10, 20);
    public static final Sku SKU202 = new Sku("STORER2", "SKU202", "ALL", SKU_DESCRIPTION, 2.02, true,
            newSet("EAN202-1", "EAN202-2"), 20, 20, 20, 20);

    public static final Carrier CARRIER_MP1 = Carrier.builder()
            .storerKey("CARRIER-MP1")
            .company("СД-МП")
            .cartonGroup("PK")
            .supportsMultiPackaging(true)
            .build();
    public static final Carrier CARRIER_SP1 = Carrier.builder()
            .storerKey("CARRIER-SP1")
            .company("СД-1П")
            .cartonGroup("PK")
            .supportsMultiPackaging(false)
            .build();

    public static final String MORNING_CUTOFF_TIME = "12:00";
    public static final String CARRIER_MP1_SHIPPING_CUTOFF = "23:00:00";
    public static final String CARRIER_SP1_SHIPPING_CUTOFF = "05:30:00";

    public static final Carton CARTON_YMA = Carton.builder()
            .group("PK").type("YMA").length(80).width(50).height(35).build();
    public static final Carton CARTON_YMB = Carton.builder()
            .group("PK").type("YMB").length(100).width(60).height(40).build();
    public static final Carton CARTON_YMC = Carton.builder()
            .group("PK").type("YMC").length(120).width(70).height(45).build();

    private BaseDbObjects() {
    }

    private static Set<String> newSet(String... strings) {
        return Arrays.stream(strings).collect(Collectors.toSet());
    }
}
