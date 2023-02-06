package ru.yandex.chemodan.app.djfs.core.album;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTimeZone;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.geobase.Geobase;
import ru.yandex.inside.geobase.LinguisticsItem;
import ru.yandex.inside.geobase.RegionNode;
import ru.yandex.inside.geobase.RegionType;
import ru.yandex.misc.geo.Coordinates;
import ru.yandex.misc.ip.IpAddress;

public class MockGeobase implements Geobase {

    private MapF<Integer, RegionNode> regions;
    private MapF<Integer, ListF<Integer>> regionParentIds;
    private ListF<RegionCircle> regionCircles;

    public MockGeobase() {
        regions = Cf.hashMap();
        regionParentIds = Cf.hashMap();
        regionCircles = Cf.arrayList();
    }

    public void addRegion(int id, RegionType type, ListF<Integer> parentIds) {
        regions.put(
                id, new RegionNode(
                        "region",
                        "shortEnName",
                        Option.empty(),
                        Option.empty(),
                        id,
                        type,
                        true,
                        new Coordinates(42, 42),
                        Option.empty()
                )
        );
        regionParentIds.put(id, parentIds);
    }

    public void reset() {
        regionParentIds.clear();
        regions.clear();
        regionCircles.clear();
    }

    public void setRegionForCoordinates(Coordinates coordinates, float radius, int regionId) {
        regionCircles.add(new RegionCircle(coordinates, radius, regionId));
    }

    @Override
    public ListF<Integer> getRegionIdsByType(RegionType type) {
        return regions.filterValues(v -> v.getType() == type).keys();
    }

    @Override
    public Option<RegionNode> getRegionByIpAddress(IpAddress address) {
        return Option.empty();
    }

    @Override
    public Option<RegionNode> getRegionById(int regionId) {
        return regions.getO(regionId);
    }

    @Override
    public Option<DateTimeZone> getTimeZoneById(int regionId) {
        return Option.empty();
    }

    @Override
    public Option<Integer> getRegionIdByCoordinates(Coordinates coordinates) {
        for (RegionCircle circle : regionCircles) {
            if (circle.contains(coordinates)) {
                return Option.of(circle.getRegionId());
            }
        }

        return Option.empty();
    }

    @Override
    public ListF<Integer> getParentIdsById(int regionId) {
        return regionParentIds.getOrElse(regionId, Cf.list());
    }

    @Override
    public ListF<Integer> getChildrenIdsById(int regionId) {
        return Option.empty();
    }

    @Override
    public boolean isRegionInsideParentRegionById(int childRegionId, int parentRegionId) {
        return false;
    }

    @Override
    public boolean isIpAddressInsideRegionById(IpAddress address, int regionId) {
        return false;
    }

    @Override
    public Option<LinguisticsItem> getLinguisticsItemByRegionId(int regionId, String language) {
        return Option.of(new MockLinguisticsItem("<region name>"));
    }

    @Override
    public ListF<String> getSupportedLinguistics() {
        return Option.empty();
    }

    @Override
    public Option<Integer> findCountryId(int regionId) {
        return Option.empty();
    }

    @Override
    public Option<Integer> findCountryId(int regionId, String domain) {
        return Option.empty();
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    private class MockLinguisticsItem implements LinguisticsItem {

        private final String name;

        public MockLinguisticsItem(String name) {
            this.name = name;
        }

        @Override
        public String getNominativeCase() {
            return name;
        }

        @Override
        public String getGenitiveCase() {
            return "";
        }

        @Override
        public String getDativeCase() {
            return "";
        }

        @Override
        public String getPrepositionalCase() {
            return "";
        }

        @Override
        public String getPreposition() {
            return "";
        }

        @Override
        public String getLocativeCase() {
            return "";
        }

        @Override
        public String getDirectionalCase() {
            return "";
        }

        @Override
        public String getAblativeCase() {
            return "";
        }

        @Override
        public String getAccusativeCase() {
            return "";
        }

        @Override
        public String getInstrumentalCase() {
            return "";
        }
    }

    @RequiredArgsConstructor
    @Getter
    private class RegionCircle {
        private final Coordinates center;
        private final float radius;
        private final int regionId;

        public boolean contains(Coordinates point) {
            return Math.pow(point.getLatitude() - center.getLatitude(), 2) +
                    Math.pow(point.getLongitude() - center.getLongitude(), 2) <= radius;
        }
    }
}
