package ru.yandex.market.tsum.registry.v2;

import ru.yandex.market.tsum.registry.v2.dao.model.autodelete.LegacyNannyService;

import java.time.Instant;

public class TestLegacyNannyServiceBuilder {

    private LegacyNannyService legacyNannyService;

    public TestLegacyNannyServiceBuilder() {
        legacyNannyService = new LegacyNannyService();
    }

    public LegacyNannyService build() {
        return legacyNannyService;
    }

    public TestLegacyNannyServiceBuilder withDataToDelete(Instant timestamp) {
        this.legacyNannyService.setTimestamp(timestamp);
        return this;
    }

    public TestLegacyNannyServiceBuilder withServicesName(String servicesName) {
        this.legacyNannyService.setServiceName(servicesName);
        return this;
    }

    public static TestLegacyNannyServiceBuilder builder() {
        return new TestLegacyNannyServiceBuilder();
    }
}
