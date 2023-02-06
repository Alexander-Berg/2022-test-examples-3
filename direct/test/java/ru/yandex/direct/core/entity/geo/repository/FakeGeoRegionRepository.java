package ru.yandex.direct.core.entity.geo.repository;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.geo.model.GeoRegion;
import ru.yandex.direct.regions.Region;

public final class FakeGeoRegionRepository implements IGeoRegionRepository {
    private static final Map<Long, GeoRegion> regions = Map.of(
            Region.RUSSIA_REGION_ID, new GeoRegion().withId(Region.RUSSIA_REGION_ID),
            Region.TURKEY_REGION_ID, new GeoRegion().withId(Region.TURKEY_REGION_ID));

    @Override
    public Collection<GeoRegion> getGeoRegionsByIds(Collection<Long> ids) {
        return ids.stream().filter(regions::containsKey).map(regions::get).collect(Collectors.toList());
    }
}
