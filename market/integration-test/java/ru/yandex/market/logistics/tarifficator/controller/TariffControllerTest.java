package ru.yandex.market.logistics.tarifficator.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;
import ru.yandex.market.logistics.tarifficator.util.ValidationUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест контроллера TariffController")
class TariffControllerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Получение тарифа по идентификатору")
    @DatabaseSetup("/controller/tariffs/db/before/tariffs.xml")
    void getTariff() throws Exception {
        mockMvc.perform(
            get("/tariffs/2")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent("controller/tariffs/response/get_tariff.json")));
    }

    @Test
    @DisplayName("Получение несуществующего тарифа")
    void getTariffNotExists() throws Exception {
        mockMvc.perform(
            get("/tariffs/2")
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json(
                extractFileContent("controller/tariffs/response/tariff_not_exists.json")));
    }

    @Test
    @DisplayName("Создание нового тарифа общего характера")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/create_tariff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createTariffGeneral() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff.json"))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание нового тарифа собственной доставки")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/create_tariff_own_delivery.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createTariffOwnDelivery() throws Exception {
        mockLmsSearchPartner(PartnerType.OWN_DELIVERY);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff_own_delivery.json"))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание нового тарифа с нарушением ограничения уникальности")
    @DatabaseSetup("/controller/tariffs/db/after/create_tariff.xml")
    void createTariffWithUniqueConstraintViolation() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Tariff with name = 'Почта России' already exists for partner with id = 300"));
    }

    @Test
    @DisplayName("Создание нового минимального тарифа")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/create_tariff_with_required_only_fields.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createTariffWithRequiredOnlyFields() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    extractFileContent("controller/tariffs/request/create_tariff_with_required_only_fields.json"))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание нового тарифа с несуществующим партнёром")
    void createTariffWithNotExistingPartner() throws Exception {
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff.json"))
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [[300]]"));
    }

    @Test
    @DisplayName("Создание нового тарифа с неподходящим партнёром")
    void createTariffWithIncompatiblePartner() throws Exception {
        mockLmsSearchPartner(PartnerType.SORTING_CENTER);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(ValidationUtil.errorMessage("Partner type mismatch: tariff type 'GENERAL'"
                + " is not compatible with partner type 'SORTING_CENTER' (id = 300)"));
    }

    @Test
    @DisplayName("Создание нового тарифа без указания валюты")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/create_tariff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createTariffWithoutCurrency() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff_without_currency.json"))
        )
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск тарифов")
    @DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
    void search(
        @SuppressWarnings("unused") String displayName,
        TariffSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new TariffSearchFilter(),
                "controller/tariffs/response/all.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору",
                new TariffSearchFilter().setTariffId(1L),
                "controller/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по набору идентификаторов",
                new TariffSearchFilter().setTariffIds(Set.of(1L, 4L)),
                "controller/tariffs/response/id_1_4.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору партнёра",
                new TariffSearchFilter().setPartnerIds(Set.of(1L)),
                "controller/tariffs/response/id_1_4.json"
            ),
            Arguments.of(
                "Фильтр по способу доставки",
                new TariffSearchFilter().setDeliveryMethod(DeliveryMethod.POST),
                "controller/tariffs/response/id_2_3.json"
            ),
            Arguments.of(
                "Фильтр по типу тарифа",
                new TariffSearchFilter().setType(TariffType.GENERAL),
                "controller/tariffs/response/id_1_2.json"
            ),
            Arguments.of(
                "Фильтр включенных тарифов",
                new TariffSearchFilter().setEnabled(true),
                "controller/tariffs/response/id_1_2_3.json"
            ),
            Arguments.of(
                "Фильтр по названию тарифа",
                new TariffSearchFilter().setName("Первый"),
                "controller/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                new TariffSearchFilter()
                    .setPartnerIds(Set.of(1L))
                    .setTariffId(1L)
                    .setDeliveryMethod(DeliveryMethod.PICKUP)
                    .setName("Первый")
                    .setType(TariffType.GENERAL),
                "controller/tariffs/response/id_1.json"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("searchFilterValidationSource")
    @DisplayName("Валидация фильтра поиска тарифов")
    void searchFilterValidation(
        String field,
        String message,
        TariffSearchFilter filter
    ) throws Exception {
        mockMvc.perform(
            put("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(String.format(
                "Following validation errors occurred:\nField: '%s', message: '%s'",
                field,
                message
            )));
    }

    @Nonnull
    private static Stream<Arguments> searchFilterValidationSource() {
        return Stream.of(
            Arguments.of(
                "tariffIds[]",
                "must not be null",
                new TariffSearchFilter().setTariffIds(Collections.singleton(null))
            ),
            Arguments.of(
                "partnerIds[]",
                "must not be null",
                new TariffSearchFilter().setPartnerIds(Collections.singleton(null))
            )
        );
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Попытка создания невалидного тарифа")
    @MethodSource("invalidTariffProvider")
    @DatabaseSetup("/controller/tariffs/db/before/tariffs.xml")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/before/tariffs.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createInvalidTariff(
        @SuppressWarnings("unused") String caseName,
        String requestFilePath,
        String errorMessage
    ) throws Exception {
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    extractFileContent(requestFilePath))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(errorMessage));
    }

    @Test
    @DisplayName("Обновление тарифа")
    @DatabaseSetup("/controller/tariffs/db/before/tariffs.xml")
    @ExpectedDatabase(
        value = "/controller/tariffs/db/after/update_tariff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateTariff() throws Exception {
        mockMvc.perform(
            put("/tariffs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/update_tariff.json"))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Попытка обновления несуществующего тарифа")
    void updateTariffNotExists() throws Exception {
        mockMvc.perform(
            put("/tariffs/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/update_tariff.json"))
        )
            .andExpect(status().isNotFound());
    }

    private static Stream<Arguments> invalidTariffProvider() {
        return Stream.of(
            Triple.of(
                "Тариф без имени",
                "controller/tariffs/request/not_valid_tariff_without_name.json",
                "Following validation errors occurred:\nField: 'name', message: 'must not be blank'"
            ),
            Triple.of(
                "Тариф с пустым именем",
                "controller/tariffs/request/not_valid_tariff_with_empty_name.json",
                "Following validation errors occurred:\nField: 'name', message: 'must not be blank'"
            ),
            Triple.of(
                "Тариф без метода доставки",
                "controller/tariffs/request/not_valid_tariff_without_delivery_method.json",
                "Following validation errors occurred:\nField: 'deliveryMethod', message: 'must not be null'"
            ),
            Triple.of(
                "Тариф без идентификатора партнёра",
                "controller/tariffs/request/not_valid_tariff_without_parnter_id.json",
                "Following validation errors occurred:\nField: 'partnerId', message: 'must not be null'"
            ),
            Triple.of(
                "Тариф с неправильной валютой (больше 3х символов)",
                "controller/tariffs/request/not_valid_tariff_with_invalid_currency_1.json",
                "Following validation errors occurred:\n" +
                    "Field: 'currency', message: 'must match \"[A-Z]{3}\"'"
            ),
            Triple.of(
                "Тариф с неправильной валютой (меньше 3х символов)",
                "controller/tariffs/request/not_valid_tariff_with_invalid_currency_2.json",
                "Following validation errors occurred:\n" +
                    "Field: 'currency', message: 'must match \"[A-Z]{3}\"'"
            ),
            Triple.of(
                "Тариф с неправильной валютой (недопустимые символы)",
                "controller/tariffs/request/not_valid_tariff_with_invalid_currency_3.json",
                "Following validation errors occurred:\n" +
                    "Field: 'currency', message: 'must match \"[A-Z]{3}\"'"
            ),
            Triple.of(
                "Тариф без типа",
                "controller/tariffs/request/not_valid_tariff_with_type.json",
                "Following validation errors occurred:\nField: 'type', message: 'must not be null'"
            )
        ).map(triple -> Arguments.of(triple.first, triple.second, triple.third));
    }

    private void mockLmsSearchPartner(PartnerType partnerType) {
        doReturn(List.of(
            PartnerResponse.newBuilder()
                .id(300)
                .partnerType(partnerType)
                .build()
        ))
            .when(lmsClient)
            .searchPartners(eq(
                SearchPartnerFilter.builder()
                    .setIds(Set.of(300L))
                    .build()
            ));
    }
}
