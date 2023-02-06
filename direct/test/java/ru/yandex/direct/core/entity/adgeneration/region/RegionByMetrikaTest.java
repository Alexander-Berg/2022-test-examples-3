package ru.yandex.direct.core.entity.adgeneration.region;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgeneration.model.RegionSuggest;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.region.repository.RegionRepository;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.regions.GeoTreeFactory;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgeneration.region.InputContainer.COUNTER_IDS;

@CoreTest
@RunWith(SpringRunner.class)
public class RegionByMetrikaTest {
    private RegionByMetrika regionByMetrika;
    private MetrikaClient metrikaClient;
    @Autowired
    public GeoTreeFactory geoTreeFactory;

    @Before
    public void before() {
        metrikaClient = mock(MetrikaClient.class);
        regionByMetrika = new RegionByMetrika(metrikaClient, mock(TurboLandingService.class),
                mock(BannersUrlHelper.class), mock(CampMetrikaCountersService.class),
                mock(RegionRepository.class), geoTreeFactory,
                mock(GeoBaseHelper.class), mock(PpcPropertiesSupport.class));
    }

    @Test
    public void testCorrectSortingWeight() {
        var counterIds = Set.of(12345L);
        // у всех регионов пользователей около 100, кроме 51го - у него 200
        Map<Long, Long> usersByRegion = Map.of(
                157L, 101L,
                11086L, 100L,
                213L, 99L,
                2L, 102L,
                51L, 200L);
        when(metrikaClient.getUserNumberByRegions(eq(counterIds), anyInt(), anyInt()))
                .thenReturn(usersByRegion);
        Collection<RegionSuggest> regionSuggests =
                regionByMetrika.generateRegionsInternal(new InputContainer().put(COUNTER_IDS, counterIds)).getResult();
        List<Long> regionIdsSortedByWeight = StreamEx.of(regionSuggests)
                .sortedByDouble(r -> -r.getWeight())
                .map(RegionSuggest::getRegionId)
                .toList();
        List<Long> regionIdsSortedByUsers = EntryStream.of(usersByRegion)
                .sortedByLong(entry -> -entry.getValue())
                .keys()
                .toList();
        // проверяем, что сортировка регионов по кол-ву пользователей
        // соотносится с сортировкой регионов по вычисленным весам
        Assertions.assertThat(regionIdsSortedByWeight).isEqualTo(regionIdsSortedByUsers);
    }
}
