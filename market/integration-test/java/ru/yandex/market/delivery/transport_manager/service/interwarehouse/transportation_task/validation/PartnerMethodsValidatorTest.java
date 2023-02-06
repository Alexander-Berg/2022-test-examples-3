package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.validation;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTask;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationTaskMapper;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

@DatabaseSetup("/repository/transportation_task/task_with_transportation.xml")
public class PartnerMethodsValidatorTest extends AbstractContextualTest {
    @Autowired
    private PartnerMethodsValidator methodsValidator;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TransportationTaskMapper transportationTaskMapper;

    @BeforeEach
    void init() {
        Mockito.when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(1L, 2L)).build()))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(1L).partnerId(1L).build(),
                LogisticsPointResponse.newBuilder().id(2L).partnerId(2L).build()
            ));
    }

    @Test
    void testValid() {
        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(
            SettingsMethodFilter.newBuilder().partnerIds(Set.of(1L, 2L)).methodTypes(Mockito.any()).build()
        )).thenReturn(List.of(
            SettingsMethodDto.newBuilder()
                    .partnerId(1L).active(true).method("putOutbound").settingsApiId(1L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(1L).active(true).method("putOutboundRegistry").settingsApiId(2L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(1L).active(true).method("getOutbound").settingsApiId(3L).build(),

            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInbound").settingsApiId(4L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInboundRegistry").settingsApiId(5L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("getInbound").settingsApiId(6L).build()
            )
        );

        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
            .thenReturn(List.of(
                SettingsApiDto.newBuilder().partnerId(1L).id(1L).apiType(ApiType.DELIVERY).build(),
                SettingsApiDto.newBuilder().partnerId(1L).id(2L).apiType(ApiType.FULFILLMENT).build(),
                SettingsApiDto.newBuilder().partnerId(1L).id(3L).apiType(ApiType.DELIVERY).build(),

                SettingsApiDto.newBuilder().partnerId(2L).id(4L).apiType(ApiType.DELIVERY).build(),
                SettingsApiDto.newBuilder().partnerId(2L).id(5L).apiType(ApiType.FULFILLMENT).build(),
                SettingsApiDto.newBuilder().partnerId(2L).id(6L).apiType(ApiType.DELIVERY).build()
            )
        );

        TransportationTask task = transportationTaskMapper.getById(1);
        ValidationResult result = methodsValidator.isValid(task);
        ValidationResult expected = new ValidationResult().setInvalid(false).setErrorMessage("");

        assertThatModelEquals(expected, result);
    }

    @Test
    void testOneInvalid() {
        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(
            SettingsMethodFilter.newBuilder().partnerIds(Set.of(1L, 2L)).methodTypes(Mockito.any()).build()
        )).thenReturn(List.of(
            SettingsMethodDto.newBuilder()
                    .partnerId(1L).active(true).method("getOutbound").settingsApiId(1L).build(),

            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInbound").settingsApiId(2L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInboundRegistry").settingsApiId(3L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("getInbound").settingsApiId(4L).build()
            )
        );

        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
            .thenReturn(List.of(
                SettingsApiDto.newBuilder().partnerId(1L).id(1L).apiType(ApiType.DELIVERY).build(),

                SettingsApiDto.newBuilder().partnerId(2L).id(2L).apiType(ApiType.DELIVERY).build(),
                SettingsApiDto.newBuilder().partnerId(2L).id(3L).apiType(ApiType.FULFILLMENT).build(),
                SettingsApiDto.newBuilder().partnerId(2L).id(4L).apiType(ApiType.DELIVERY).build()
            )
        );

        TransportationTask task = transportationTaskMapper.getById(1);
        ValidationResult result = methodsValidator.isValid(task);
        ValidationResult expected = new ValidationResult()
            .setInvalid(true)
            .setErrorMessage(
                "Партнёр 1 не поддерживает следующие необходимые методы: [putOutbound, putOutboundRegistry]"
            );

        assertThatModelEquals(expected, result);
    }

    @Test
    void testBothInvalid() {
        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(
            SettingsMethodFilter.newBuilder().partnerIds(Set.of(1L, 2L)).methodTypes(Mockito.any()).build()
        )).thenReturn(List.of(
            SettingsMethodDto.newBuilder()
                    .partnerId(1L).active(true).method("getOutbound").settingsApiId(1L).build(),

            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInbound").settingsApiId(2L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInboundRegistry").settingsApiId(3L).build()
            )
        );

        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
        .thenReturn(List.of(
                SettingsApiDto.newBuilder().partnerId(1L).id(1L).apiType(ApiType.DELIVERY).build(),

                SettingsApiDto.newBuilder().partnerId(2L).id(2L).apiType(ApiType.FULFILLMENT).build(),
                SettingsApiDto.newBuilder().partnerId(2L).id(3L).apiType(ApiType.DELIVERY).build()
            )
        );

        TransportationTask task = transportationTaskMapper.getById(1);
        ValidationResult result = methodsValidator.isValid(task);
        ValidationResult expected = new ValidationResult()
            .setInvalid(true)
            .setErrorMessage(
                "Партнёр 1 не поддерживает следующие необходимые методы: [putOutbound, putOutboundRegistry], " +
                    "Партнёр 2 не поддерживает следующие необходимые методы: [getInbound]"
            );

        assertThatModelEquals(expected, result);
    }

    @Test
    void testOneWithoutMethods() {
        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(
            SettingsMethodFilter.newBuilder().partnerIds(Set.of(1L, 2L)).methodTypes(Mockito.any()).build()
        )).thenReturn(List.of(
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInbound").settingsApiId(1L).build(),
            SettingsMethodDto.newBuilder()
                    .partnerId(2L).active(true).method("putInboundRegistry").settingsApiId(2L).build()
            )
        );

        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any()))
            .thenReturn(List.of(
                SettingsApiDto.newBuilder().partnerId(2L).id(1L).apiType(ApiType.DELIVERY).build(),
                SettingsApiDto.newBuilder().partnerId(2L).id(2L).apiType(ApiType.FULFILLMENT).build()
            )
        );

        TransportationTask task = transportationTaskMapper.getById(1);
        ValidationResult result = methodsValidator.isValid(task);
        ValidationResult expected = new ValidationResult()
            .setInvalid(true)
            .setErrorMessage(
                "Партнёр 1 не поддерживает следующие необходимые методы: " +
                    "[getOutbound, putOutbound, putOutboundRegistry], " +
                    "Партнёр 2 не поддерживает следующие необходимые методы: [getInbound]"
            );

        assertThatModelEquals(expected, result);
    }
}
