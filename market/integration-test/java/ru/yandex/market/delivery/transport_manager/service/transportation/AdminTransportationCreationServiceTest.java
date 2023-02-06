package ru.yandex.market.delivery.transport_manager.service.transportation;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.NewTransportationDto;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderBindingType;
import ru.yandex.market.delivery.transport_manager.service.order.OrderBindingService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerTransportFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

class AdminTransportationCreationServiceTest extends AbstractContextualTest {

    @Autowired
    private AdminTransportationCreationService creationService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private OrderBindingService bindingService;

    @BeforeEach
    void init() {
        LogisticsPointResponse outbound = LogisticsPointResponse.newBuilder().id(1L).partnerId(1L).build();
        LogisticsPointResponse inbound = LogisticsPointResponse.newBuilder().id(2L).partnerId(2L).build();

        Mockito.when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(1L, 2L)).build()))
            .thenReturn(List.of(outbound, inbound));

        clock.setFixed(Instant.parse("2021-07-21T16:59:00.00Z"), ZoneOffset.UTC);

        Mockito.when(lmsClient.getPartnerTransport(PartnerTransportFilter.builder().ids(Set.of(1L)).build()))
            .thenReturn(List.of(
                PartnerTransportDto.newBuilder()
                    .id(1L)
                    .price(100L)
                    .duration(Duration.ofMinutes(40))
                    .logisticsPointFrom(outbound)
                    .logisticsPointTo(inbound)
                    .palletCount(5)
                    .partner(PartnerResponse.newBuilder().id(5L).build())
                    .build()
            ));

        Mockito.when(lmsClient.getPartnerTransport(PartnerTransportFilter.builder().ids(Set.of(2L)).build()))
            .thenReturn(List.of(
                PartnerTransportDto.newBuilder()
                    .id(2L)
                    .price(1000L)
                    .duration(Duration.ofMinutes(40))
                    .logisticsPointFrom(outbound)
                    .logisticsPointTo(LogisticsPointResponse.newBuilder().id(12L).partnerId(20L).build())
                    .palletCount(5)
                    .partner(PartnerResponse.newBuilder().id(5L).build())
                    .build()
            ));
    }

    @Test
    void testBaseValidations() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(2L)
                .setInboundLogisticsPointId(1L)
                .setInboundPartnerId(3L)
                .setAsap(true);


        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Отправитель и получатель (и их лог. точки) должны быть разными партнёрами");

        dto.setInboundLogisticsPointId(2L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Указанный отправитель id=2 не совпадает с партнёром лог. точки отправления id=1");

        dto.setOutboundPartnerId(1L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Указанный получатель id=3 не совпадает с партнёром лог. точки получения id=2");

        dto.setInboundPartnerId(2L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Флаг \"Как можно быстрее\" только для магистрального XDock");

        dto.setAsap(false);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Должен быть задан хотя бы один временной интервал");

        dto.setOutboundTimeFrom(LocalTime.of(13, 0, 0));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("В интервале должны быть указаны начало и конец");

        dto
            .setOutboundTimeTo(LocalTime.of(14, 0, 0))
            .setInboundTimeFrom(LocalTime.of(15, 0, 0))
            .setInboundTimeTo(LocalTime.of(16, 0, 0));


        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Не указана дата перемещения");

        dto.setDay(LocalDate.of(2021, 7, 21));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Начало перемещения должно быть минимум через час от текущего времени");

        dto.setDay(LocalDate.of(2021, 8, 5))
            .setMovingPartnerId(5L);

        creationService.validateTransportation(dto);

        dto.setOutboundTimeFrom(null);
        dto.setOutboundTimeTo(null);
        dto.setInboundTimeFrom(null);
        dto.setInboundTimeTo(null);
        dto.setMovingPartnerId(null);
        dto.setAsap(true);
        dto.setPallets(33);
        dto.setTransportationType(AdminTransportationType.XDOC_TRANSPORT);
        creationService.validateTransportation(dto);
    }

    @Test
    void testOrdersOperationValidations() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setDay(LocalDate.of(2021, 8, 5))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("У перемещения с типом Заборка должен быть указан перемещающий партнёр");

        dto.setMovingPartnerId(2L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage(
                "Для перемещения с типом Заборка со способом отгрузки забор должен быть указан интервал отгрузки"
            );

        dto.setOutboundTimeFrom(LocalTime.of(12, 0, 0))
            .setOutboundTimeTo(LocalTime.of(14, 0, 0))
            .setInboundTimeFrom(null)
            .setInboundTimeTo(null)
            .setMovingPartnerId(1L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage(
                "Для перемещения с типом Заборка со способом отгрузки самопривоз или третий перемещающий партнёр " +
                    "должен быть указан интервал приёмки"
            );

        dto.setMovingPartnerId(2L)
            .setInboundTimeFrom(LocalTime.of(13, 0, 0))
            .setInboundTimeTo(LocalTime.of(16, 0, 0));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Интервал приёмки должен быть позже чем интервал отгрузки");

        dto.setInboundTimeFrom(LocalTime.of(15, 0, 0));

        creationService.validateTransportation(dto);
    }

    @Test
    void testXDocValidations() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.XDOC_TRANSPORT)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setMovingPartnerId(5L)
                .setDay(LocalDate.of(2021, 8, 5))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto)).hasMessage(
            "У перемещения с типом Транспорт XDOC с транзитного на целевой склад " +
                "не должно быть перемещающего партнёра"
        );

        dto.setMovingPartnerId(null);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto)).hasMessage(
                "Для перемещения с типом Транспорт XDOC с транзитного на целевой склад " +
                    "должно быть указано число паллет"
        );

        dto.setPallets(10);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto)).hasMessage(
                "Для перемещения с типом Транспорт XDOC с транзитного на целевой склад " +
                    "должен быть задан интервал отгрузки"
        );

        dto.setOutboundTimeFrom(LocalTime.of(10, 0, 0)).setOutboundTimeTo(LocalTime.of(12, 0, 0));

        creationService.validateTransportation(dto);

        // with transport
        dto.setTransportId(2L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Указанные лог. точки не совпадают с лог. точками транспорта");

        dto.setTransportId(1L)
            .setInboundLogisticsPointId(2L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Плановое число паллет 10 больше вместимости машины 5");

        dto.setPallets(5);

        creationService.validateTransportation(dto);

    }

    @Test
    void testLinehaulValidations() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.LINEHAUL)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setMovingPartnerId(5L)
                .setDay(LocalDate.of(2021, 8, 5))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("У перемещения с типом Лайнхол из СЦ/ФФ в СЦ не должно быть перемещающего партнёра");

        dto.setMovingPartnerId(null);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Для перемещения с типом Лайнхол из СЦ/ФФ в СЦ должен быть задан интервал отгрузки");

        dto.setOutboundTimeFrom(LocalTime.of(10, 0, 0)).setOutboundTimeTo(LocalTime.of(12, 0, 0));

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("У перемещения с типом Лайнхол из СЦ/ФФ в СЦ должен быть указан транспорт");

        // with transport
        dto.setTransportId(2L);

        softly.assertThatThrownBy(() -> creationService.validateTransportation(dto))
            .hasMessage("Указанные лог. точки не совпадают с лог. точками транспорта");

        dto.setTransportId(1L)
            .setInboundLogisticsPointId(2L);

        creationService.validateTransportation(dto);

    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_xdoc_transport_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testXDockTransportationCreation() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.XDOC_TRANSPORT)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setPallets(10)
                .setDay(LocalDate.of(2021, 8, 5))
                .setOutboundTimeFrom(LocalTime.of(10, 0, 0))
                .setOutboundTimeTo(LocalTime.of(12, 0, 0))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));

        creationService.createTransportation(dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_orders_operation_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testOrdersOperationTransportationCreation() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setMovingPartnerId(1L)
                .setDay(LocalDate.of(2021, 8, 5))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));
        creationService.createTransportation(dto);

        Mockito.verify(bindingService).bindAllMatchingToTransportation(
            any(),
            eq(OrderBindingType.ON_TRANSPORTATION_CREATION),
            anyList()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_xdoc_transport_with_car_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testXDocWithTransportCreation() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.XDOC_TRANSPORT)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setPallets(4)
                .setTransportId(1L)
                .setDay(LocalDate.of(2021, 8, 5))
                .setOutboundTimeFrom(LocalTime.of(10, 0, 0))
                .setOutboundTimeTo(LocalTime.of(12, 0, 0))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));

        creationService.validateTransportation(dto);
        creationService.createTransportation(dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_linehaul_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testLinehaulCreation() {
        NewTransportationDto dto =
            new NewTransportationDto()
                .setTransportationType(AdminTransportationType.LINEHAUL)
                .setOutboundLogisticsPointId(1L)
                .setOutboundPartnerId(1L)
                .setInboundLogisticsPointId(2L)
                .setInboundPartnerId(2L)
                .setTransportId(1L)
                .setDay(LocalDate.of(2021, 8, 5))
                .setOutboundTimeFrom(LocalTime.of(10, 0, 0))
                .setOutboundTimeTo(LocalTime.of(12, 0, 0))
                .setInboundTimeFrom(LocalTime.of(15, 0, 0))
                .setInboundTimeTo(LocalTime.of(16, 0, 0));

        creationService.validateTransportation(dto);
        creationService.createTransportation(dto);
    }
}
