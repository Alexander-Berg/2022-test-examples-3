package ru.yandex.market.tpl.core.domain.ds;

import java.time.Clock;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class DsRegionManagerTest {

    @Mock
    private DeliveryServiceRegionRepository deliveryServiceRegionRepository;
    @Mock
    private Clock clock;
    @InjectMocks
    private DsRegionManager dsRegionManager;

    @Test
    void calculateDsDiff_whenDifferent() {
        //given
        int dataVersion = 0;
        Mockito.doReturn(
                Set.of(
                        createMock(1L,  3L),
                        createMock(2L,  123L),
                        createMock(3L,  222L)
                )
        ).when(deliveryServiceRegionRepository).findAllDsRegionCounter(eq(dataVersion));

        //when
        Set<Long> diffIds = dsRegionManager.calculateDsDiff(Set.of(
                createDsRegion(1L, 1),
                createDsRegion(1L, 2),
                createDsRegion(1L, 3)), dataVersion);

        //then
        Assertions.assertThat(diffIds).containsExactly(2L, 3L);
    }

    @Test
    void calculateDsDiff_whenSame() {
        //given
        int dataVersion = 0;
        Mockito.doReturn(
                Set.of(
                        createMock(1L,  3L),
                        createMock(2L,  1L),
                        createMock(3L,  1L)
                )
        ).when(deliveryServiceRegionRepository).findAllDsRegionCounter(eq(dataVersion));

        //when
        Set<Long> diffIds = dsRegionManager.calculateDsDiff(Set.of(
                createDsRegion(1L, 1),
                createDsRegion(1L, 2),
                createDsRegion(1L, 3),
                createDsRegion(2L, 2),
                createDsRegion(3L, 3)), dataVersion);

        //then
        Assertions.assertThat(diffIds).isEmpty();
    }

    @Test
    void calculateDsDiff_whenOneEmpty() {
        //given
        int dataVersion = 0;
        Mockito.doReturn(
                Set.of(
                        createMock(1L,  3L),
                        createMock(2L,  1L),
                        createMock(3L,  1L)
                )
        ).when(deliveryServiceRegionRepository).findAllDsRegionCounter(eq(dataVersion));

        //when
        Set<Long> diffIds = dsRegionManager.calculateDsDiff(Set.of(), dataVersion);

        //then
        Assertions.assertThat(diffIds).containsExactly(1L, 2L, 3L);
    }

    private DeliveryServiceRegionRepository.DsRegionCounter createMock(Long dsId,
                                                                       Long total) {
        var mockedCounter = Mockito.mock(DeliveryServiceRegionRepository.DsRegionCounter.class);

        Mockito.when(mockedCounter.getDsId()).thenReturn(dsId);
        Mockito.when(mockedCounter.getTotal()).thenReturn(total);

        return mockedCounter;
    }

    private DsRegion createDsRegion(Long dsId, int regionId) {
        DsRegion dsRegion = new DsRegion();
        dsRegion.setDeliveryServiceId(dsId);
        dsRegion.setRegionId(regionId);
        return dsRegion;
    }
}
