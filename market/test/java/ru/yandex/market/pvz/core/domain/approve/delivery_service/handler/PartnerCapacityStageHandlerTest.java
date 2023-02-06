package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.BERU_PLATFORM_ID;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.CAPACITY_SERVICE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.CAPACITY_TYPE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.CAPACITY_VALUE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.COUNTING_TYPE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.DELIVERY_TYPE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.LOCATION_FROM;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerCapacityStageHandler.LOCATION_TO;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerCapacityStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    @Captor
    private ArgumentCaptor<PartnerCapacityDto> capacityCaptor;

    private final PartnerCapacityStageHandler partnerCapacityStageHandler;

    @Test
    void setupPartnerCapacity() {
        when(lmsClient.getPartnerCapacities(DELIVERY_SERVICE_ID)).thenReturn(List.of());
        partnerCapacityStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));
        verify(lmsClient, times(1))
                .createCapacity(capacityCaptor.capture());

        var expected = buildCapacityRequest(LOCATION_FROM, LOCATION_TO);

        assertThat(equalsCapacityRequest(capacityCaptor.getValue(), expected)).isTrue();
    }

    @Test
    void setupPartnerCapacityWhenDifferentRegionFromExists() {
        when(lmsClient.getPartnerCapacities(DELIVERY_SERVICE_ID)).thenReturn(List.of(
                buildCapacityRequest(LOCATION_FROM + 1, LOCATION_TO)
        ));
        partnerCapacityStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));
        verify(lmsClient, times(1))
                .createCapacity(capacityCaptor.capture());

        var expected = buildCapacityRequest(LOCATION_FROM, LOCATION_TO);

        assertThat(equalsCapacityRequest(capacityCaptor.getValue(), expected)).isTrue();
    }

    @Test
    void setupPartnerCapacityWhenDifferentRegionToExists() {
        when(lmsClient.getPartnerCapacities(DELIVERY_SERVICE_ID)).thenReturn(List.of(
                buildCapacityRequest(LOCATION_FROM, LOCATION_TO + 1)
        ));
        partnerCapacityStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));
        verify(lmsClient, times(1))
                .createCapacity(capacityCaptor.capture());

        var expected = buildCapacityRequest(LOCATION_FROM, LOCATION_TO);

        assertThat(equalsCapacityRequest(capacityCaptor.getValue(), expected)).isTrue();
    }

    @Test
    void setupPartnerCapacityWhenOneCapacityAlreadyExists() {
        when(lmsClient.getPartnerCapacities(DELIVERY_SERVICE_ID)).thenReturn(List.of(
                buildCapacityRequest(LOCATION_FROM, LOCATION_TO)
        ));
        partnerCapacityStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));
        verify(lmsClient, never()).createCapacity(any());
    }

    @Test
    void lmsCInternalErrorOnGet() {
        when(lmsClient.getPartnerCapacities(DELIVERY_SERVICE_ID)).thenThrow(new HttpTemplateException(500, ""));
        assertThatThrownBy(() ->
                partnerCapacityStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);
        verify(lmsClient, never()).createCapacity(any());
    }

    @Test
    void lmsInternalErrorOnCreate() {
        when(lmsClient.getPartnerCapacities(DELIVERY_SERVICE_ID)).thenReturn(List.of());
        when(lmsClient.createCapacity(any())).thenThrow(new HttpTemplateException(500, ""));
        assertThatThrownBy(() ->
                partnerCapacityStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);
        verify(lmsClient, times(1))
                .createCapacity(capacityCaptor.capture());

        var expected = buildCapacityRequest(LOCATION_FROM, LOCATION_TO);

        assertThat(equalsCapacityRequest(capacityCaptor.getValue(), expected)).isTrue();
    }

    private PartnerCapacityDto buildCapacityRequest(int locationFrom, int locationTo) {
        return PartnerCapacityDto.newBuilder()
                .partnerId(DELIVERY_SERVICE_ID)
                .locationFrom(locationFrom)
                .locationTo(locationTo)
                .deliveryType(DELIVERY_TYPE)
                .type(CAPACITY_TYPE)
                .countingType(COUNTING_TYPE)
                .capacityService(CAPACITY_SERVICE)
                .platformClientId(BERU_PLATFORM_ID)
                .value(CAPACITY_VALUE)
                .build();
    }

    private boolean equalsCapacityRequest(PartnerCapacityDto first, PartnerCapacityDto second) {
        return
                first.getPartnerId().equals(second.getPartnerId()) &&
                        first.getLocationFrom().equals(second.getLocationFrom()) &&
                        first.getLocationTo().equals(second.getLocationTo()) &&
                        first.getDeliveryType().equals(second.getDeliveryType()) &&
                        first.getType().equals(second.getType()) &&
                        first.getCountingType().equals(second.getCountingType()) &&
                        first.getCapacityService().equals(second.getCapacityService()) &&
                        first.getPlatformClientId().equals(second.getPlatformClientId()) &&
                        first.getValue().equals(second.getValue());
    }
}
