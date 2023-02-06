package ru.yandex.market.logistics.logistics4shops.controller.excludefromshipment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.ExcludeOrderFromShipmentApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentValidationResult;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExclusionFromShipmentRestrictedReason;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderCanNotBeExcludedReasonWrapper;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidateExcludeOrderFromShipmentRequest;
import ru.yandex.market.logistics.logistics4shops.factory.TmFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.ExclusionFromShipmentRestrictedReason.ORDER_FROM_SHIPMENT_EXCLUSION_DEADLINE_EXPIRED;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.ExclusionFromShipmentRestrictedReason.SHIPMENT_IS_CONFIRMED;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.OrderExclusionRestrictedReason.DOES_NOT_BELONG_TO_SHIPMENT;
import static ru.yandex.market.logistics.logistics4shops.client.api.model.OrderExclusionRestrictedReason.WAS_ALREADY_EXCLUDED;
import static ru.yandex.market.logistics.logistics4shops.factory.TmFactory.OUTBOUND_YANDEX_ID;

@DisplayName("Валидация возможности исключения заказов из отгрузки")
@ParametersAreNonnullByDefault
class ValidateExcludeOrderFromShipmentRequestTest extends AbstractIntegrationTest {
    @Autowired
    private TransportManagerClient transportManagerClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-12-16T11:29:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(
                LogisticsPointResponse.newBuilder()
                    .address(Address.newBuilder().locationId(213).build())
                    .build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, transportManagerClient);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки возможно, в l4s нет отгрузки")
    void exclusionIsAllowed() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .registers(
                    List.of(
                        RegisterDto.builder().type(RegisterType.FACT).id(1002L).build(),
                        RegisterDto.builder().type(RegisterType.PLAN).id(1001L).build()
                    )
                )
                .yandexId(OUTBOUND_YANDEX_ID)
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
                .build()
        );

        ExcludeOrderFromShipmentValidationResult result = buildRequest()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            buildResultWithShipmentReason(null)
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(transportManagerClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки возможно, в l4s есть отгрузка")
    @DatabaseSetup("/order/excludefromshipment/validate/before/outbound.xml")
    void exclusionIsAllowedL4sOutbound() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .registers(
                    List.of(
                        RegisterDto.builder().type(RegisterType.FACT).id(1002L).build(),
                        RegisterDto.builder().type(RegisterType.PLAN).id(1001L).build()
                    )
                )
                .yandexId(OUTBOUND_YANDEX_ID)
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
                .build()
        );
        ExcludeOrderFromShipmentValidationResult result = buildRequest()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            buildResultWithShipmentReason(null)
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(transportManagerClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки частично возможно - заказы не принадлежат отгрузке")
    @DatabaseSetup("/order/excludefromshipment/validate/before/outbound.xml")
    void exclusionIsPartiallyAllowedOrderDontBelongToShipment() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .registers(
                    List.of(
                        RegisterDto.builder().type(RegisterType.FACT).id(1002L).build(),
                        RegisterDto.builder().type(RegisterType.PLAN).id(1001L).build()
                    )
                )
                .yandexId(OUTBOUND_YANDEX_ID)
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
                .build()
        );

        ExcludeOrderFromShipmentValidationResult result = buildRequest(Set.of(101L, 104L))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            new ExcludeOrderFromShipmentValidationResult()
                .ordersCanBeExcluded(List.of(101L))
                .ordersCanNotBeExcludedReasons(List.of(
                    new OrderCanNotBeExcludedReasonWrapper().orderId(104L).reason(DOES_NOT_BELONG_TO_SHIPMENT)
                ))
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(transportManagerClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DatabaseSetup(value = {
        "/order/excludefromshipment/validate/before/outbound.xml",
        "/order/excludefromshipment/validate/before/order_was_already_excluded.xml"
    })
    @DisplayName("Исключение заказа из отгрузки частично возможно - заказ уже был исключен из отгрузки")
    void exclusionIsPartiallyAllowedOrderWasAlreadyExcluded() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .registers(
                    List.of(
                        RegisterDto.builder().type(RegisterType.FACT).id(1002L).build(),
                        RegisterDto.builder().type(RegisterType.PLAN).id(1001L).build()
                    )
                )
                .yandexId(OUTBOUND_YANDEX_ID)
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
                .build()
        );

        ExcludeOrderFromShipmentValidationResult result = buildRequest(Set.of(101L))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            new ExcludeOrderFromShipmentValidationResult()
                .ordersCanBeExcluded(List.of())
                .ordersCanNotBeExcludedReasons(List.of(
                    new OrderCanNotBeExcludedReasonWrapper().orderId(101L).reason(WAS_ALREADY_EXCLUDED)
                ))
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(transportManagerClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки невозможно - слишком поздно")
    void exclusionIsNotAllowedDeadlineExpired() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 14, 0))
                .build()
        );

        ExcludeOrderFromShipmentValidationResult result = buildRequest()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            buildResultWithShipmentReason(ORDER_FROM_SHIPMENT_EXCLUSION_DEADLINE_EXPIRED)
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки невозможно - в l4s есть подтвержденная отгрузка")
    @DatabaseSetup("/order/excludefromshipment/validate/before/outbound_confirmed.xml")
    void exclusionIsNotAllowedL4sShipmentIsConfirmed() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .yandexId(OUTBOUND_YANDEX_ID)
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
                .build()
        );

        ExcludeOrderFromShipmentValidationResult result = buildRequest()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            buildResultWithShipmentReason(SHIPMENT_IS_CONFIRMED)
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки возможно - отсутствует приёмка")
    @DatabaseSetup("/order/excludefromshipment/validate/before/outbound.xml")
    void exclusionIsNotAllowedOutboundDoesNotExist() {
        mockTransportManagerClient(
            TransportationUnitDto.builder()
                .yandexId(OUTBOUND_YANDEX_ID)
                .logisticPointId(1L)
                .plannedIntervalEnd(LocalDateTime.of(2021, 12, 16, 15, 0))
                .build()
        );

        ExcludeOrderFromShipmentValidationResult result = buildRequest()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            buildResultWithShipmentReason(null)
        );
        verify(transportManagerClient).getTransportation(1L);
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Отгрузка не найдена")
    void shipmentNotFound() {
        ApiError apiError = buildRequest()
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError.getMessage())
            .isEqualTo("Failed to find [SHIPMENT] with id [1]");
        verify(transportManagerClient).getTransportation(1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Исключение заказа из отгрузки невозможно - у отгрузки нет дедлайна")
    void shipmentNotHasNoDeadline(boolean checkL4s) {
        mockTransportManagerClient(null);
        ExcludeOrderFromShipmentValidationResult result = buildRequest()
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(
            buildResultWithShipmentReason(ORDER_FROM_SHIPMENT_EXCLUSION_DEADLINE_EXPIRED)
        );
        verify(transportManagerClient).getTransportation(1L);
    }

    @Test
    @DisplayName("Невалидный запрос")
    void requestIsInvalid() {
        ApiError error = apiClient.excludeOrderFromShipment()
            .validateExcludeOrderFromShipment()
            .shipmentIdPath(1L)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).contains("Required request body is missing");
    }

    @Nonnull
    private ExcludeOrderFromShipmentApi.ValidateExcludeOrderFromShipmentOper buildRequest() {
        return buildRequest(null);
    }

    @Nonnull
    private ExcludeOrderFromShipmentApi.ValidateExcludeOrderFromShipmentOper buildRequest(
        @Nullable Set<Long> orderIds
    ) {
        return apiClient.excludeOrderFromShipment()
            .validateExcludeOrderFromShipment()
            .shipmentIdPath(1L)
            .body(new ValidateExcludeOrderFromShipmentRequest().orderIds(orderIds));
    }

    @Nonnull
    private ExcludeOrderFromShipmentValidationResult buildResultWithShipmentReason(
        @Nullable ExclusionFromShipmentRestrictedReason reason
    ) {
        ExcludeOrderFromShipmentValidationResult result = new ExcludeOrderFromShipmentValidationResult();
        result.setExclusionFromShipmentRestrictedReason(reason);
        result.setOrdersCanBeExcluded(List.of());
        result.setOrdersCanNotBeExcludedReasons(List.of());
        return result;
    }

    void mockTransportManagerClient(@Nullable TransportationUnitDto transportationUnitDto) {
        when(transportManagerClient.getTransportation(1L))
            .thenReturn(Optional.of(new TransportationDto().setOutbound(transportationUnitDto)));
        when(transportManagerClient.searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class)))
            .thenReturn(new Page<RegisterUnitDto>().setData(List.of(TmFactory.registerUnitDto())));
    }
}
