package ru.yandex.market.tpl.core.service.region;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.region.RegionMeta;
import ru.yandex.market.tpl.core.domain.region.actualization.RegionBorderHistory;
import ru.yandex.market.tpl.core.domain.region.actualization.RegionBorderHistoryRepository;
import ru.yandex.market.tpl.core.domain.region.actualization.TplRegionBorderGisDao;
import ru.yandex.market.tpl.core.util.TplDbConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TplRegionDbStorageServiceTest {

    @Mock
    private RegionBorderHistoryRepository regionBorderHistoryRepository;
    @Mock
    private TplRegionBorderGisDao tplRegionBorderGisDao;
    @InjectMocks
    private TplRegionDbStorageService tplRegionDbStorageService;

    @Test
    void getVersion_whenExists() {
        //given
        Instant expectedDate = Instant.now();

        RegionBorderHistory borderHistory = new RegionBorderHistory();
        borderHistory.setActualizationDate(expectedDate);

        Mockito.doReturn(Optional.of(borderHistory)).when(regionBorderHistoryRepository).findFirstByOrderByActualizationDateDesc();

        //when
        TplRegionStorage.RegionStorageVersion version = tplRegionDbStorageService.getVersion();

        //then
        assertNotNull(version);
        assertThat(version.getActualizationDate()).isEqualTo(expectedDate);
    }

    @Test
    void getVersion_whenNotExists() {
        //given
        Mockito.doReturn(Optional.empty()).when(regionBorderHistoryRepository).findFirstByOrderByActualizationDateDesc();

        //when
        TplRegionStorage.RegionStorageVersion version = tplRegionDbStorageService.getVersion();

        //then
        assertNull(version);
    }

    @Test
    void forEachRegion_whenNotEmpty() {
        //given
        Set<Integer> regionFilter = Set.of(1);

        var expectedRegionMeta = RegionMeta.of(1, null);

        Mockito.doReturn(Collections.singletonList(expectedRegionMeta), Collections.emptyList())
                .when(tplRegionBorderGisDao)
                .findAllByRegions(eq(regionFilter), any());

        Consumer<RegionMeta> mockedConsumer = mock(Consumer.class);

        //when
        tplRegionDbStorageService.forEachRegion(TplRegionStorage.RegionStorageFilter.of(regionFilter), mockedConsumer);

        //then
        Mockito.verify(mockedConsumer, times(1)).accept(any());
    }

    @Test
    void forEachRegion_whenNotEmpty_SplitHugeQuery() {
        //given
        Set<Integer> hugeRegionFilter = Stream
                .iterate(1, n -> n +1)
                .limit(TplDbConstants.MAX_IDS_IN_ONE_DB_QUERY + 1)
                .collect(Collectors.toSet());

        var expectedRegionMeta = RegionMeta.of(1, null);

        ArgumentCaptor<Set<Integer>> requestRegionsCaptor =
                ArgumentCaptor.forClass(Set.class);

        Mockito.doReturn(Collections.singletonList(expectedRegionMeta), Collections.emptyList(),
                Collections.singletonList(expectedRegionMeta), Collections.emptyList())
                .when(tplRegionBorderGisDao)
                .findAllByRegions(requestRegionsCaptor.capture(), any());


        Consumer<RegionMeta> mockedConsumer = mock(Consumer.class);

        //when
        tplRegionDbStorageService.forEachRegion(TplRegionStorage.RegionStorageFilter.of(hugeRegionFilter), mockedConsumer);

        //then

        assertThat(hugeRegionFilter).isEqualTo(requestRegionsCaptor
                .getAllValues()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
        );
        Mockito.verify(mockedConsumer, times(2)).accept(any());
    }

    @Test
    void forEachRegion_whenEmpty() {
        //given
        Consumer<RegionMeta> mockedConsumer = mock(Consumer.class);

        //when
        tplRegionDbStorageService.forEachRegion(TplRegionStorage.RegionStorageFilter.of(Set.of()), mockedConsumer);

        //then
        Mockito.verify(mockedConsumer, never()).accept(any());
    }
}
