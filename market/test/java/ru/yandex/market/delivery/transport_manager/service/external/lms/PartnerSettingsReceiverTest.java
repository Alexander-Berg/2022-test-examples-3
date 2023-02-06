package ru.yandex.market.delivery.transport_manager.service.external.lms;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

public class PartnerSettingsReceiverTest {
    private PartnerSettingsReceiver partnerSettingsReceiver;

    @BeforeEach
    public void init() {
        LMSClient lmsClient = Mockito.mock(LMSClient.class);
        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(Mockito.any()))
            .thenReturn(settingsMethods());
        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
            .thenReturn(settingsApiDtos());

        partnerSettingsReceiver = new PartnerSettingsReceiver(lmsClient);
    }

    @Test
    void testPartnerSettingReceiving() {
        Set<TransportationPartnerMethod> enablesMethods = partnerSettingsReceiver.getEnabledMethods(
            1L,
            Set.of(1L, 2L, 4L)
        );

        Assertions.assertThat(enablesMethods)
            .containsExactlyInAnyOrder(enabledMethods());
    }

    @Test
    void testPartnerSettingReceivingWithSameMethodName() {
        Set<TransportationPartnerMethod> enablesMethods = partnerSettingsReceiver.getEnabledMethods(1L, Set.of(3L));

        Assertions.assertThat(enablesMethods)
            .containsExactlyInAnyOrder(enabledMethodsWithSameName());
    }

    private static TransportationPartnerMethod[] enabledMethods() {
        return new TransportationPartnerMethod[]{
            transportationPartnerMethod(
                1L,
                1L,
                PartnerMethod.CREATE_SELF_EXPORT,
                ru.yandex.market.delivery.transport_manager.model.enums.ApiType.DELIVERY
            ),
            transportationPartnerMethod(
                1L,
                1L,
                PartnerMethod.PUT_MOVEMENT,
                ru.yandex.market.delivery.transport_manager.model.enums.ApiType.FULFILLMENT
            ),
            transportationPartnerMethod(
                1L,
                2L,
                PartnerMethod.PUT_MOVEMENT,
                ru.yandex.market.delivery.transport_manager.model.enums.ApiType.DELIVERY
            ),
            transportationPartnerMethod(
                1L,
                2L,
                PartnerMethod.GET_MOVEMENT_STATUS_HISTORY,
                null
            )
        };
    }

    private static TransportationPartnerMethod[] enabledMethodsWithSameName() {
        return new TransportationPartnerMethod[]{
            transportationPartnerMethod(
                1L,
                3L,
                PartnerMethod.PUT_MOVEMENT,
                ru.yandex.market.delivery.transport_manager.model.enums.ApiType.DELIVERY
            ),
            transportationPartnerMethod(
                1L,
                3L,
                PartnerMethod.PUT_MOVEMENT,
                ru.yandex.market.delivery.transport_manager.model.enums.ApiType.FULFILLMENT
            )
        };
    }

    private static TransportationPartnerMethod transportationPartnerMethod(
        Long transportationId,
        Long partnerId,
        PartnerMethod method,
        ru.yandex.market.delivery.transport_manager.model.enums.ApiType apiType
    ) {
        return new TransportationPartnerMethod()
            .setTransportationId(transportationId)
            .setPartnerId(partnerId)
            .setMethod(method)
            .setApiType(apiType);
    }

    private static List<SettingsMethodDto> settingsMethods() {
        return List.of(
            settingsMethodDto(1L, 1L, "someFakeMethod", true),
            settingsMethodDto(2L, 1L, "putMovement", true),
            settingsMethodDto(3L, 1L, "createTransportationRequest", false),
            settingsMethodDto(4L, 1L, "createSelfExport", true),
            settingsMethodDto(5L, 2L, "createSelfExport", false),
            settingsMethodDto(6L, 2L, "getMovementStatusHistory", true),
            settingsMethodDto(7L, 2L, "putMovement", true),
            settingsMethodDto(8L, 2L, "someFakeMethod", true),
            settingsMethodDto(9L, 3L, "putMovement", true),
            settingsMethodDto(10L, 3L, "putMovement", true)
        );
    }

    private static List<SettingsApiDto> settingsApiDtos() {
        return List.of(
            settingsApiDto(1L, 1L, ApiType.DELIVERY),
            settingsApiDto(2L, 1L, ApiType.FULFILLMENT),
            settingsApiDto(3L, 1L, ApiType.DELIVERY),
            settingsApiDto(4L, 1L, ApiType.DELIVERY),
            settingsApiDto(5L, 2L, ApiType.FULFILLMENT),
            settingsApiDto(6L, 2L, null),
            settingsApiDto(7L, 2L, ApiType.DELIVERY),
            settingsApiDto(8L, 2L, ApiType.FULFILLMENT),
            settingsApiDto(9L, 3L, ApiType.FULFILLMENT),
            settingsApiDto(10L, 3L, ApiType.DELIVERY)
        );
    }

    private static SettingsMethodDto settingsMethodDto(
        Long settingsApiId,
        Long partnerId,
        String method,
        boolean active
    ) {
        return SettingsMethodDto.newBuilder()
            .settingsApiId(settingsApiId)
            .partnerId(partnerId)
            .method(method)
            .active(active)
            .build();
    }

    private static SettingsApiDto settingsApiDto(
        Long id,
        Long partnerId,
        ApiType apiType
    ) {
        return SettingsApiDto.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .apiType(apiType)
            .build();
    }
}
