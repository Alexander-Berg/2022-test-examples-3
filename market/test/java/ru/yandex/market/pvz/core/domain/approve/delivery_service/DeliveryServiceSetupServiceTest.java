package ru.yandex.market.pvz.core.domain.approve.delivery_service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.LmsParams;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PlatformClientPartnerStageHandler;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryServiceRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PARTNER_CAPACITY;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage.PLATFORM_CLIENT_PARTNER;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryServiceSetupServiceTest {

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;

    private final DeliveryServiceRepository deliveryServiceRepository;

    private final DeliveryServiceSetupService deliveryServiceSetupService;

    @MockBean
    private PlatformClientPartnerStageHandler platformClientPartnerStageHandler;

    @MockBean
    private LMSClient lmsClient;

    @BeforeEach
    void setupMock() {
        when(platformClientPartnerStageHandler.getSetupStage()).thenReturn(PLATFORM_CLIENT_PARTNER);
        deliveryServiceSetupService.handlerMap.remove(null);
        deliveryServiceSetupService.handlerMap.put(PLATFORM_CLIENT_PARTNER, platformClientPartnerStageHandler);
    }

    @Test
    void fullSetup() {
        DeliveryService deliveryService = deliveryServiceFactory.createNotSetupDeliveryService();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService)
                        .build()
        );

        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(deliveryService.getId())
                .marketId(deliveryService.getId() + 1000)
                .build();
        when(lmsClient.getPartner(deliveryService.getId())).thenReturn(Optional.of(partnerResponse));
        when(lmsClient.createApiSettings(eq(deliveryService.getId()), any()))
                .thenReturn(SettingsApiDto.newBuilder().id(1L).build());

        deliveryServiceSetupService.setup(deliveryService.getId());
        verify(platformClientPartnerStageHandler, times(1)).handle(LmsParams.builder()
                .deliveryServiceId(deliveryService.getId())
                .token(deliveryService.getToken())
                .build());

        DeliveryService setup = deliveryServiceRepository.findByIdOrThrow(deliveryService.getId());

        assertThat(setup.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);
    }

    @Test
    void setupFromIntermediateStage() {
        DeliveryService deliveryService = deliveryServiceFactory.createNotSetupDeliveryService();
        deliveryService = deliveryServiceFactory.updateSetupStage(deliveryService.getId(), PARTNER_CAPACITY);

        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(deliveryService.getId())
                .marketId(deliveryService.getId() + 1000)
                .build();
        when(lmsClient.getPartner(deliveryService.getId())).thenReturn(Optional.of(partnerResponse));

        deliveryServiceSetupService.setup(deliveryService.getId());
        verify(platformClientPartnerStageHandler, never()).handle(LmsParams.builder()
                .deliveryServiceId(deliveryService.getId())
                .token(deliveryService.getToken())
                .build());

        DeliveryService setup = deliveryServiceRepository.findByIdOrThrow(deliveryService.getId());

        assertThat(setup.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);
    }

    @Test
    void failedOnPlatformClientPartnerStageHandler() {
        DeliveryService deliveryService = deliveryServiceFactory.createNotSetupDeliveryService();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService)
                        .build()
        );

        when(lmsClient.createApiSettings(eq(deliveryService.getId()), any()))
                .thenReturn(SettingsApiDto.newBuilder().id(1L).build());

        doThrow(RuntimeException.class).when(platformClientPartnerStageHandler).handle(LmsParams.builder()
                .deliveryServiceId(deliveryService.getId())
                .token(deliveryService.getToken())
                .build());
        assertThatThrownBy(() -> deliveryServiceSetupService.setup(deliveryService.getId()))
                .isExactlyInstanceOf(RuntimeException.class);

        DeliveryService setup = deliveryServiceRepository.findByIdOrThrow(deliveryService.getId());

        assertThat(setup.getSetupStage()).isEqualTo(SetupStage.PARTNER_EXTERNAL_PARAM);
    }

    @Test
    void tryToSetupActivatedDs() {
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();

        deliveryServiceSetupService.setup(deliveryService.getId());
        verify(platformClientPartnerStageHandler, never()).handle(LmsParams.builder()
                .deliveryServiceId(deliveryService.getId())
                .token(deliveryService.getToken())
                .build());

        DeliveryService setup = deliveryServiceRepository.findByIdOrThrow(deliveryService.getId());

        assertThat(setup.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);
    }

    @Test
    void setupBatch() {
        DeliveryService partnerCreateDs = deliveryServiceFactory.createNotSetupDeliveryService();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(partnerCreateDs)
                        .build()
        );

        PartnerResponse partnerResponseForCreateDs = PartnerResponse.newBuilder()
                .id(partnerCreateDs.getId())
                .marketId(partnerCreateDs.getId() + 1000)
                .build();
        when(lmsClient.getPartner(partnerCreateDs.getId())).thenReturn(Optional.of(partnerResponseForCreateDs));
        when(lmsClient.createApiSettings(eq(partnerCreateDs.getId()), any()))
                .thenReturn(SettingsApiDto.newBuilder().id(1L).build());
        DeliveryService apiSettingsDs = deliveryServiceFactory.createNotSetupDeliveryService();
        apiSettingsDs = deliveryServiceFactory.updateSetupStage(apiSettingsDs.getId(), SetupStage.API_SETTINGS);
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(apiSettingsDs)
                        .build()
        );

        PartnerResponse partnerResponseForApiSettings = PartnerResponse.newBuilder()
                .id(apiSettingsDs.getId())
                .marketId(apiSettingsDs.getId() + 1000)
                .build();
        when(lmsClient.getPartner(apiSettingsDs.getId())).thenReturn(Optional.of(partnerResponseForApiSettings));
        when(lmsClient.createApiSettings(eq(apiSettingsDs.getId()), any()))
                .thenReturn(SettingsApiDto.newBuilder().id(2L).build());

        DeliveryService mdbDs = deliveryServiceFactory.createNotSetupDeliveryService();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(mdbDs)
                        .build()
        );
        mdbDs = deliveryServiceFactory.updateSetupStage(mdbDs.getId(), SetupStage.MDB);
        PartnerResponse partnerResponseForMdb = PartnerResponse.newBuilder()
                .id(mdbDs.getId())
                .marketId(mdbDs.getId() + 1000)
                .build();
        when(lmsClient.getPartner(mdbDs.getId())).thenReturn(Optional.of(partnerResponseForMdb));
        when(lmsClient.createApiSettings(eq(mdbDs.getId()), any()))
                .thenReturn(SettingsApiDto.newBuilder().id(3L).build());

        DeliveryService activatedDs = deliveryServiceFactory.createDeliveryService();
        legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(activatedDs)
                        .build()
        );
        PartnerResponse partnerResponseForActivateDs = PartnerResponse.newBuilder()
                .id(activatedDs.getId())
                .marketId(activatedDs.getId() + 1000)
                .build();
        when(lmsClient.getPartner(activatedDs.getId())).thenReturn(Optional.of(partnerResponseForActivateDs));
        when(lmsClient.createApiSettings(eq(activatedDs.getId()), any()))
                .thenReturn(SettingsApiDto.newBuilder().id(4L).build());

        deliveryServiceSetupService.setupAll();

        verify(platformClientPartnerStageHandler, times(1)).handle(LmsParams.builder()
                .deliveryServiceId(partnerCreateDs.getId())
                .token(partnerCreateDs.getToken())
                .build());
        verify(platformClientPartnerStageHandler, times(1)).handle(LmsParams.builder()
                .deliveryServiceId(apiSettingsDs.getId())
                .token(apiSettingsDs.getToken())
                .build());
        verify(platformClientPartnerStageHandler, never()).handle(LmsParams.builder()
                .deliveryServiceId(mdbDs.getId())
                .token(mdbDs.getToken())
                .build());
        verify(platformClientPartnerStageHandler, never()).handle(LmsParams.builder()
                .deliveryServiceId(activatedDs.getId())
                .token(activatedDs.getToken())
                .build());

        partnerCreateDs = deliveryServiceRepository.findByIdOrThrow(partnerCreateDs.getId());
        assertThat(partnerCreateDs.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);

        apiSettingsDs = deliveryServiceRepository.findByIdOrThrow(apiSettingsDs.getId());
        assertThat(apiSettingsDs.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);

        mdbDs = deliveryServiceRepository.findByIdOrThrow(mdbDs.getId());
        assertThat(mdbDs.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);

        activatedDs = deliveryServiceRepository.findByIdOrThrow(activatedDs.getId());
        assertThat(activatedDs.getSetupStage()).isEqualTo(SetupStage.PARTNER_ACTIVATE);
    }
}
