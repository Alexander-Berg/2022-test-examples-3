package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.ScheduleDayDto;
import ru.yandex.market.logistics.nesu.dto.own.delivery.OwnDeliveryPickupPointDto;
import ru.yandex.market.logistics.nesu.dto.own.delivery.OwnPickupPointSearchFilter;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.OWN_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.ownDeliveryFilter;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.partnerBuilder;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты АПИ OwnPickupPointController")
@DatabaseSetup("/repository/own-delivery/own_delivery.xml")
public class OwnPickupPointControllerTest extends AbstractContextualTest {

    private static final long POINT_ID = 24L;

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Создать ПВЗ собственной СД магазина")
    void createPickupPoint() throws Exception {
        LogisticsPointFilter pointsFilter = searchPointsLmsFilter()
            .ids(null)
            .active(true)
            .partnerIds(Set.of(OWN_PARTNER_ID))
            .build();
        SearchPartnerFilter partnerFilter = ownDeliveryFilter()
            .setIds(Set.of(OWN_PARTNER_ID))
            .setStatuses(null)
            .build();
        when(lmsClient.searchPartners(partnerFilter))
            .thenReturn(List.of(partnerBuilder().build()));

        LogisticsPointCreateRequest createRequest = lmsPointCreateRequest().businessId(41L).build();
        LogisticsPointResponse response = lmsPointResponse().build();
        when(lmsClient.createLogisticsPoint(createRequest))
            .thenReturn(response);

        execCreate(pickupPointDto())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_pickup_point_response.json"));

        verify(lmsClient).searchPartners(partnerFilter);
        verify(lmsClient).createLogisticsPoint(createRequest);
        verify(lmsClient).getLogisticsPoints(safeRefEq(pointsFilter));
    }

    @Test
    @DisplayName("Ограничения на количество ПВЗ")
    void logisticsPointsRestriction() throws Exception {
        LogisticsPointFilter pointsFilter = searchPointsLmsFilter()
            .active(true)
            .ids(null)
            .partnerIds(Set.of(OWN_PARTNER_ID))
            .build();
        when(lmsClient.getLogisticsPoints(safeRefEq(pointsFilter)))
            .thenReturn(List.of(LogisticsPointResponse.newBuilder().active(true).build()));

        execCreate(pickupPointDto())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/own-delivery/restricted_pickup_point.json"));

        verify(lmsClient).getLogisticsPoints(safeRefEq(pointsFilter));
    }

    @Test
    @DisplayName("Партнер не принадлежит магазину")
    void createPickupPointInvalidPartner() throws Exception {
        execCreate(pickupPointDto())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [45]"));

        verify(lmsClient).getLogisticsPoints(
            safeRefEq(searchPointsLmsFilter().ids(null).active(true).partnerIds(Set.of(OWN_PARTNER_ID)).build())
        );
        verify(lmsClient).searchPartners(
            safeRefEq(ownDeliveryFilter().setStatuses(null).setIds(Set.of(OWN_PARTNER_ID)).build())
        );
    }

    @MethodSource("invalidPickupPointRequestProvider")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validateCreateDto(
        @SuppressWarnings("unused") String displayName,
        OwnDeliveryPickupPointDto createRequest,
        ValidationErrorData error
    ) throws Exception {
        execCreate(createRequest)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Test
    @DisplayName("Обновление ПВЗ собственной СД магазина")
    void updatePickupPoint() throws Exception {
        LogisticsPointFilter filter = searchPointsLmsFilter().build();
        when(lmsClient.getLogisticsPoints(safeRefEq(filter)))
            .thenReturn(List.of(lmsPointResponse().build()));

        LogisticsPointUpdateRequest updateRequest = lmsPointUpdateRequest().build();
        when(lmsClient.updateLogisticsPoint(eq(POINT_ID), safeRefEq(updateRequest)))
            .thenReturn(lmsPointResponse().build());

        execUpdate(pickupPointDto())
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(lmsClient).getLogisticsPoints(safeRefEq(filter));
        verify(lmsClient).updateLogisticsPoint(POINT_ID, updateRequest);
    }

    @Test
    @DisplayName("Ограничения при активации")
    void updatePickupPointActivationRestricted() throws Exception {
        LogisticsPointFilter filter = searchPointsLmsFilter().build();
        LogisticsPointResponse currentPoint = lmsPointResponse().active(false).build();
        when(lmsClient.getLogisticsPoints(safeRefEq(filter)))
            .thenReturn(List.of(currentPoint));

        LogisticsPointFilter allPointsFilter = searchPointsLmsFilter()
            .active(true)
            .ids(null)
            .partnerIds(Set.of(OWN_PARTNER_ID))
            .build();
        when(lmsClient.getLogisticsPoints(safeRefEq(allPointsFilter)))
            .thenReturn(List.of(currentPoint, LogisticsPointResponse.newBuilder().active(true).build()));

        execUpdate(pickupPointDto())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/own-delivery/restricted_pickup_point.json"));

        verify(lmsClient).getLogisticsPoints(safeRefEq(allPointsFilter));
        verify(lmsClient).getLogisticsPoints(safeRefEq(filter));
    }

    @Test
    @DisplayName("ПВЗ не найдена")
    void updatePickupPointNotFound() throws Exception {
        when(lmsClient.getLogisticsPoints(safeRefEq(searchPointsLmsFilter().build())))
            .thenReturn(List.of());

        execUpdate(pickupPointDto())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PICKUP_POINT] with ids [24]"));

        verify(lmsClient).getLogisticsPoints(safeRefEq(searchPointsLmsFilter().build()));
    }

    @MethodSource("invalidPickupPointRequestProvider")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validateUpdateDto(
        @SuppressWarnings("unused") String displayName,
        OwnDeliveryPickupPointDto updateRequest,
        ValidationErrorData error
    ) throws Exception {
        execUpdate(updateRequest)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Test
    @DisplayName("Поиск ПВЗ")
    void searchPickupPoint() throws Exception {
        LogisticsPointFilter filter = searchPointsLmsFilter()
            .partnerIds(Set.of(OWN_PARTNER_ID))
            .ids(Set.of(POINT_ID, 25L))
            .build();
        when(lmsClient.getLogisticsPoints(safeRefEq(filter)))
            .thenReturn(List.of(lmsPointResponse().build(), lmsPointResponse().id(25L).active(false).build()));

        execSearch(searchPointsFilterBuilder().ids(Set.of(POINT_ID, 25L)).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_pickup_point_search_response.json"));

        verify(lmsClient).getLogisticsPoints(safeRefEq(filter));
    }

    @Test
    @DisplayName("Не найден ПВЗ")
    void searchPickupPointNotFound() throws Exception {
        LogisticsPointFilter filter = searchPointsLmsFilter()
            .partnerIds(Set.of(OWN_PARTNER_ID))
            .ids(Set.of(POINT_ID, 25L))
            .build();
        when(lmsClient.getLogisticsPoints(safeRefEq(filter)))
            .thenReturn(List.of(lmsPointResponse().build()));

        execSearch(searchPointsFilterBuilder().ids(Set.of(POINT_ID, 25L)).build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PICKUP_POINT] with ids [25]"));

        verify(lmsClient).getLogisticsPoints(safeRefEq(filter));
    }

    @MethodSource("invalidPickupPointFilterProvider")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validateSearchFilter(
        @SuppressWarnings("unused") String displayName,
        OwnPickupPointSearchFilter filter,
        ValidationErrorData error
    ) throws Exception {
        execSearch(filter)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> invalidPickupPointRequestProvider() {
        return Stream.of(
            Arguments.of(
                "Не указано название точки",
                pickupPointDto().setName(null),
                fieldError("name", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Пустое указано название точки",
                pickupPointDto().setName("  "),
                fieldError("name", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Не указан регион",
                pickupPointDto().setRegion(null),
                fieldError("region", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Пустой регион",
                pickupPointDto().setRegion("  "),
                fieldError("region", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Не указан населенный пункт",
                pickupPointDto().setSettlement(null),
                fieldError("settlement", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Пустой населенный пункт",
                pickupPointDto().setSettlement("  "),
                fieldError("settlement", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Не указана улица",
                pickupPointDto().setStreet(null),
                fieldError("street", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Пустая улица",
                pickupPointDto().setStreet("  "),
                fieldError("street", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Не указан дом",
                pickupPointDto().setHouse(null),
                fieldError("house", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Пустой дом",
                pickupPointDto().setHouse("  "),
                fieldError("house", "must not be blank", "ownDeliveryPickupPointDto", "NotBlank")
            ),
            Arguments.of(
                "Не заполнен признак активности",
                pickupPointDto().setActive(null),
                fieldError("active", "must not be null", "ownDeliveryPickupPointDto", "NotNull")
            ),
            Arguments.of(
                "Пустое расписание",
                pickupPointDto().setSchedule(List.of()),
                fieldError("schedule", "must not be empty", "ownDeliveryPickupPointDto", "NotEmpty")
            ),
            Arguments.of(
                "Null в расписании",
                pickupPointDto().setSchedule(Arrays.asList(schedule(LocalTime.of(9, 0), LocalTime.of(12, 0)), null)),
                fieldError("schedule[1]", "must not be null", "ownDeliveryPickupPointDto", "NotNull")
            ),
            Arguments.of(
                "Пересечения в расписании",
                pickupPointDto().setSchedule(List.of(
                    schedule(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                    schedule(LocalTime.of(11, 0), LocalTime.of(22, 0))
                )),
                fieldError(
                    "schedule[]",
                    "Schedule must not contain intersections",
                    "ownDeliveryPickupPointDto",
                    "ValidSchedule"
                )
            ),
            Arguments.of(
                "Невалидный интервал в расписании",
                pickupPointDto().setSchedule(List.of(schedule(LocalTime.of(9, 0), LocalTime.of(8, 0)))),
                fieldError(
                    "schedule[]",
                    "Time interval start must be before time interval end",
                    "ownDeliveryPickupPointDto",
                    "ValidSchedule"
                )
            ),
            Arguments.of(
                "День - null",
                pickupPointDto().setSchedule(List.of(schedule(LocalTime.of(9, 0), LocalTime.of(22, 0)).setDay(null))),
                fieldError("schedule[0].day", "must not be null", "ownDeliveryPickupPointDto", "NotNull")
            ),
            Arguments.of(
                "День больше 7",
                pickupPointDto().setSchedule(List.of(schedule(LocalTime.of(9, 0), LocalTime.of(22, 0)).setDay(8))),
                fieldError(
                    "schedule[0].day",
                    "must be less than or equal to 7",
                    "ownDeliveryPickupPointDto",
                    "Max",
                    Map.of("value", 7)
                )
            ),
            Arguments.of(
                "Время начала - null",
                pickupPointDto().setSchedule(List.of(schedule(null, LocalTime.of(22, 0)))),
                fieldError(
                    "schedule[0].timeFrom",
                    "must not be null",
                    "ownDeliveryPickupPointDto",
                    "NotNull"
                )
            ),
            Arguments.of(
                "Время конца - null",
                pickupPointDto().setSchedule(List.of(schedule(LocalTime.of(9, 0), null))),
                fieldError("schedule[0].timeTo", "must not be null", "ownDeliveryPickupPointDto", "NotNull")
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidPickupPointFilterProvider() {
        return Stream.of(
            Arguments.of(
                "Null в идентификаторах",
                searchPointsFilterBuilder().ids(Sets.newHashSet(1L, null)).build(),
                fieldError("ids[]", "must not be null", "ownPickupPointSearchFilter", "NotNull")
            ),
            Arguments.of(
                "Null в идентификаторах партнеров",
                searchPointsFilterBuilder().partnerIds(Sets.newHashSet(1L, null)).build(),
                fieldError(
                    "partnerIds[]",
                    "must not be null",
                    "ownPickupPointSearchFilter",
                    "NotNull"
                )
            )
        );
    }

    @Nonnull
    private ResultActions execCreate(OwnDeliveryPickupPointDto request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/back-office/own-pickup-point/" + OWN_PARTNER_ID, request)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execUpdate(OwnDeliveryPickupPointDto request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/own-pickup-point/" + POINT_ID, request)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execSearch(OwnPickupPointSearchFilter filter) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/own-pickup-point/search", filter)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private static OwnPickupPointSearchFilter.OwnPickupPointSearchFilterBuilder searchPointsFilterBuilder() {
        return OwnPickupPointSearchFilter.builder()
            .ids(Set.of(POINT_ID))
            .partnerIds(Set.of(OWN_PARTNER_ID));
    }

    @Nonnull
    private static LogisticsPointFilter.LogisticsPointFilterBuilder searchPointsLmsFilter() {
        return LogisticsPointFilter.newBuilder()
            .ids(Set.of(POINT_ID))
            .businessIds(Set.of(41L))
            .type(PointType.PICKUP_POINT);
    }

    @Nonnull
    private static OwnDeliveryPickupPointDto pickupPointDto() {
        return new OwnDeliveryPickupPointDto()
            .setStreet("Льва Толстого")
            .setSettlement("Москва")
            .setSchedule(List.of(schedule(LocalTime.of(9, 0), LocalTime.of(22, 0))))
            .setRegion("МО")
            .setSubRegion("МО")
            .setPostCode("653000")
            .setPhone("+79998887776")
            .setName("own pickup")
            .setInstruction("some instructions")
            .setHousing("h5")
            .setHouse("5")
            .setExternalId("own ext id")
            .setCashAllowed(true)
            .setCardAllowed(false)
            .setBuilding("15")
            .setActive(true)
            .setApartment("3a");
    }

    @Nonnull
    private static ScheduleDayDto schedule(LocalTime timeFrom, LocalTime to) {
        return new ScheduleDayDto()
            .setDay(1)
            .setTimeFrom(timeFrom)
            .setTimeTo(to);
    }

    @Nonnull
    private static LogisticsPointCreateRequest.Builder lmsPointCreateRequest() {
        return LogisticsPointCreateRequest.newBuilder()
            .active(true)
            .address(createAddress(null))
            .cardAllowed(false)
            .cashAllowed(true)
            .externalId("own ext id")
            .instruction("some instructions")
            .isFrozen(false)
            .businessId(100L)
            .partnerId(OWN_PARTNER_ID)
            .name("own pickup")
            .phones(Set.of(new Phone("+79998887776", null, null, PhoneType.PRIMARY)))
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .prepayAllowed(true)
            .returnAllowed(false)
            .schedule(Set.of(
                new ScheduleDayResponse(null, 1, LocalTime.of(9, 0), LocalTime.of(22, 0))
            ))
            .type(PointType.PICKUP_POINT)
            .maxWidth(500)
            .maxLength(500)
            .maxHeight(500)
            .maxSidesSum(1500)
            .maxWeight(500.0);
    }

    @Nonnull
    private static Address createAddress(@Nullable Integer locationId) {
        return Address.newBuilder()
            .locationId(locationId)
            .settlement("Москва")
            .postCode("653000")
            .street("Льва Толстого")
            .house("5")
            .housing("h5")
            .building("15")
            .apartment("3a")
            .region("МО")
            .subRegion("МО")
            .build();
    }

    @Nonnull
    private static LogisticsPointUpdateRequest.Builder lmsPointUpdateRequest() {
        return LogisticsPointUpdateRequest.newBuilder()
            .externalId("own ext id")
            .active(true)
            .cardAllowed(false)
            .cashAllowed(true)
            .prepayAllowed(true)
            .returnAllowed(false)
            .instruction("some instructions")
            .isFrozen(false)
            .name("own pickup")
            .phones(Set.of(new Phone("+79998887776", null, null, PhoneType.PRIMARY)))
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .schedule(Set.of(
                new ScheduleDayResponse(null, 1, LocalTime.of(9, 0), LocalTime.of(22, 0))
            ))
            .marketBranded(false)
            .maxWidth(500)
            .maxLength(500)
            .maxHeight(500)
            .maxSidesSum(1500)
            .maxWeight(500.0);
    }

    @Nonnull
    private static LogisticsPointResponse.LogisticsPointResponseBuilder lmsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(POINT_ID)
            .active(true)
            .partnerId(OWN_PARTNER_ID)
            .address(createAddress(213))
            .cardAllowed(false)
            .cashAllowed(true)
            .contact(null)
            .externalId("own ext id")
            .instruction("some instructions")
            .isFrozen(false)
            .name("own pickup")
            .phones(Set.of(new Phone("+79998887776", null, null, PhoneType.PRIMARY)))
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .prepayAllowed(true)
            .returnAllowed(false)
            .schedule(Set.of(
                new ScheduleDayResponse(null, 1, LocalTime.of(9, 0), LocalTime.of(22, 0))
            ))
            .type(PointType.PICKUP_POINT);
    }
}
