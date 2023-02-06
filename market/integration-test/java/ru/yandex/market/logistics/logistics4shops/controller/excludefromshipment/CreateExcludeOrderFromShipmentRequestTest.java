package ru.yandex.market.logistics.logistics4shops.controller.excludefromshipment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.ExcludeOrderFromShipmentApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.CreateExcludeOrderFromShipmentRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4shops.factory.TmFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.factory.TmFactory.INVALID_ORDER_ID;
import static ru.yandex.market.logistics.logistics4shops.factory.TmFactory.ORDER_ID_0;
import static ru.yandex.market.logistics.logistics4shops.factory.TmFactory.ORDER_ID_1;
import static ru.yandex.market.logistics.logistics4shops.factory.TmFactory.ORDER_ID_2;

@DisplayName("Создание запроса на исключение заказа из отгрузки")
@ParametersAreNonnullByDefault
class CreateExcludeOrderFromShipmentRequestTest extends AbstractIntegrationTest {

    private static final long SHIPMENT_ID = 1000L;
    @Autowired
    protected TransportManagerClient tmClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-12-16T11:29:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(tmClient, lmsClient);
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {1} {2}")
    @MethodSource
    @DisplayName("Валидация запроса")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void requestValidation(Set<Long> orderIds, String field, String message) {
        ValidationError error = createRequests(orderIds)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .containsExactly(new ValidationViolation().field(field).message(message));
    }

    @Nonnull
    static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(null, "orderIds", "must not be null"),
            Arguments.of(Set.of(), "orderIds", "size must be between 1 and 100")
        );
    }

    @Test
    @DisplayName("Создание единственной заявки")
    @DatabaseSetup("/controller/excludefromshipment/create/before/outbound.xml")
    @ExpectedDatabase(
        value = "/controller/excludefromshipment/create/after/single_request_creation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void singleRequestCreation() {
        mockClients(null);
        var createExcludeOrderFromShipmentResponse = createRequests(Set.of(ORDER_ID_1))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));
        softly.assertThat(createExcludeOrderFromShipmentResponse).isNotNull();
        softly.assertThat(createExcludeOrderFromShipmentResponse.getExcludeOrderFromShipmentRequests())
            .containsExactlyInAnyOrderElementsOf(List.of(buildRequest(ORDER_ID_1)));
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(tmClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Создание нескольких заявок")
    @DatabaseSetup("/controller/excludefromshipment/create/before/outbound.xml")
    @ExpectedDatabase(
        value = "/controller/excludefromshipment/create/after/multiple_requests_creation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void multipleRequestsCreation() {
        mockClients(null);

        var createExcludeOrderFromShipmentResponse = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, ORDER_ID_2))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));
        softly.assertThat(createExcludeOrderFromShipmentResponse).isNotNull();
        softly.assertThat(createExcludeOrderFromShipmentResponse.getExcludeOrderFromShipmentRequests())
            .containsExactlyInAnyOrderElementsOf(List.of(
                buildRequest(ORDER_ID_0),
                buildRequest(ORDER_ID_1),
                buildRequest(ORDER_ID_2)
            ));
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(tmClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Для одного из заказов заявка уже создана")
    @DatabaseSetup("/controller/excludefromshipment/create/before/request_already_exists.xml")
    @ExpectedDatabase(
        value = "/controller/excludefromshipment/create/after/multiple_requests_creation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void requestAlreadyExists() {
        mockClients(null);
        var createExcludeOrderFromShipmentResponse = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, ORDER_ID_2))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(createExcludeOrderFromShipmentResponse.getExcludeOrderFromShipmentRequests())
            .containsExactlyInAnyOrderElementsOf(List.of(
                buildRequest(ORDER_ID_0),
                buildRequest(ORDER_ID_1),
                buildRequest(ORDER_ID_2)
            ));
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(tmClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Для одного из заказов заявка уже обработана")
    @DatabaseSetup("/controller/excludefromshipment/create/before/request_already_processed.xml")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void requestAlreadyProcessed() {
        mockClients(null);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, ORDER_ID_2))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message(
                "Orders with ids [%s] already have processed requests for exclusion from shipment."
                    .formatted(ORDER_ID_0))
        );
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(tmClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки невозможно - слишком поздно")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void shipmentDeadlineExpired() {
        clock.setFixed(Instant.parse("2021-12-16T15:45:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        mockClients(null);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, ORDER_ID_2))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(new ApiError().message("Shipment 1000 exclusion deadline expired"));
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Исключение заказа из отгрузки невозможно - отгрузка подтверждена")
    @DatabaseSetup("/controller/excludefromshipment/create/before/outbound_confirmed.xml")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void shipmentIsConfirmed() {
        mockClients(null);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, ORDER_ID_2))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(new ApiError().message("Shipment 1000 is already confirmed"));
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("Заказ не принадлежит этой отгрузке")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void orderDoesNotBelongToShipment() {
        mockClients(null);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, INVALID_ORDER_ID))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Orders with ids [%s] don't belong to shipment with id %s."
                .formatted(INVALID_ORDER_ID, SHIPMENT_ID))
        );
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(tmClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Test
    @DisplayName("mbiPartnerId невалидный")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void invalidMbiPartnerId() {
        mockClients(null);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, INVALID_ORDER_ID), 1L)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Failed to find dropship partner for mbiPartnerId 1")
        );
    }

    @Test
    @DisplayName("mbiPartnerId не принадлежит дропшипу")
    @DatabaseSetup(value = "/controller/excludefromshipment/create/before/dropship_by_seller_mapping_exists.xml")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void mbiPartnerIsNotDropship() {
        mockClients(null);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, INVALID_ORDER_ID), 1L)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Failed to find dropship partner for mbiPartnerId 1")
        );
    }

    @Test
    @DisplayName("Отгрузка не принадлежит магазину")
    @DatabaseSetup(value = "/controller/excludefromshipment/create/before/partner_mapping_exists.xml")
    @ExpectedDatabase(value = "/jobs/no_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    void singleRequestCreationWithMbiPartnerId() {
        mockClients(2L);
        var apiError = createRequests(Set.of(ORDER_ID_0, ORDER_ID_1, INVALID_ORDER_ID), 1L)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError().message("Failed to find [SHIPMENT] with id [1000]")
        );
        verify(tmClient).getTransportation(SHIPMENT_ID);
    }

    @Test
    @DisplayName("Успешное создание заявки с mbiPartnerId")
    @DatabaseSetup(value = "/controller/excludefromshipment/create/before/partner_mapping_exists.xml")
    @ExpectedDatabase(
        value = "/controller/excludefromshipment/create/after/single_request_creation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void requestCreationWithMbiPartnerId() {
        mockClients(300100L);
        var createExcludeOrderFromShipmentResponse = createRequests(Set.of(ORDER_ID_1), 1L)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));
        softly.assertThat(createExcludeOrderFromShipmentResponse).isNotNull();
        softly.assertThat(createExcludeOrderFromShipmentResponse.getExcludeOrderFromShipmentRequests())
            .containsExactlyInAnyOrderElementsOf(List.of(buildRequest(ORDER_ID_1)));
        verify(tmClient).getTransportation(SHIPMENT_ID);
        verify(tmClient).searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class));
        verify(lmsClient).getLogisticsPoint(1L);
    }

    @Nonnull
    private ExcludeOrderFromShipmentApi.CreateExcludeOrderFromShipmentRequestOper createRequests(
        @Nullable Set<Long> orderIds
    ) {
        return createRequests(orderIds, null);
    }

    @Nonnull
    private ExcludeOrderFromShipmentApi.CreateExcludeOrderFromShipmentRequestOper createRequests(
        @Nullable Set<Long> orderIds,
        @Nullable Long mbiPartnerId
    ) {
        return apiClient.excludeOrderFromShipment().createExcludeOrderFromShipmentRequest()
            .shipmentIdPath(SHIPMENT_ID)
            .mbiPartnerIdQuery(mbiPartnerId)
            .body(new CreateExcludeOrderFromShipmentRequest().orderIds(orderIds));
    }

    @Nonnull
    private ExcludeOrderFromShipmentRequestDto buildRequest(long orderId) {
        return new ExcludeOrderFromShipmentRequestDto()
            .shipmentId(SHIPMENT_ID)
            .orderId(orderId)
            .status(ExcludeOrderFromShipmentRequestStatus.CREATED);
    }

    private void mockClients(@Nullable Long partnerId) {
        when(tmClient.getTransportation(SHIPMENT_ID)).thenReturn(Optional.of(TmFactory.transportation(partnerId)));
        when(tmClient.searchRegisterUnits(eq(TmFactory.itemSearchFilter()), any(PageRequest.class)))
            .thenReturn(new Page<RegisterUnitDto>().setData(List.of(TmFactory.registerUnitDto())));
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(
            Optional.of(
                LogisticsPointResponse.newBuilder()
                    .address(Address.newBuilder().locationId(213).build())
                    .build()
            )
        );
    }
}
