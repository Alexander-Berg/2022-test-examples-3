package ru.yandex.market.logistics.nesu.controller.settings;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.dto.SenderVatOptionsUpdateRequestDto;
import ru.yandex.market.logistics.nesu.model.dto.SenderVatOptionsUpdateRequestDtoListWrapper;
import ru.yandex.market.logistics.nesu.utils.MatcherUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class SettingsControllerVatTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешное получение настроек ставок НДС у сендеров магазина")
    @DatabaseSetup("/repository/settings/vat/before/senders_with_vat_options_settings.xml")
    void getShopSendersVatOptionsSuccess() throws Exception {
        getTaxSettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/settings/sender/senders_with_tax_options_settings.json"));
    }

    @Test
    @DisplayName("Успешное получение настроек ставок НДС у сендера без настроек ставок НДС")
    @DatabaseSetup("/repository/settings/vat/before/sender_without_vat_options_settings.xml")
    void getShopSendersWithoutVatOptionsSuccess() throws Exception {
        getTaxSettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/settings/sender/sender_without_tax_options_settings.json"));
    }

    @Test
    @DisplayName("Получение настроек ставок НДС у сендеров несуществующего магазина")
    void getShopSendersVatOptionsFailNoShop() throws Exception {
        getTaxSettings()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @Test
    @DisplayName("Обновление настроек ставок НДС у сендеров магазина")
    @DatabaseSetup("/repository/settings/vat/before/senders_with_vat_options_settings.xml")
    @ExpectedDatabase(
        value = "/repository/settings/vat/after/senders_with_updated_vat_options_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateShopSendersVatOptionsSuccess() throws Exception {
        setTaxSettings(defaultUpdateRequest())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Настройка ставок НДС у сендеров магазина")
    @DatabaseSetup("/repository/settings/vat/before/sender_without_vat_options_settings.xml")
    @ExpectedDatabase(
        value = "/repository/settings/vat/after/sender_with_created_vat_options_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createShopSendersVatOptionsSuccess() throws Exception {
        setTaxSettings(defaultCreateRequest())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Настройка ставок НДС у несуществующего сендера")
    void setShopSendersVatOptionsFailNoSender() throws Exception {
        setTaxSettings(defaultCreateRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [1]"));
    }

    @Test
    @DisplayName("Настройка ставок НДС у сендера, не принадлежащего магазину")
    @DatabaseSetup("/repository/settings/vat/before/multiple_shops_with_senders.xml")
    @ExpectedDatabase(
        value = "/repository/settings/vat/before/multiple_shops_with_senders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setShopSendersVatOptionsFailSenderDoesNotBelongToShop() throws Exception {
        setTaxSettings(defaultCreateRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [1]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация запроса")
    @MethodSource("getRequestsForValidation")
    void requestValidation(
        @SuppressWarnings("unused") String caseName,
        SenderVatOptionsUpdateRequestDtoListWrapper request,
        String fieldName
    ) throws Exception {
        setTaxSettings(request)
            .andExpect(status().isBadRequest())
            .andExpect(MatcherUtils.validationErrorMatcher(fieldError(
                fieldName,
                "must not be null",
                "senderVatOptionsUpdateRequestDtoListWrapper",
                "NotNull"
            )));
    }

    @Nonnull
    private static Stream<Arguments> getRequestsForValidation() {
        return Stream.of(
            Triple.of(
                "null senderVatOptionsUpdateRequest",
                Collections.singletonList((SenderVatOptionsUpdateRequestDto) null),
                "senderVatOptionsUpdateRequests[0]"
            ),
            Triple.of(
                "null senderId",
                List.of(new SenderVatOptionsUpdateRequestDto(null, List.of())),
                "senderVatOptionsUpdateRequests[0].senderId"
            ),
            Triple.of(
                "null vatOptions",
                List.of(new SenderVatOptionsUpdateRequestDto(1L, null)),
                "senderVatOptionsUpdateRequests[0].newEnabledVatOptions"
            ),
            Triple.of(
                "null vatOption",
                List.of(new SenderVatOptionsUpdateRequestDto(1L, Collections.singletonList(null))),
                "senderVatOptionsUpdateRequests[0].newEnabledVatOptions[0]"
            )
        )
            .map(triple -> Arguments.of(
                triple.getLeft(),
                new SenderVatOptionsUpdateRequestDtoListWrapper(triple.getMiddle()),
                triple.getRight()
            ));
    }

    @Nonnull
    private ResultActions getTaxSettings() throws Exception {
        return mockMvc.perform(
            get("/back-office/settings/shop/vat")
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions setTaxSettings(SenderVatOptionsUpdateRequestDtoListWrapper request) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, "/back-office/settings/shop/vat", request)
            .param("userId", "1")
            .param("shopId", "1")
        );
    }

    @Nonnull
    SenderVatOptionsUpdateRequestDtoListWrapper defaultUpdateRequest() {
        return new SenderVatOptionsUpdateRequestDtoListWrapper(
            List.of(
                new SenderVatOptionsUpdateRequestDto(1L, List.of(VatType.VAT_10, VatType.VAT_20)),
                new SenderVatOptionsUpdateRequestDto(2L, List.of(VatType.VAT_0, VatType.VAT_20))
            )
        );
    }

    @Nonnull
    SenderVatOptionsUpdateRequestDtoListWrapper defaultCreateRequest() {
        return new SenderVatOptionsUpdateRequestDtoListWrapper(
            List.of(
                new SenderVatOptionsUpdateRequestDto(1L, List.of(VatType.VAT_10, VatType.VAT_20))
            )
        );
    }
}
