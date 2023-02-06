package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.geo.model.GeoRegion;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.GeoRegions.GEO_REGIONS;

public class TestGeoRegionRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestGeoRegionRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public void addRegion(GeoRegion geoRegion) {
        dslContextProvider.ppcdict()
                .insertInto(GEO_REGIONS)
                .set(GEO_REGIONS.REGION_ID, geoRegion.getId())
                .set(GEO_REGIONS.PARENT_ID, geoRegion.getParentId())
                .set(GEO_REGIONS.TYPE, geoRegion.getType().getTypedValue())
                .set(GEO_REGIONS.OFFICE_ID, geoRegion.getOfficeId())
                .set(GEO_REGIONS.NAME, geoRegion.getName())
                .set(GEO_REGIONS.ENAME, geoRegion.getEname())
                .set(GEO_REGIONS.UA_NAME, geoRegion.getUaname())
                .set(GEO_REGIONS.TR_NAME, geoRegion.getTrname())
                .onDuplicateKeyIgnore()
                .execute();
    }
}
