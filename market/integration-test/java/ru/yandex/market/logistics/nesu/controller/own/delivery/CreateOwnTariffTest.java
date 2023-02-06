package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.tarifficator.CreateTariffRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.OWN_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.TARIFF_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.mockSearchOwnDeliveries;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.ownDeliveryFilter;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.partnerBuilder;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.tariffBuilder;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты создания тарифов собственных СД")
@DatabaseSetup("/repository/own-delivery/own_delivery.xml")
class CreateOwnTariffTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private TarifficatorClient tarifficatorClient;
    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
        verifyNoMoreInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("Создать тариф собственной СД магазина")
    void createOwnTariff() throws Exception {
        SearchPartnerFilter filter = ownDeliveryFilter(Set.of(OWN_PARTNER_ID)).build();
        mockSearchOwnDeliveries(
            List.of(partnerBuilder().build()),
            lmsClient,
            filter
        );
        when(tarifficatorClient.createTariff(safeRefEq(tariffBuilder().build())))
            .thenReturn(tariffBuilder().id(TARIFF_ID).build());

        execCreate(createRequest())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-tariff/create_tariff_response.json"));

        verify(lmsClient).searchPartners(filter);
        TariffSearchFilter allTariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID));
        allTariffFilter.setEnabled(true);
        verify(tarifficatorClient).searchTariffs(safeRefEq(allTariffFilter));
        verify(tarifficatorClient).createTariff(safeRefEq(tariffBuilder().build()));

        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(tarifficatorClient).updateTags(eq(TARIFF_ID), captor.capture());
        Set<String> tags = captor.getValue();
        softly.assertThat(tags).containsOnly("DAAS");
    }

    @Test
    @DisplayName("Попытка создать тариф собственной СД магазина. Превышено максимальное количество тарифов")
    void createOwnTariffMaxNumberExceeded() throws Exception {
        SearchPartnerFilter filter = ownDeliveryFilter(Set.of(OWN_PARTNER_ID)).build();
        mockSearchOwnDeliveries(
            List.of(partnerBuilder().build()),
            lmsClient,
            filter
        );
        TariffSearchFilter tariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID));
        tariffFilter.setEnabled(true);
        when(tarifficatorClient.searchTariffs(safeRefEq(tariffFilter))).thenReturn(List.of(tariffBuilder().build()));

        execCreate(createRequest())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/own-tariff/create_tariff_count_exceeded.json"));

        verify(lmsClient).searchPartners(filter);
        verify(tarifficatorClient).searchTariffs(safeRefEq(tariffFilter));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/own-delivery/own_delivery_restrictions_not_found.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @DisplayName("Попытка создать тариф собственной СД магазина. Настройки ограничений не найдены")
    void createOwnTariffRestrictionsNotFound() throws Exception {
        SearchPartnerFilter filter = ownDeliveryFilter(Set.of(OWN_PARTNER_ID)).build();
        mockSearchOwnDeliveries(
            List.of(partnerBuilder().build()),
            lmsClient,
            filter
        );

        execCreate(createRequest())
            .andExpect(status().is5xxServerError())
            .andExpect(errorMessage("Can't find own delivery restrictions for shop: 1"));

        verify(lmsClient).searchPartners(filter);
        TariffSearchFilter allTariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID));
        allTariffFilter.setEnabled(true);
        verify(tarifficatorClient).searchTariffs(safeRefEq(allTariffFilter));
    }

    @Test
    @DisplayName("Создать тариф собственной СД магазина, партнер не найден")
    void createOwnTariffNoPartner() throws Exception {
        execCreate(createRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [45]"));

        verify(lmsClient).searchPartners(ownDeliveryFilter(Set.of(OWN_PARTNER_ID)).build());
        verifyZeroInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("Создать тариф собственной СД магазина, партнер не активен")
    void createOwnTariffNotActivePartner() throws Exception {
        SearchPartnerFilter filter = ownDeliveryFilter(Set.of(OWN_PARTNER_ID)).build();
        mockSearchOwnDeliveries(
            List.of(partnerBuilder().status(PartnerStatus.INACTIVE).build()),
            lmsClient,
            filter
        );

        execCreate(createRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Partner with id 45 is inactive."));

        verify(lmsClient).searchPartners(filter);
        verifyZeroInteractions(tarifficatorClient);
    }

    @MethodSource("invalidCreateRequestProvider")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validateCreateDto(
        @SuppressWarnings("unused") String displayName,
        CreateTariffRequest createRequest,
        ValidationErrorData error
    ) throws Exception {
        execCreate(createRequest)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> invalidCreateRequestProvider() {
        return Stream.of(
            Arguments.of(
                "Не указан идентификатор партнёра",
                createRequest().setPartnerId(null),
                fieldError("partnerId", "must not be null", "createTariffRequest", "NotNull")
            ),
            Arguments.of(
                "Не указан способ доставки",
                createRequest().setDeliveryMethod(null),
                fieldError("deliveryMethod", "must not be null", "createTariffRequest", "NotNull")
            ),
            Arguments.of(
                "Не указано название тарифа",
                createRequest().setName(null),
                fieldError("name", "must not be blank", "createTariffRequest", "NotBlank")
            ),
            Arguments.of(
                "Указано пустое название тарифа",
                createRequest().setName(""),
                fieldError("name", "must not be blank", "createTariffRequest", "NotBlank")
            )
        );
    }

    @Nonnull
    private static CreateTariffRequest createRequest() {
        CreateTariffRequest createTariffRequest = new CreateTariffRequest()
            .setPartnerId(OWN_PARTNER_ID)
            .setDeliveryMethod(DeliveryMethod.COURIER);
        createTariffRequest
            .setDescription("my tariff description")
            .setEnabled(true)
            .setName("my tariff")
            .setCode("trf_code");
        return createTariffRequest;
    }

    @Nonnull
    private ResultActions execCreate(CreateTariffRequest request) throws Exception {
        return mockMvc.perform(
            post("/back-office/own-tariff")
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }
}
