package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partner.UpdatePartnerDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerShipmentSettingsDto;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientPartnerDto;
import ru.yandex.market.logistics.management.entity.type.AllowedShipmentWay;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.own.delivery.OwnDeliveryDto;
import ru.yandex.market.logistics.nesu.dto.own.delivery.OwnDeliveryFilter;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.OWN_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.ownDeliveryFilter;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты АПИ OwnDeliveryController")
@DatabaseSetup("/repository/own-delivery/own_delivery.xml")
class OwnDeliveryControllerTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Создать собственную СД магазина")
    void createOwnDelivery() throws Exception {
        PartnerResponse partner = partnerResponse().businessId(41L).build();
        SearchPartnerFilter filter = ownDeliveryFilter().build();
        CreatePartnerDto createRequest = createRequest().businessId(41L).build();
        PlatformClientPartnerDto platformClientPartnerDto = platformClientPartnerDtoBuilder().build();

        when(lmsClient.createPartner(createRequest)).thenReturn(partner);
        when(lmsClient.changePartnerStatus(partner.getId(), PartnerStatus.ACTIVE)).thenReturn(partner);
        when(lmsClient.addOrUpdatePlatformClientPartner(platformClientPartnerDto)).thenReturn(partner);

        execCreate(defaultRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_delivery_response.json"));

        verify(lmsClient).searchPartners(filter);
        verify(lmsClient).createPartner(createRequest);
        verify(lmsClient).changePartnerStatus(partner.getId(), PartnerStatus.ACTIVE);
        verify(lmsClient).addOrUpdatePlatformClientPartner(platformClientPartnerDto);
    }

    @Test
    @DisplayName("Создать собственную СД магазина со статусом TESTING")
    void createOwnDeliveryTesting() throws Exception {
        PartnerResponse partner = partnerResponse().status(PartnerStatus.TESTING).businessId(41L).build();
        SearchPartnerFilter filter = ownDeliveryFilter().build();
        CreatePartnerDto createRequest = createRequest().businessId(41L).build();
        PlatformClientPartnerDto platformClientPartnerDto = platformClientPartnerDtoBuilder().build();

        when(lmsClient.createPartner(createRequest)).thenReturn(partner);
        when(lmsClient.changePartnerStatus(partner.getId(), PartnerStatus.TESTING)).thenReturn(partner);
        when(lmsClient.addOrUpdatePlatformClientPartner(platformClientPartnerDto)).thenReturn(partner);

        execCreate(defaultRequest().setStatus(PartnerStatus.TESTING))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/create_own_delivery_testing_response.json"));

        verify(lmsClient).searchPartners(filter);
        verify(lmsClient).createPartner(createRequest);
        verify(lmsClient).changePartnerStatus(partner.getId(), PartnerStatus.TESTING);
        verify(lmsClient).addOrUpdatePlatformClientPartner(platformClientPartnerDto);
    }

    @Test
    @DisplayName("Попытка создать собственную СД магазина. Превышено максимальное количество собственных СД")
    void createOwnDeliveryMaxNumberExceeded() throws Exception {
        PartnerResponse partner = partnerResponse().build();
        SearchPartnerFilter filter = ownDeliveryFilter().build();

        when(lmsClient.searchPartners(filter)).thenReturn(List.of(partner));

        execCreate(defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/own-delivery/create_own_delivery_ds_count_exceeded.json"));

        verify(lmsClient).searchPartners(filter);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/own-delivery/own_delivery_restrictions_not_found.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @DisplayName("Попытка создать собственную СД магазина. Настройки ограничений не найдены")
    void createOwnDeliveryRestrictionsNotFound() throws Exception {
        execCreate(defaultRequest())
            .andExpect(status().is5xxServerError())
            .andExpect(errorMessage("Can't find own delivery restrictions for shop: 1"));

        verify(lmsClient).searchPartners(ownDeliveryFilter().build());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationSource")
    @DisplayName("Валидация создания собственных СД магазинов")
    void createValidation(ValidationErrorData error, UnaryOperator<OwnDeliveryDto> requestModifier) throws Exception {
        execCreate(requestModifier.apply(defaultRequest()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationSource")
    @DisplayName("Валидация обновления собственных СД магазинов")
    void updateValidation(ValidationErrorData error, UnaryOperator<OwnDeliveryDto> requestModifier) throws Exception {
        execUpdate(requestModifier.apply(defaultRequest()))
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> validationSource() {
        return Stream.<Pair<ValidationErrorDataBuilder, UnaryOperator<OwnDeliveryDto>>>of(
            Pair.of(
                fieldErrorBuilder("name", ErrorType.NOT_BLANK),
                request -> request.setName(null)
            ),
            Pair.of(
                fieldErrorBuilder("name", ErrorType.NOT_BLANK),
                request -> request.setName(" \n\t ")
            ),
            Pair.of(
                fieldErrorBuilder("readableName", ErrorType.NOT_BLANK),
                request -> request.setReadableName(null)
            ),
            Pair.of(
                fieldErrorBuilder("readableName", ErrorType.NOT_BLANK),
                request -> request.setReadableName(" \n\t ")
            ),
            Pair.of(
                fieldErrorBuilder("status", ErrorType.NOT_NULL),
                request -> request.setStatus(null)
            )
        ).map(pair -> Arguments.of(pair.getLeft().forObject("ownDeliveryDto"), pair.getRight()));
    }

    @Test
    @DisplayName("Обновить собственную СД магазина")
    void updateOwnDelivery() throws Exception {
        PartnerResponse partner = partnerResponse().build();
        SearchPartnerFilter filter = ownDeliveryFilter().setStatuses(null).setIds(Set.of(100500L)).build();
        UpdatePartnerDto updateRequest = updateRequest().billingClientId(46L).build();

        when(lmsClient.searchPartners(filter)).thenReturn(List.of(partner));
        when(lmsClient.updatePartner(100500L, updateRequest)).thenReturn(partner);

        execUpdate(defaultRequest())
            .andExpect(status().isOk());

        verify(lmsClient).searchPartners(filter);
        verify(lmsClient).updatePartner(100500L, updateRequest);
    }

    @Test
    @DisplayName("При обновлении не затираем имеющиеся данные")
    void updateOwnDeliveryPreserveData() throws Exception {
        PartnerResponse partner = partnerResponse()
            .domain("domain")
            .logoUrl("logo")
            .build();
        SearchPartnerFilter filter = ownDeliveryFilter().setStatuses(null).setIds(Set.of(100500L)).build();
        UpdatePartnerDto updateRequest = updateRequest()
            .domain("domain")
            .logoUrl("logo")
            .billingClientId(46L)
            .build();

        when(lmsClient.searchPartners(filter)).thenReturn(List.of(partner));
        when(lmsClient.updatePartner(100500L, updateRequest)).thenReturn(partner);

        execUpdate(defaultRequest())
            .andExpect(status().isOk());

        verify(lmsClient).searchPartners(filter);
        verify(lmsClient).updatePartner(100500L, updateRequest);
    }

    @Test
    @DisplayName("Активация запрещена")
    void activateRestricted() throws Exception {
        PartnerResponse partner = partnerResponse().status(PartnerStatus.INACTIVE).build();
        SearchPartnerFilter filter = ownDeliveryFilter().setStatuses(null).setIds(Set.of(100500L)).build();
        when(lmsClient.searchPartners(filter)).thenReturn(List.of(partner));

        SearchPartnerFilter allPartnersFilter = ownDeliveryFilter().build();
        when(lmsClient.searchPartners(allPartnersFilter)).thenReturn(List.of(partner));

        execUpdate(defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/own-delivery/create_own_delivery_ds_count_exceeded.json"));

        verify(lmsClient).searchPartners(allPartnersFilter);
        verify(lmsClient).searchPartners(filter);
    }

    @Test
    @DisplayName("Обновление собственной СД магазина, СД не найдена")
    void updateOwnDeliveryNotFound() throws Exception {
        execUpdate(defaultRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [100500]"));

        verify(lmsClient).searchPartners(ownDeliveryFilter().setStatuses(null).setIds(Set.of(100500L)).build());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchSource")
    @DisplayName("Поиск собственных СД")
    void searchTest(
        @SuppressWarnings("unused") String displayName,
        SearchPartnerFilter partnerFilter,
        OwnDeliveryFilter ownDeliveryFilter
    ) throws Exception {
        when(lmsClient.searchPartners(partnerFilter))
            .thenReturn(List.of(
                partnerResponse().build(),
                partnerResponse()
                    .id(2L)
                    .marketId(1L)
                    .name("own delivery 2")
                    .readableName("readable own delivery 2")
                    .partnerType(PartnerType.OWN_DELIVERY)
                    .status(PartnerStatus.INACTIVE)
                    .build()
            ));

        execSearch(ownDeliveryFilter)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/two_own_deliveries_response.json"));

        verify(lmsClient).searchPartners(partnerFilter);
    }

    @Nonnull
    private static Stream<Arguments> searchSource() {
        return Stream.of(
            Arguments.of(
                "Получение списка собственных СД",
                ownDeliveryFilter().setStatuses(null).build(),
                searchRequest(null, null)
            ),
            Arguments.of(
                "Поиск партнеров по фильтру",
                ownDeliveryFilter()
                    .setStatuses(Set.of(PartnerStatus.ACTIVE))
                    .setIds(Set.of(OWN_PARTNER_ID, 100500L))
                    .build(),
                searchRequest(Set.of(OWN_PARTNER_ID, 100500L), Set.of(PartnerStatus.ACTIVE))
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("invalidSearchSource")
    @DisplayName("Валидация фильтра поиска собственных СД")
    void invalidSearchTest(ValidationErrorData error, OwnDeliveryFilter ownDeliveryFilter) throws Exception {
        execSearch(ownDeliveryFilter)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> invalidSearchSource() {
        return Stream.of(
            Pair.of(
                fieldErrorBuilder("ids[]", ErrorType.NOT_NULL),
                searchRequest(Collections.singleton(null), null)
            ),
            Pair.of(
                fieldErrorBuilder("statuses[]", ErrorType.NOT_NULL),
                searchRequest(null, Collections.singleton(null))
            )
        ).map(t -> Arguments.of(t.getLeft().forObject("ownDeliveryFilter"), t.getRight()));
    }

    @Test
    @DisplayName("Получение собственной СД по идентификатору")
    void getOwnDelivery() throws Exception {
        SearchPartnerFilter filter = ownDeliveryFilter().setStatuses(null).setIds(Set.of(100500L)).build();

        when(lmsClient.searchPartners(filter)).thenReturn(List.of(partnerResponse().build()));

        execGet()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_delivery_response.json"));

        verify(lmsClient).searchPartners(filter);
    }

    @Test
    @DisplayName("Получение собственной СД по идентификатору, СД не найдена")
    void getOwnDeliveryNotFound() throws Exception {
        execGet()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [100500]"));

        verify(lmsClient).searchPartners(
            ownDeliveryFilter().setStatuses(null).setIds(Set.of(100500L)).build()
        );
    }

    @Test
    @DisplayName("Получение ограничений собственной СД магазина")
    @DatabaseSetup(value = "/repository/own-delivery/own_restrictions.xml", type = DatabaseOperation.UPDATE)
    void getRestrictions() throws Exception {
        execGetRestrictions()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_restrictions_response.json"));
    }

    @Test
    @DisplayName("Не заданы ограничения")
    @DatabaseSetup(value = "/repository/own-delivery/own_restrictions.xml", type = DatabaseOperation.DELETE)
    void getRestrictionsNotFound() throws Exception {
        execGetRestrictions()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_OWN_DELIVERY_RESTRICTIONS] with ids [1]"));
    }

    @Nonnull
    private PartnerResponse.PartnerResponseBuilder partnerResponse() {
        return PartnerResponse.newBuilder()
            .id(100500L)
            .marketId(1L)
            .name("own delivery")
            .readableName("readable own delivery")
            .partnerType(PartnerType.OWN_DELIVERY)
            .status(PartnerStatus.ACTIVE);
    }

    @Nonnull
    private CreatePartnerDto.Builder createRequest() {
        return CreatePartnerDto.newBuilder()
            .name("own delivery")
            .readableName("readable own delivery")
            .partnerType(PartnerType.OWN_DELIVERY)
            .businessId(100L)
            .marketId(1L);
    }

    @Nonnull
    private UpdatePartnerDto.Builder updateRequest() {
        return UpdatePartnerDto.newBuilder()
            .name("own delivery")
            .readableName("readable own delivery")
            .status(PartnerStatus.ACTIVE);
    }

    @Nonnull
    private PlatformClientPartnerDto.PlatformClientPartnerDtoBuilder platformClientPartnerDtoBuilder() {
        return PlatformClientPartnerDto.newBuilder()
            .partnerId(100500L)
            .platformClientId(PlatformClientId.YANDEX_DELIVERY.getId())
            .status(PartnerStatus.ACTIVE)
            .shipmentSettings(Set.of(
                PartnerShipmentSettingsDto.newBuilder()
                    .shipmentType(ShipmentType.WITHDRAW)
                    .allowedShipmentWay(AllowedShipmentWay.DIRECTLY)
                    .build()
            ));
    }

    @Nonnull
    private static OwnDeliveryFilter searchRequest(@Nullable Set<Long> ids, @Nullable Set<PartnerStatus> statuses) {
        return OwnDeliveryFilter.builder().ids(ids).statuses(statuses).build();
    }

    @Nonnull
    private static OwnDeliveryDto defaultRequest() {
        return new OwnDeliveryDto()
            .setName("own delivery")
            .setReadableName("readable own delivery")
            .setStatus(PartnerStatus.ACTIVE);
    }

    @Nonnull
    private ResultActions execCreate(OwnDeliveryDto createDto) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/back-office/own-delivery", createDto)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execUpdate(OwnDeliveryDto updateDto) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/own-delivery/100500", updateDto)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execSearch(OwnDeliveryFilter filter) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/own-delivery/search", filter)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execGet() throws Exception {
        return mockMvc.perform(
            get("/back-office/own-delivery/100500")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    @Nonnull
    private ResultActions execGetRestrictions() throws Exception {
        return mockMvc.perform(
            get("/back-office/own-delivery/restrictions")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
        );
    }
}
