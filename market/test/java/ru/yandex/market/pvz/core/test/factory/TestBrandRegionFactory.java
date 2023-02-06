package ru.yandex.market.pvz.core.test.factory;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.domain.pickup_point.branding.BrandRegion;
import ru.yandex.market.pvz.core.domain.pickup_point.branding.BrandRegionRepository;

@Transactional
public class TestBrandRegionFactory {

    public static final List<BrandRegionTestParams> DEFAULT_REGIONS = List.of(
            new BrandRegionTestParams("Город Москва (в пределах МКАД)", 75),
            new BrandRegionTestParams("Ближняя Московская область (МКАД +30 км)", 65),
            new BrandRegionTestParams("Московская область – остальные города", 60),
            new BrandRegionTestParams("Санкт-Петербург", 65),
            new BrandRegionTestParams("Ленинградская область", 60),
            new BrandRegionTestParams("Прочие города-миллионники", 60)
    );

    @Autowired
    private BrandRegionRepository brandRegionRepository;

    public void createDefaults() {
        List<String> createdRegions = brandRegionRepository.findAll().stream()
                .map(BrandRegion::getRegion)
                .collect(Collectors.toList());

        boolean defaultsCreated = createdRegions.containsAll(DEFAULT_REGIONS.stream()
                .map(BrandRegionTestParams::getRegion)
                .collect(Collectors.toList()));

        if (defaultsCreated) {
            return;
        }

        brandRegionRepository.saveAll(
                StreamEx.of(DEFAULT_REGIONS)
                        .map(p -> new BrandRegion(p.getRegion(), p.getDailyTransmissionThreshold()))
                        .toList()
        );
    }

    public BrandRegion create(BrandRegionTestParams params) {
        return brandRegionRepository.save(new BrandRegion(params.getRegion(), params.getDailyTransmissionThreshold()));
    }

    @Data
    @Builder
    public static class BrandRegionTestParams {

        @Builder.Default
        private String region;

        @Builder.Default
        private int dailyTransmissionThreshold;
    }
}
