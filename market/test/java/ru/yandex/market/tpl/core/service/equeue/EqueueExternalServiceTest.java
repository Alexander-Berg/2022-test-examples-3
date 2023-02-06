package ru.yandex.market.tpl.core.service.equeue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.yard.client.YardClientApi;
import ru.yandex.market.logistics.yard.client.dto.service.ServiceParamListDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.service.equeue.mapper.TplYardEqueuePropertiesMapper;
import ru.yandex.market.tpl.core.service.equeue.model.EqueuePropertiesStateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EqueueExternalServiceTest {
    @Mock
    private ConfigurationProviderAdapter providerAdapter;
    @Mock
    private YardClientApi yardClientApi;
    @Mock
    private EqueuePropertiesService equeuePropertiesService;
    @Mock
    private TplYardEqueuePropertiesMapper propertiesMapper;
    @InjectMocks
    private EqueueExternalService equeueExternalService;

    @Test
    void whenDisabled() {
        //given
        EqueuePropertiesStateDto stateDto = EqueuePropertiesStateDto.builder().build();
        Mockito.when(providerAdapter.isBooleanEnabled(ConfigurationProperties
                .ELECTRONIC_QUEUE_PROPERTIES_PUSH_TO_YARD_ENABLED)).thenReturn(false);

        //when
        equeueExternalService.pushStateToRemoteSystem(stateDto);

        //then
        Mockito.verify(equeuePropertiesService, never()).getState(anyLong());
        Mockito.verify(yardClientApi, never()).updateServicesParams(any());

    }
    @Test
    void whenEnabled() {
        Mockito.when(providerAdapter.isBooleanEnabled(ConfigurationProperties
                .ELECTRONIC_QUEUE_PROPERTIES_PUSH_TO_YARD_ENABLED)).thenReturn(true);

        EqueuePropertiesStateDto stateDto = EqueuePropertiesStateDto.builder().build();

        ServiceParamListDto params = new ServiceParamListDto(List.of());
        Mockito.when(propertiesMapper.map(stateDto)).thenReturn(params);

        //when
        equeueExternalService.pushStateToRemoteSystem(stateDto);

        //then
        Mockito.verify(yardClientApi).updateServicesParams(eq(params));
    }
}
