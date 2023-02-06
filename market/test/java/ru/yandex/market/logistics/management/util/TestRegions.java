package ru.yandex.market.logistics.management.util;

import lombok.experimental.UtilityClass;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;

import static ru.yandex.common.util.region.RegionType.CITY;
import static ru.yandex.common.util.region.RegionType.CITY_DISTRICT;
import static ru.yandex.common.util.region.RegionType.COUNTRY;
import static ru.yandex.common.util.region.RegionType.METRO_STATION;
import static ru.yandex.common.util.region.RegionType.OTHERS_UNIVERSAL;
import static ru.yandex.common.util.region.RegionType.REGION;
import static ru.yandex.common.util.region.RegionType.SECONDARY_DISTRICT;
import static ru.yandex.common.util.region.RegionType.SUBJECT_FEDERATION;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class TestRegions {

    public static final int EARTH_ID = 1;
    public static final int RUSSIA_ID = 2;
    public static final int MOSCOW_REGION_ID = 3;
    public static final int MOSCOW_ID = 4;
    public static final int SPB_ID = 10;
    public static final int NEVSKY_PROSPEKT_ID = 11;
    public static final int NON_EXISTS_REGION_ID = 6;
    public static final int BALTIC_ID = 100;
    public static final int LATVIA_ID = 101;
    public static final int RIGA_ID = 102;
    public static final int NORTH_DISTRICT_ID = 103;
    public static final int SARKANDAUGAVA_ID = 104;
    public static final int NOVOSIBIRSK_ID = 65;

    public static final Region EARTH = new Region(EARTH_ID, "Земля", OTHERS_UNIVERSAL, null);
    public static final Region RUSSIA = new Region(RUSSIA_ID, "Россия", COUNTRY, EARTH);
    public static final Region MO =
        new Region(MOSCOW_REGION_ID, "Москва и Московская область", SUBJECT_FEDERATION, RUSSIA);
    public static final Region MOSCOW = new Region(MOSCOW_ID, "Москва", CITY, MO);
    public static final Region SPB = new Region(SPB_ID, "Санкт-Петербург", CITY, RUSSIA);
    public static final Region METRO_STATION_PARK_KULTURY =
        new Region(NEVSKY_PROSPEKT_ID, "Невский проспект", METRO_STATION, SPB);
    public static final Region BALTIC = new Region(BALTIC_ID, "Прибалтика", REGION, EARTH);
    public static final Region LATVIA = new Region(LATVIA_ID, "Латвия", COUNTRY, BALTIC);
    public static final Region RIGA = new Region(RIGA_ID, "Рига", CITY, LATVIA);
    public static final Region NORTH_DISTRICT =
        new Region(NORTH_DISTRICT_ID, "Северный район", CITY_DISTRICT, RIGA);
    public static final Region SARKANDAUGAVA =
        new Region(SARKANDAUGAVA_ID, "Саркандаугава", SECONDARY_DISTRICT, NORTH_DISTRICT);
    public static final Region NOVOSIBIRSK =
        new Region(NOVOSIBIRSK_ID, "Новосибирск", CITY, RUSSIA);

    /**
     * Каждый регион входит в множество и своих предков, и своих потомков
     * <p>
     * <p>
     * EARTH
     * ^
     * |
     * RUSSIA
     * ^
     * /   \
     * /   MOSCOW_REGION
     * /         ^
     * /          |
     * SPB        MOSCOW
     */
    public static RegionTree<Region> buildRegionTree() {
        return new RegionTree<>(EARTH);
    }
}
