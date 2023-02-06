package ru.yandex.market.tpl.dora.test.factory;


import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.dora.db.jooq.tables.pojos.Platform;
import ru.yandex.market.tpl.dora.domain.feature.FeatureCommandService;
import ru.yandex.market.tpl.dora.domain.platform.PlatformCommandService;

public class TestPlatformFactory {

    @Autowired
    private PlatformCommandService platformCommandService;

    @Autowired
    private FeatureCommandService featureCommandService;

    public Platform create() {
        return create(TestPlatformFactory.PlatformTestParams.builder().build());
    }

    public Platform create(TestPlatformFactory.PlatformTestParams params) {
        return platformCommandService.create(params.getPlatformName());
    }

    public Platform createWithFeatures(List<String> featureNames) {
        return createWithFeatures(TestPlatformFactory.PlatformTestParams.builder().build(), featureNames);
    }

    public Platform createWithFeatures(TestPlatformFactory.PlatformTestParams params, List<String> featureNames) {
        Platform platform = create(params);
        for (var name : featureNames) {
            featureCommandService.create(platform.getId(), name);
        }
        return platform;
    }

    @Data
    @Builder
    public static class PlatformTestParams {

        public static final String DEFAULT_PLATFORM_NAME = "DORA";

        @Builder.Default
        private String platformName = DEFAULT_PLATFORM_NAME;
    }

}
