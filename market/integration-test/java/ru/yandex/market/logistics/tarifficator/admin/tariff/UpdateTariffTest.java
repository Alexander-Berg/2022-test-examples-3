package ru.yandex.market.logistics.tarifficator.admin.tariff;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.TariffDetailDto;
import ru.yandex.market.logistics.tarifficator.admin.dto.TariffGridDto;
import ru.yandex.market.logistics.tarifficator.admin.enums.AdminDeliveryMethod;
import ru.yandex.market.logistics.tarifficator.admin.enums.AdminTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.PlatformClient;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationFrontError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновить информацию о тарифе через админку")
@DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
class UpdateTariffTest extends AbstractContextualTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LMSClient lmsClient;

    @BeforeEach
    public void init() {
        when(lmsClient.getPartner(1L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().id(1L).readableName("partner_1").build()));
    }

    @Test
    @DisplayName("Валидация запроса")
    void requestValidation() throws Exception {
        mockMvc.perform(
                put("/admin/tariffs/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(defaultTariffDetailDto().setCode("")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationFrontError("code", "must not be empty"));
    }

    @Test
    @DisplayName("Обновление тарифа — поля неизменны")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/search_prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffNoUpdatedFields() {
        updateTariff(defaultTariffDetailDto(), "controller/admin/tariffs/response/id_1_details.json");
    }

    @Test
    @DisplayName("Обновление тарифа — выключение")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_enabled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffEnabled() {
        updateTariff(
            defaultTariffDetailDto().setEnabled(false),
            "controller/admin/tariffs/response/id_1_details_enabled_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — изменение названия")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_name.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffName() {
        updateTariff(
            defaultTariffDetailDto().setName("Новое название"),
            "controller/admin/tariffs/response/id_1_details_name_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — изменение описания")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_description.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffDescription() {
        updateTariff(
            defaultTariffDetailDto().setDescription("Новое описание"),
            "controller/admin/tariffs/response/id_1_details_description_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — изменение кода")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffCode() {
        updateTariff(
            defaultTariffDetailDto().setCode("Новый код"),
            "controller/admin/tariffs/response/id_1_details_code_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — изменение флага равнества себестоимости и стоимости")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_equal_prices_flag.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffEqualPricesFlag() {
        updateTariff(
            defaultTariffDetailDto().setEqualPublicAndNonpublicPrices(true),
            "controller/admin/tariffs/response/id_1_details_equal_prices_flag_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — изменение всех возможных полей")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_all_possible_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateAllPossibleFields() {
        updateTariff(
            defaultTariffDetailDto()
                .setCode("Новый код")
                .setEnabled(false)
                .setName("Новое название")
                .setDescription("Новое описание")
                .setEqualPublicAndNonpublicPrices(true),
            "controller/admin/tariffs/response/id_1_details_all_possible_fields_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — тариф не найден")
    void updateTariffNotFound() throws Exception {
        mockMvc.perform(
                put("/admin/tariffs/5")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(defaultTariffDetailDto()))
            )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [[5]]"));
    }

    @Test
    @DisplayName("Обновление тарифа платформы")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_enabled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffEnabledWithPlatform() {
        updateTariff(
            "/admin/platform/tariffs/1",
            defaultTariffDetailDto().setEnabled(false),
            "controller/admin/tariffs/response/id_1_details_enabled_changed.json",
            Set.of(PlatformClient.YANDEX_DELIVERY)
        );
    }

    @Test
    @DisplayName("Обновление тарифа не относящегося к какой-либо платформе")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_enabled_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffEnabledWithoutPlatform() {
        updateTariff(
            "/admin/platform/tariffs/2",
            tariffDetailDto2().setEnabled(false),
            "controller/admin/tariffs/response/id_2_details_enabled_changed.json",
            Set.of()
        );
    }

    @Test
    @DisplayName("Отказано в доступе при обновлении тарифа платформы")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/search_prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffWithPlatformForbiddenError() throws Exception {
        mockMvc.perform(
                put("/admin/platform/tariffs/1")
                    .param("platformClients", "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(defaultTariffDetailDto()))
            )
            .andExpect(status().isForbidden())
            .andExpect(errorMessage("Unable to access [TARIFF] with ids [1]"));
    }

    @Test
    @DisplayName("Отказано в доступе при обновлении не принадлежащего платформе тарифа")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/search_prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTariffWithoutPlatformForbiddenError() throws Exception {
        mockMvc.perform(
                put("/admin/platform/tariffs/2?platformClients={platform}", "YANDEX_DELIVERY")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(defaultTariffDetailDto()))
            )
            .andExpect(status().isForbidden())
            .andExpect(errorMessage("Unable to access [TARIFF] with ids [2]"));
    }

    @Test
    @DisplayName("Обновление тарифа — изменение группы тарифов")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_change_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateChangeGroup() {
        updateTariff(
            defaultTariffDetailDto().setTariffGroupId(23L),
            "controller/admin/tariffs/response/id_1_details_group_changed.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — попытка изменения группы тарифов на несуществующую")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/search_prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateFailChangeToNonexistentGroup() throws Exception {
        performUpdateTariff(
            1,
            defaultTariffDetailDto().setTariffGroupId(999L)
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF_GROUP] with ids [[999]]"));
    }

    @Test
    @DisplayName("Обновление тарифа — удаление группы тарифов")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_delete_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDeleteGroup() {
        updateTariff(
            defaultTariffDetailDto().setTariffGroupId(null),
            "controller/admin/tariffs/response/id_1_details_group_deleted.json"
        );
    }

    @Test
    @DisplayName("Обновление тарифа — добавление группы тарифов")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/update_add_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateAddGroup() {
        updateTariff(
            "/admin/platform/tariffs/2",
            tariffDetailDto2().setTariffGroupId(23L),
            "controller/admin/tariffs/response/id_2_details_group_added.json",
            null
        );
    }

    @Test
    @DisplayName("Обновление тарифа — попытка добавления несуществующей группы тарифов")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/search_prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateFailAddNonexistenceGroup() throws Exception {
        performUpdateTariff(
            2,
            tariffDetailDto2().setTariffGroupId(999L)
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF_GROUP] with ids [[999]]"));
    }

    @SneakyThrows
    private void updateTariff(TariffGridDto tariffDetailDto, String responseFileName) {
        updateTariff("/admin/tariffs/1", tariffDetailDto, responseFileName, null);
    }

    @SneakyThrows
    private void updateTariff(
        String url,
        TariffGridDto tariffDetailDto,
        String responseFileName,
        @Nullable Set<PlatformClient> platforms
    ) {
        mockMvc.perform(updateTariffRequestBuilder(url, tariffDetailDto, platforms))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responseFileName));
    }

    @SneakyThrows
    @Nonnull
    private ResultActions performUpdateTariff(long tariffId, TariffGridDto setTariffGroupId) {
        return mockMvc.perform(
                updateTariffRequestBuilder("/admin/tariffs/" + tariffId, setTariffGroupId, null)
            );
    }

    @NotNull
    private MockHttpServletRequestBuilder updateTariffRequestBuilder(
        String url,
        TariffGridDto tariffDetailDto,
        @Nullable Set<PlatformClient> platforms
    ) throws JsonProcessingException {
        MockHttpServletRequestBuilder requestBuilder = put(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(tariffDetailDto));

        if (platforms != null) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.addAll(
                "platformClients",
                platforms.stream()
                    .map(Enum::name)
                    .collect(Collectors.toList())
            );
            requestBuilder.params(params);
        }
        return requestBuilder;
    }

    @Nonnull
    private TariffDetailDto defaultTariffDetailDto() {
        return (TariffDetailDto) new TariffDetailDto()
            .setCode("Code1")
            .setTitle("Тариф № 1")
            .setId(1L)
            .setTariffGroupId(18L)
            .setPartner(new ReferenceObject("1", "partner_1", "lms/partner", true))
            .setDeliveryMethod(AdminDeliveryMethod.PICKUP)
            .setName("Первый тариф")
            .setDescription("Первый тестовый тариф")
            .setTags("DAAS")
            .setType(AdminTariffType.GENERAL)
            .setEnabled(true)
            .setArchived(false)
            .setCreatedAt(Instant.parse("2019-08-01T12:09:31Z"))
            .setEqualPublicAndNonpublicPrices(false);
    }

    @Nonnull
    private TariffDetailDto tariffDetailDto2() {
        return (TariffDetailDto) new TariffDetailDto()
            .setCode("Code2")
            .setTitle("Тариф № 2")
            .setId(2L)
            .setPartner(new ReferenceObject("2", "2: <не найден>", "lms/partner", true))
            .setDeliveryMethod(AdminDeliveryMethod.PICKUP)
            .setName("Второй тариф")
            .setDescription("Второй тестовый тариф")
            .setType(AdminTariffType.GENERAL)
            .setEnabled(true)
            .setArchived(false)
            .setCreatedAt(Instant.parse("2019-08-01T12:09:31Z"))
            .setEqualPublicAndNonpublicPrices(false);
    }
}
