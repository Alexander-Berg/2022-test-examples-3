package ru.yandex.market.tpl.tms.executor.ds;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.usershift.location.DSRegionYtDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsRegionManager;
import ru.yandex.market.tpl.core.domain.usershift.location.DeliveryRegionInfoService;
import ru.yandex.market.tpl.core.service.delivery.YtDsService;
import ru.yandex.market.tpl.core.service.order.validator.OrderRevalidateTriggerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DsExecutorTest {

    @Mock
    private YtDsService ytDsService;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private DeliveryRegionInfoService deliveryRegionInfoService;
    @Mock
    private OrderRevalidateTriggerService orderRevalidateTriggerService;
    @Mock
    private DsRegionManager dsRegionManager;
    @Mock
    private Clock clock;
    @InjectMocks
    private DsExecutor dsExecutor;

    @AfterEach
    void after() {
        Mockito.reset(ytDsService);
        Mockito.reset(configurationProviderAdapter);
        Mockito.reset(deliveryRegionInfoService);
        Mockito.reset(orderRevalidateTriggerService);
        Mockito.reset(dsRegionManager);
        Mockito.reset(clock);
    }

    @Test
    void revalidating_when_Enabled() {
        //given
        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.IS_YT_TAKEN_ZONES_ENABLED);

        doReturn(List.of(DSRegionYtDto.builder().build())).when(ytDsService)
                .getDsRegions();

        doReturn(Set.of(1L, 2L, 3L)).when(dsRegionManager)
                .calculateDsDiff(any(), anyInt());

        doReturn(Set.of(1L, 2L, 5L)).when(configurationProviderAdapter)
                .getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS);

        doReturn(Instant.now())
                .when(clock).instant();
        //when
        dsExecutor.doRealJob(null);

        //then
        verify(orderRevalidateTriggerService, times(1))
                .triggerRevalidateOnChangeDs(eq(Set.of(1L, 2L)), any());
    }

    @Test
    void revalidating_when_DisabledDs() {
        //given
        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.IS_YT_TAKEN_ZONES_ENABLED);

        doReturn(List.of(DSRegionYtDto.builder().build())).when(ytDsService)
                .getDsRegions();

        doReturn(Set.of(1L, 2L, 3L)).when(dsRegionManager)
                .calculateDsDiff(any(), anyInt());

        doReturn(Set.of()).when(configurationProviderAdapter)
                .getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS);

        //when
        dsExecutor.doRealJob(null);

        //then
        verify(orderRevalidateTriggerService, never())
                .triggerRevalidateOnChangeDs(any(), any());
    }
}
