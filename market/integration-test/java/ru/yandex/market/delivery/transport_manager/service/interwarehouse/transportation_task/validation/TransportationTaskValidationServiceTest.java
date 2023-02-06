package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.validation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.ZeroQuantityValidationDto;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorTicketDto;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StEntityErrorTicketService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

public class TransportationTaskValidationServiceTest extends AbstractContextualTest {

    @Autowired
    private TransportationTaskValidationService validationService;

    @Autowired
    private StEntityErrorTicketService ticketCreationService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private RegisterMapper registerMapper;

    @BeforeEach
    void init() {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(1L).partnerId(1L).build(),
                LogisticsPointResponse.newBuilder().id(2L).partnerId(2L).build(),
                LogisticsPointResponse.newBuilder().id(6L).partnerId(6L).build()
            ));

        Mockito.when(lmsClient.searchPartnerApiSettingsMethods(Mockito.any())).thenReturn(List.of(
            SettingsMethodDto.newBuilder()
                .partnerId(1L).active(true).method("putOutbound").settingsApiId(1L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(1L).active(true).method("putOutboundRegistry").settingsApiId(2L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(1L).active(true).method("getOutbound").settingsApiId(3L).build(),

            SettingsMethodDto.newBuilder()
                .partnerId(2L).active(true).method("putOutbound").settingsApiId(4L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(2L).active(true).method("putOutboundRegistry").settingsApiId(5L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(2L).active(true).method("getOutbound").settingsApiId(6L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(2L).active(true).method("putInbound").settingsApiId(7L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(2L).active(true).method("putInboundRegistry").settingsApiId(8L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(2L).active(true).method("getInbound").settingsApiId(9L).build(),

            SettingsMethodDto.newBuilder()
                .partnerId(6L).active(true).method("putInbound").settingsApiId(10L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(6L).active(true).method("putInboundRegistry").settingsApiId(11L).build(),
            SettingsMethodDto.newBuilder()
                .partnerId(6L).active(true).method("getInbound").settingsApiId(12L).build()
            )
        );

        Mockito.when(lmsClient.searchPartnerApiSettings(Mockito.any())).thenReturn(List.of(
            SettingsApiDto.newBuilder().partnerId(1L).id(1L).apiType(ApiType.DELIVERY).build(),
            SettingsApiDto.newBuilder().partnerId(1L).id(2L).apiType(ApiType.DELIVERY).build(),
            SettingsApiDto.newBuilder().partnerId(1L).id(3L).apiType(ApiType.DELIVERY).build(),

            SettingsApiDto.newBuilder().partnerId(2L).id(4L).apiType(ApiType.DELIVERY).build(),
            SettingsApiDto.newBuilder().partnerId(2L).id(5L).apiType(ApiType.DELIVERY).build(),
            SettingsApiDto.newBuilder().partnerId(2L).id(6L).build(),
            SettingsApiDto.newBuilder().partnerId(2L).id(7L).build(),
            SettingsApiDto.newBuilder().partnerId(2L).id(8L).build(),
            SettingsApiDto.newBuilder().partnerId(2L).id(9L).apiType(ApiType.DELIVERY).build(),

            SettingsApiDto.newBuilder().partnerId(6L).id(10L).apiType(ApiType.DELIVERY).build(),
            SettingsApiDto.newBuilder().partnerId(6L).id(11L).apiType(ApiType.DELIVERY).build(),
            SettingsApiDto.newBuilder().partnerId(6L).id(12L).apiType(ApiType.DELIVERY).build()
            )
        );
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/additional_transportation_tasks.xml",
        "/repository/transportation_task/transport_metadata.xml",
        "/repository/register/single_register_single_fit.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_validation_valid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validateValid() {
        softly.assertThat(validationService.validate(1L))
            .isEqualTo(TransportationTaskStatus.VALID);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/additional_transportation_tasks.xml",
        "/repository/transportation_task/transport_metadata.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_validation_invalid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validateInvalid() {
        Mockito.when(registerMapper.hasZeroQuantities(3L))
            .thenReturn(new ZeroQuantityValidationDto(true, false, true));
//            .doReturn(new ZeroQuantityValidationDto(true, false, true))
//            .when(registerMapper).hasZeroQuantities(eq(3L));

        softly.assertThat(validationService.validate(3L))
            .isEqualTo(TransportationTaskStatus.INVALID);

        StartrekErrorTicketDto expected = new StartrekErrorTicketDto()
            .setErrorType(StartrekErrorType.VALIDATION_ERROR)
            .setTags(List.of())
            .setMessage("Ошибки валидации:\nРегистр не существует; ");

        Mockito.verify(ticketCreationService).createErrorTicket(EntityType.TRANSPORTATION_TASK, 3L, expected);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/additional_transportation_tasks.xml",
        "/repository/transportation_task/transport_metadata.xml"
    })
    void checkStatus() {
        softly.assertThatThrownBy(() -> validationService.validate(2L));
    }
}
