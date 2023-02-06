package ru.yandex.direct.core.testing.data;

import static ru.yandex.direct.regions.Region.CENTRAL_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.FAR_EASTERN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.NORTH_CAUCASIAN_FEDERAL_DISTRICT;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.SIBERIAN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.regions.Region.URAL_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.VOLGA_FEDERAL_DISTRICT_REGION_ID;

public final class TestRegions {
    public static final long RUSSIA = RUSSIA_REGION_ID;
    public static final long UKRAINE = UKRAINE_REGION_ID;

    public static final long NORTHWESTERN_DISTRICT = NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID;
    public static final long CENTRAL_DISTRICT = CENTRAL_FEDERAL_DISTRICT_REGION_ID;
    public static final long SOUTH_DISTRICT = SOUTH_FEDERAL_DISTRICT_REGION_ID;
    public static final long URAL_DISTRICT = URAL_FEDERAL_DISTRICT_REGION_ID;
    public static final long SIBERIAN_DISTRICT = SIBERIAN_FEDERAL_DISTRICT_REGION_ID;
    public static final long FAR_EASTERN_DISTRICT = FAR_EASTERN_FEDERAL_DISTRICT_REGION_ID;
    public static final long VOLGA_DISTRICT = VOLGA_FEDERAL_DISTRICT_REGION_ID;
    public static final long NORTH_CAUCASIAN_DISTRICT = NORTH_CAUCASIAN_FEDERAL_DISTRICT;

    public static final long SAINT_PETERSBURG_PROVINCE = SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
    public static final long CRIMEA_PROVINCE = CRIMEA_REGION_ID;

    // SOUTH_DISTRICT children
    public static final Long ASTRAKHAN_PROVINCE = 10946L;
    public static final Long VOLGOGRAD_PROVINCE = 10950L;
    public static final Long KRASNODAR_KRAI = 10995L;
    public static final Long REPUBLIC_OF_ADYGEA = 11004L;
    public static final Long REPUBLIC_OF_KALMIKIA = 11015L;
    public static final Long ROSTOV_PROVINCE = 11029L;

    // VOLGA_DISTRICT children
    public static final Long KIROV_PROVINCE = 11070L;

    public static final Long URYUPINSK_REGION_ID = 10981L;

}
