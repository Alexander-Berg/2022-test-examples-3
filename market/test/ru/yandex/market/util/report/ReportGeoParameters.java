package ru.yandex.market.util.report;

import ru.yandex.market.checkout.util.report.generators.geo.GeoGeneratorParameters;
import ru.yandex.market.common.report.model.outlet.Outlet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportGeoParameters {
    private long shopId;
    private Map<String, ReportGeoParametersEntry> entries = new LinkedHashMap<>();

    public void setResourceUrl(String wareMd5, String resourceUrl) {
        setResourceUrl(wareMd5, (List<Outlet>) null);
    }

    public void setResourceUrl(String wareMd5, List<Outlet> outlets) {
        entries.put(wareMd5, new ReportGeoParametersEntry(wareMd5, outlets));
    }

    List<ReportGeoParametersEntry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public long getShopId() {
        return shopId;
    }

    public void setShopId(long shopId) {
        this.shopId = shopId;
    }

    static class ReportGeoParametersEntry implements GeoGeneratorParameters {
        private final String wareMd5;
        private final List<Outlet> outlets;

        public ReportGeoParametersEntry(String wareMd5,
                                        List<Outlet> outlets) {
            this.wareMd5 = wareMd5;
            this.outlets = outlets;
        }

        public String getWareMd5() {
            return wareMd5;
        }

        @Override
        public List<Outlet> getOutlets() {
            return outlets;
        }
    }
}
