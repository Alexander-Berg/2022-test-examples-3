package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.tarifficator.UpdateTariffRequest;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffUpdateDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.OWN_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.tariffBuilder;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты обновления тарифов собственных СД")
@DatabaseSetup("/repository/own-delivery/own_delivery.xml")
class UpdateOwnTariffTest extends AbstractContextualTest {

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
    @DisplayName("Обновить собственный тариф магазина")
    void updateOwnTariff() throws Exception {
        when(tarifficatorClient.getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.tariffBuilder().build()));
        when(lmsClient.getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.partnerBuilder().build()));
        when(tarifficatorClient.updateTariff(
            eq(TestOwnDeliveryUtils.TARIFF_ID),
            safeRefEq(updateTariffBuilder().build())
        ))
            .thenReturn(TestOwnDeliveryUtils.tariffBuilder().id(TestOwnDeliveryUtils.TARIFF_ID).build());

        execUpdate(updateRequest())
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(tarifficatorClient).getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID);
        verify(lmsClient).getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID);
        verify(tarifficatorClient).updateTariff(
            eq(TestOwnDeliveryUtils.TARIFF_ID),
            safeRefEq(updateTariffBuilder().enabled(true).build())
        );
    }

    @Test
    @DisplayName("Активация тарифа. Превышено максимальное количество тарифов")
    void activateOwnTariffMaxNumberExceeded() throws Exception {
        when(tarifficatorClient.getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.tariffBuilder().enabled(false).build()));
        when(lmsClient.getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.partnerBuilder().build()));

        TariffSearchFilter tariffFilter = new TariffSearchFilter().setPartnerIds(Set.of(OWN_PARTNER_ID));
        tariffFilter.setEnabled(true);
        when(tarifficatorClient.searchTariffs(safeRefEq(tariffFilter))).thenReturn(List.of(tariffBuilder().build()));

        execUpdate(updateRequest())
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/own-tariff/create_tariff_count_exceeded.json"));

        verify(tarifficatorClient).getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID);
        verify(tarifficatorClient).searchTariffs(safeRefEq(tariffFilter));
        verify(lmsClient).getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID);
    }

    @Test
    @DisplayName("Обновить собственный тариф магазина, тариф не найден")
    void updateOwnTariffNotFound() throws Exception {
        execUpdate(updateRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [12]"));

        verifyZeroInteractions(lmsClient);
        verify(tarifficatorClient).getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID);
    }

    @Test
    @DisplayName("Попытка обновления чужого тарифа")
    void updateNotOwnedTariff() throws Exception {
        when(tarifficatorClient.getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.tariffBuilder().build()));
        when(lmsClient.getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.partnerBuilder().marketId(2L).businessId(100L).build()));

        execUpdate(updateRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [12]"));

        verify(tarifficatorClient).getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID);
        verify(lmsClient).getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID);
    }

    @Test
    @DisplayName("Попытка обновления тарифа неактивного партнера")
    void updateInactivePartnerTariff() throws Exception {
        when(tarifficatorClient.getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.tariffBuilder().build()));
        when(lmsClient.getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.partnerBuilder().status(PartnerStatus.INACTIVE).build()));

        execUpdate(updateRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Partner with id 45 is inactive."));

        verify(tarifficatorClient).getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID);
        verify(lmsClient).getPartner(TestOwnDeliveryUtils.OWN_PARTNER_ID);
    }

    @MethodSource("invalidUpdateRequestProvider")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validateUpdateDto(
        @SuppressWarnings("unused") String displayName,
        UpdateTariffRequest createRequest,
        ValidationErrorData error
    ) throws Exception {
        execUpdate(createRequest)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> invalidUpdateRequestProvider() {
        return Stream.of(
            Arguments.of(
                "Указано пустое название тарифа",
                updateRequest().setName(""),
                fieldError(
                    "name",
                    "must not be blank",
                    "updateTariffRequest",
                    "NotBlank"
                )
            ),
            Arguments.of(
                "Указано слишком длинное название тарифа",
                updateRequest().setName("a".repeat(129)),
                fieldError(
                    "name",
                    "size must be between 0 and 128",
                    "updateTariffRequest",
                    "Size",
                    Map.of("min", 0, "max", 128)
                )
            ),
            Arguments.of(
                "Указан слишком длинный код тарифа",
                updateRequest().setCode("a".repeat(129)),
                fieldError(
                    "code",
                    "size must be between 0 and 128",
                    "updateTariffRequest",
                    "Size",
                    Map.of("min", 0, "max", 128)
                )
            )
        );
    }

    @Nonnull
    private static TariffUpdateDto.TariffUpdateDtoBuilder updateTariffBuilder() {
        return TariffUpdateDto.builder()
            .name("my tariff")
            .enabled(false)
            .description("my tariff description")
            .code("trf_code");
    }

    @Nonnull
    private static UpdateTariffRequest updateRequest() {
        UpdateTariffRequest updateRequest = new UpdateTariffRequest();
        updateRequest
            .setCode("trf_code")
            .setEnabled(true)
            .setName("my tariff")
            .setDescription("my tariff description");
        return updateRequest;
    }

    @Nonnull
    private ResultActions execUpdate(UpdateTariffRequest request) throws Exception {
        return mockMvc.perform(
            put("/back-office/own-tariff/" + TestOwnDeliveryUtils.TARIFF_ID)
                .param("userId", "1")
                .param("shopId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }
}
