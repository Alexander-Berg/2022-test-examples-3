package ru.yandex.market.crm.core.test;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

import ru.yandex.market.crm.core.services.geo.domain.GeoData;
import ru.yandex.market.crm.core.services.geo.domain.GeoRegion;
import ru.yandex.market.crm.core.suppliers.GeoDataSupplier;

/**
 * @author apershukov
 */
public class TestGeoDataSupplier implements GeoDataSupplier {

    private GeoData data;

    public TestGeoDataSupplier() {
        GeoRegion moscow = new GeoRegion(
                213,
                0,
                "Москве",
                "Europe/Moscow"
        );

        Int2ObjectMap<GeoRegion> regions = new Int2ObjectArrayMap<>();
        regions.put(moscow.getId(), moscow);

        this.data = new GeoData(regions, Int2ObjectMaps.emptyMap());
    }

    @Override
    public GeoData get() {
        return data;
    }
}
