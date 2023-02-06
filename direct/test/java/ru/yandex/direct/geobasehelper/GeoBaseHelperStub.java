package ru.yandex.direct.geobasehelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.geobase.CrimeaStatus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

@ParametersAreNonnullByDefault
public class GeoBaseHelperStub extends GeoBaseHelper {
    public static final String RUSSIAN_LANGUAGE = "RU";

    private Map<Long, List<Integer>> parentRegionIdsByRegionId = new HashMap<>();
    private Map<RegionLang, String> regionNameByIdAndLang = new HashMap<>();
    private Map<Pair<Double, Double>, Long> regionByCoordinates = new HashMap<>();
    private Map<Pair<Pair<Double, Double>, Pair<Double, Double>>, Double> distanceByCoordinatesPair = new HashMap<>();
    private Map<Long, Pair<Double, Double>> coordinatesByRegionId = new HashMap<>();
    private Map<String, Long> regionIdByIp = new HashMap<>();
    private final Map<Long, String> timezoneByRegionId = new HashMap<>();
    private final Map<Long, Long> chiefRegionIdByRegionId = new HashMap<>();

    public GeoBaseHelperStub(GeoTreeFactory geoTreeFactory) {
        super(geoTreeFactory);
        // Заполняем минимальную конфигурацию из Москвы, Санкт-Петербурга и России без промежуточных регионов
        addRegionWithParent(MOSCOW_REGION_ID, singletonList((int) RUSSIA_REGION_ID));

        addRegionWithName(MOSCOW_REGION_ID, RUSSIAN_LANGUAGE, "Москва");

        addRegionWithParent(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID,
                singletonList((int) RUSSIA_REGION_ID));
        addRegionWithName(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, RUSSIAN_LANGUAGE, "Санкт-Петербург");

        addRegionWithName(RUSSIA_REGION_ID, RUSSIAN_LANGUAGE, "Россия");

        addTimezone(MOSCOW_REGION_ID, MOSCOW_TIMEZONE);
        addTimezone(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, MOSCOW_TIMEZONE);
        addTimezone(RUSSIA_REGION_ID, StringUtils.EMPTY);

        addChiefRegionId(MOSCOW_REGION_ID, GLOBAL_REGION_ID);
        addChiefRegionId(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, SAINT_PETERSBURG_REGION_ID);
        addChiefRegionId(RUSSIA_REGION_ID, MOSCOW_REGION_ID);
    }

    @Override
    public double getDistanceBetweenCoordinates(
            double latitude0,
            double longitude0,
            double latitude1,
            double longitude1
    ) {
        return distanceByCoordinatesPair
                .getOrDefault(Pair.of(Pair.of(latitude0, longitude0), Pair.of(latitude1, longitude1)),0D);
    }

    @Override
    public long getRegionIdByCoordinates(double latitude, double longitude) {
        return regionByCoordinates.getOrDefault(Pair.of(latitude, longitude), 0L);
    }

    public void addRegionWithCoordinates(double latitude, double longitude, long regionId) {
        regionByCoordinates.put(Pair.of(latitude, longitude), regionId);
    }

    public void addRegionWithParent(Long regionId, List<Integer> parentRegionIds) {
        parentRegionIdsByRegionId.put(regionId, parentRegionIds);
    }

    public void addRegionIdWithIp(Long regionId, String ip) {
        regionIdByIp.put(ip, regionId);
    }

    public void addCoordinatesWithDistance(
            double latitude0,
            double longitude0,
            double latitude1,
            double longitude1,
            double distance
    ) {
        distanceByCoordinatesPair
                .put(Pair.of(Pair.of(latitude0, longitude0), Pair.of(latitude1, longitude1)), distance);
    }

    public void addCoordinatesByRegionId(double latitude, double longitude, long regionId) {
        coordinatesByRegionId.put(regionId, Pair.of(latitude, longitude));
    }

    public void addRegionWithName(Long regionId, String lang, String regionName) {
        checkArgument(ALL_LANGUAGES.contains(lang), "Language '%s' is unknown.", lang);
        RegionLang regionLang = new RegionLang(regionId, lang);
        regionNameByIdAndLang.put(regionLang, regionName);
    }

    public void addTimezone(Long regionId, String timezone) {
        timezoneByRegionId.put(regionId, timezone);
    }

    public void addChiefRegionId(Long regionId, Long chiefRegionId) {
        chiefRegionIdByRegionId.put(regionId, chiefRegionId);
    }

    @Override
    public List<Integer> getParentRegionIds(Long regionId) {
        return parentRegionIdsByRegionId.getOrDefault(regionId, Collections.emptyList());
    }

    @Override
    public List<Integer> getParentRegionIds(Long regionId, @Nullable CrimeaStatus crimeaStatus) {
        return getParentRegionIds(regionId);
    }

    @Override
    public String getRegionName(Long regionId) {
        return getRegionName(regionId, RUSSIAN_LANGUAGE);
    }

    @Override
    public String getRegionName(Long regionId, String lang) {
        checkArgument(ALL_LANGUAGES.contains(lang), "Language '%s' is unknown.", lang);
        RegionLang regionLang = new RegionLang(regionId, lang);
        return regionNameByIdAndLang.getOrDefault(regionLang, "");
    }

    @Override
    public int getCountryId(Long regionId) {
        checkArgument(parentRegionIdsByRegionId.containsKey(regionId), "Unknown regionId: %s", regionId);
        return Iterables.getLast(parentRegionIdsByRegionId.get(regionId));
    }

    @Override
    public String getPhoneCodeByRegionId(Long regionId) {
        return "7";
    }

    @Override
    public Pair<Double, Double> getCoordinatesByRegionId(long regionId) {
        return coordinatesByRegionId.getOrDefault(regionId, Pair.of(0D, 0D));
    }

    @Override
    public long getRegionIdByIp(String ip) {
        return regionIdByIp.getOrDefault(ip, 0L);
    }

    @Override
    public String getTimezoneByRegionId(Long regionId) {
        return timezoneByRegionId.getOrDefault(regionId, StringUtils.EMPTY);
    }

    @Override
    public long getChiefRegionId(Long regionId) {
        return chiefRegionIdByRegionId.getOrDefault(regionId, GLOBAL_REGION_ID);
    }

    private class RegionLang {
        private final Long regionId;
        private final String lang;

        private RegionLang(Long regionId, String lang) {
            this.regionId = regionId;
            this.lang = lang.toUpperCase();
        }

        public Long getRegionId() {
            return regionId;
        }

        public String getLang() {
            return lang;
        }

        @Override
        public int hashCode() {
            return regionId.hashCode() ^ lang.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RegionLang)) {
                return false;
            }
            RegionLang regionLang = (RegionLang) obj;
            return regionId.equals(regionLang.regionId)
                    && lang.equals(regionLang.lang);
        }
    }

}
