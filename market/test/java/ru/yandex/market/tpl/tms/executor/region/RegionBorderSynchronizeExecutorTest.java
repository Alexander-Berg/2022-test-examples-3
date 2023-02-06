package ru.yandex.market.tpl.tms.executor.region;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.service.region.RegionBoarderSynchronizerService;

@ExtendWith(MockitoExtension.class)
class RegionBorderSynchronizeExecutorTest {

    @Mock
    private RegionBoarderSynchronizerService regionBoarderSynchronizer;

    @Mock
    private ConfigurationProviderAdapter configurationProvider;

    @InjectMocks
    private RegionBorderSynchronizeExecutor regionBorderSynchronizeExecutor;

    @Test
    @SneakyThrows
    void notUpdateBorders_whenNotNeeds() {
        //given
        Mockito.doReturn(false).when(regionBoarderSynchronizer).isNeedToUpdate();
        Mockito.doReturn(true).when(configurationProvider)
                .isBooleanEnabled(ConfigurationProperties.REGION_BORDER_SYNCHRONIZATION_ENABLED);
        //when
        regionBorderSynchronizeExecutor.doRealJob(null);

        //then
        Mockito.verify(regionBoarderSynchronizer, Mockito.never()).fillTempTableWithPolygons();
    }

    @Test
    @SneakyThrows
    void updateBorders_whenNeeds() {
        //given
        Mockito.doReturn(true).when(regionBoarderSynchronizer).isNeedToUpdate();
        Mockito.doReturn(true).when(configurationProvider)
                .isBooleanEnabled(ConfigurationProperties.REGION_BORDER_SYNCHRONIZATION_ENABLED);
        //when
        regionBorderSynchronizeExecutor.doRealJob(null);

        //then
        Mockito.verify(regionBoarderSynchronizer, Mockito.times(1)).fillTempTableWithPolygons();
    }

    @Test
    @SneakyThrows
    void updateBorders_whenDisabled() {
        //given
        Mockito.doReturn(false).when(configurationProvider)
                .isBooleanEnabled(ConfigurationProperties.REGION_BORDER_SYNCHRONIZATION_ENABLED);
        //when
        regionBorderSynchronizeExecutor.doRealJob(null);

        //then
        Mockito.verify(regionBoarderSynchronizer, Mockito.never()).fillTempTableWithPolygons();
    }
}
