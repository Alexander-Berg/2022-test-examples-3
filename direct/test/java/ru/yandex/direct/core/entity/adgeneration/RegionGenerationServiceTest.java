package ru.yandex.direct.core.entity.adgeneration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import one.util.streamex.EntryStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgeneration.model.RegionSuggest;
import ru.yandex.direct.core.entity.adgeneration.region.InputContainer;
import ru.yandex.direct.core.entity.adgeneration.region.RegionSourceStub;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.regions.Region;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.common.db.PpcPropertyNames.SUGGEST_REGION_SOURCES;
import static ru.yandex.direct.common.db.PpcPropertyNames.SUGGEST_REGION_TYPES;
import static ru.yandex.direct.common.db.PpcPropertyNames.SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_SOURCE;
import static ru.yandex.direct.common.db.PpcPropertyNames.SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_TYPE_AND_SOURCE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RegionGenerationServiceTest {

    private RegionGenerationService service;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Test
    public void enabledSources() {
        ppcPropertiesSupport.set(SUGGEST_REGION_SOURCES, "source2,source3,source4");
        createRegionGenerationService(Map.of(
                "source1", List.of(
                        createRegionSuggest(1, 3, 1.0)),
                "source2", List.of(
                        createRegionSuggest(2, 3, 1.0)),
                "source3", List.of(
                        createRegionSuggest(3, 3, 1.0))
        ));
        Map<String, Object> additionalInfo = new HashMap<>();
        var results = service.generateRegions(null, new InputContainer(), additionalInfo, null);
        assertThat(results.getResult().size(), equalTo(2));
    }

    @Test
    public void enabledRegionTypes() {
        ppcPropertiesSupport.set(SUGGEST_REGION_SOURCES, "source1");
        ppcPropertiesSupport.set(SUGGEST_REGION_TYPES, "3,6");
        List<RegionSuggest> suggests = IntStream.range(1,10).mapToObj(type -> createRegionSuggest(type, type, 1.0)).collect(Collectors.toList());
        createRegionGenerationService(Map.of("source1", suggests));
        Map<String, Object> additionalInfo = new HashMap<>();
        var results = service.generateRegions(null, new InputContainer(), additionalInfo, null);
        assertThat(results.getResult().size(), equalTo(2));
        Iterator<RegionSuggest> it = results.getResult().iterator();
        checkSuggest(it.next(), 6L, 6, null);
        checkSuggest(it.next(), 3L, 3, null);
    }

    @Test
    public void weightMultiplier() {
        ppcPropertiesSupport.set(SUGGEST_REGION_SOURCES, "source1,source2,source3");
        ppcPropertiesSupport.set(SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_SOURCE, "source2=0.2");
        ppcPropertiesSupport.set(SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_TYPE_AND_SOURCE, "3:source3=0.3;4:source3=0.4;5:source3=0.5;6:source3=0.6");
        createRegionGenerationService(Map.of(
                "source1", List.of(
                        createRegionSuggest(1, 3, 1.0)),
                "source2", List.of(
                        createRegionSuggest(2, 3, 1.0)),
                "source3", List.of(
                        createRegionSuggest(3, 3, 1.0),
                        createRegionSuggest(4, 4, 1.0),
                        createRegionSuggest(5, 5, 1.0),
                        createRegionSuggest(6, 6, 1.0))
        ));
        Map<String, Object> additionalInfo = new HashMap<>();
        var results = service.generateRegions(null, new InputContainer(), additionalInfo, null);
        assertThat(results.getResult().size(), equalTo(6));
        Iterator<RegionSuggest> it = results.getResult().iterator();
        checkSuggest(it.next(), 6L, 6, 0.6);
        checkSuggest(it.next(), 5L, 5, 0.5);
        checkSuggest(it.next(), 4L, 4, 0.4);
        checkSuggest(it.next(), 3L, 3, 0.3);
        checkSuggest(it.next(), 2L, 3, 0.2);
        checkSuggest(it.next(), 1L, 3, 0.1);
    }

    @Test
    public void sumWeights() {
        ppcPropertiesSupport.set(SUGGEST_REGION_SOURCES, "source1,source2");
        ppcPropertiesSupport.set(SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_SOURCE, "source1=1.0,source2=1.0");
        ppcPropertiesSupport.set(SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_TYPE_AND_SOURCE, "");
        createRegionGenerationService(Map.of(
                "source1", List.of(
                        createRegionSuggest(1, 3, 0.5),
                        createRegionSuggest(2, 4, 0.7)),
                "source2", List.of(
                        createRegionSuggest(1, 3, 0.5),
                        createRegionSuggest(2, 4, 0.8))
        ));
        Map<String, Object> additionalInfo = new HashMap<>();
        var results = service.generateRegions(null, new InputContainer(), additionalInfo, null);
        assertThat(results.getResult().size(), equalTo(2));
        Iterator<RegionSuggest> it = results.getResult().iterator();
        checkSuggest(it.next(), 2L, 4, 0.94);
        checkSuggest(it.next(), 1L, 3, 0.75);
    }

    @Test
    public void sortResults() {
        ppcPropertiesSupport.set(SUGGEST_REGION_SOURCES, "source1");
        ppcPropertiesSupport.set(SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_SOURCE, "source1=1.0");
        ppcPropertiesSupport.set(SUGGEST_REGION_WEIGHT_MULTIPLIER_BY_TYPE_AND_SOURCE, "");
        createRegionGenerationService(Map.of(
                "source1", List.of(
                        createRegionSuggest(1, 6, 0.5),
                        createRegionSuggest(2, 6, 0.3),
                        createRegionSuggest(3, 6, 0.2),
                        createRegionSuggest(4, 6, 0.1),
                        createRegionSuggest(5, 5, 0.10),
                        createRegionSuggest(6, 5, 0.09),
                        createRegionSuggest(7, 5, 0.08),
                        createRegionSuggest(8, 4, 0.4),
                        createRegionSuggest(9, 4, 0.3),
                        createRegionSuggest(10, 4, 0.29),
                        createRegionSuggest(11, 3, 0.6),
                        createRegionSuggest(12, 3, 0.5),
                        createRegionSuggest(13, 3, 0.4),
                        createRegionSuggest(14, 3, 0.35),
                        createRegionSuggest(15, 3, 0.34),
                        createRegionSuggest(16, 3, 0.33),
                        createRegionSuggest(17, 3, 0.32),
                        createRegionSuggest(18, 3, 0.31),
                        createRegionSuggest(19, 3, 0.30),
                        createRegionSuggest(20, 3, 0.28),
                        createRegionSuggest(21, 3, 0.27))
        ));
        Map<String, Object> additionalInfo = new HashMap<>();
        var results = service.generateRegions(null, new InputContainer(), additionalInfo, null);
        assertThat(results.getResult().size(), equalTo(20));
        Iterator<RegionSuggest> it = results.getResult().iterator();

        checkSuggest(it.next(), 1L, 6, 0.5);
        checkSuggest(it.next(), 2L, 6, 0.3);

        checkSuggest(it.next(), 5L, 5, 0.10);

        checkSuggest(it.next(), 8L, 4, 0.4);
        checkSuggest(it.next(), 9L, 4, 0.3);

        checkSuggest(it.next(), 11L, 3, 0.6);
        checkSuggest(it.next(), 12L, 3, 0.5);
        checkSuggest(it.next(), 13L, 3, 0.4);

        checkSuggest(it.next(), 14L, 3, 0.35);
        checkSuggest(it.next(), 15L, 3, 0.34);
        checkSuggest(it.next(), 16L, 3, 0.33);
        checkSuggest(it.next(), 17L, 3, 0.32);
        checkSuggest(it.next(), 18L, 3, 0.31);
        checkSuggest(it.next(), 19L, 3, 0.30);
        checkSuggest(it.next(), 10L, 4, 0.29);
        checkSuggest(it.next(), 20L, 3, 0.28);
        checkSuggest(it.next(), 21L, 3, 0.27);
        checkSuggest(it.next(), 3L, 6, 0.2);
        checkSuggest(it.next(), 4L, 6, 0.1);
        checkSuggest(it.next(), 6L, 5, 0.09);
    }

    private void createRegionGenerationService(Map<String, List<RegionSuggest>> sourceToSuggests) {
        service = new RegionGenerationService(null, ppcPropertiesSupport, EntryStream.of(sourceToSuggests)
                .map(entry -> new RegionSourceStub(entry.getKey(), entry.getValue()))
                .toList());
    }

    private RegionSuggest createRegionSuggest(long regionId, int regionType, double weight) {
        Region region = mock(Region.class);
        when(region.getId()).thenReturn(regionId);
        when(region.getType()).thenReturn(regionType);
        return new RegionSuggest(region).multiWeight(weight);
    }

    private void checkSuggest(RegionSuggest suggest, Long regionId, Integer regionType, Double weight) {
        if (regionId != null) {
            assertThat(suggest.getRegionId(), equalTo(regionId));
        }
        if (regionType != null) {
            assertThat(suggest.getRegionType(), equalTo(regionType));
        }
        var w = suggest.getWeight();
        if (weight != null) {
            assertTrue(Math.abs(suggest.getWeight() - weight) < 0.0001);
        }
    }
}
