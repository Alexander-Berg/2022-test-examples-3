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

@DisplayName("???????????????????????????? ???????? ?????????????????????? TariffController")
class TariffControllerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("?????????????????? ???????????? ???? ????????????????????????????")
    @DatabaseSetup("/controller/tariffs/db/before/tariffs.xml")
    void getTariff() throws Exception {
        mockMvc.perform(
            get("/tariffs/2")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent("controller/tariffs/response/get_tariff.json")));
    }

    @Test
    @DisplayName("?????????????????? ?????????????????????????????? ????????????")
    void getTariffNotExists() throws Exception {
        mockMvc.perform(
            get("/tariffs/2")
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json(
                extractFileContent("controller/tariffs/response/tariff_not_exists.json")));
    }

    @Test
    @DisplayName("???????????????? ???????????? ???????????? ???????????? ??????????????????")
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
    @DisplayName("???????????????? ???????????? ???????????? ?????????????????????? ????????????????")
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
    @DisplayName("???????????????? ???????????? ???????????? ?? ???????????????????? ?????????????????????? ????????????????????????")
    @DatabaseSetup("/controller/tariffs/db/after/create_tariff.xml")
    void createTariffWithUniqueConstraintViolation() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        mockMvc.perform(
            post("/tariffs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tariffs/request/create_tariff.json"))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Tariff with name = '?????????? ????????????' already exists for partner with id = 300"));
    }

    @Test
    @DisplayName("???????????????? ???????????? ???????????????????????? ????????????")
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
    @DisplayName("???????????????? ???????????? ???????????? ?? ???????????????????????????? ??????????????????")
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
    @DisplayName("???????????????? ???????????? ???????????? ?? ???????????????????????? ??????????????????")
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
    @DisplayName("???????????????? ???????????? ???????????? ?????? ???????????????? ????????????")
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
    @DisplayName("?????????? ??????????????")
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
                "???????????? ????????????",
                new TariffSearchFilter(),
                "controller/tariffs/response/all.json"
            ),
            Arguments.of(
                "???????????? ???? ????????????????????????????",
                new TariffSearchFilter().setTariffId(1L),
                "controller/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "???????????? ???? ???????????? ??????????????????????????????",
                new TariffSearchFilter().setTariffIds(Set.of(1L, 4L)),
                "controller/tariffs/response/id_1_4.json"
            ),
            Arguments.of(
                "???????????? ???? ???????????????????????????? ????????????????",
                new TariffSearchFilter().setPartnerIds(Set.of(1L)),
                "controller/tariffs/response/id_1_4.json"
            ),
            Arguments.of(
                "???????????? ???? ?????????????? ????????????????",
                new TariffSearchFilter().setDeliveryMethod(DeliveryMethod.POST),
                "controller/tariffs/response/id_2_3.json"
            ),
            Arguments.of(
                "???????????? ???? ???????? ????????????",
                new TariffSearchFilter().setType(TariffType.GENERAL),
                "controller/tariffs/response/id_1_2.json"
            ),
            Arguments.of(
                "???????????? ???????????????????? ??????????????",
                new TariffSearchFilter().setEnabled(true),
                "controller/tariffs/response/id_1_2_3.json"
            ),
            Arguments.of(
                "???????????? ???? ???????????????? ????????????",
                new TariffSearchFilter().setName("????????????"),
                "controller/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "???????????? ???? ???????? ????????????????????",
                new TariffSearchFilter()
                    .setPartnerIds(Set.of(1L))
                    .setTariffId(1L)
                    .setDeliveryMethod(DeliveryMethod.PICKUP)
                    .setName("????????????")
                    .setType(TariffType.GENERAL),
                "controller/tariffs/response/id_1.json"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("searchFilterValidationSource")
    @DisplayName("?????????????????? ?????????????? ???????????? ??????????????")
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
    @DisplayName("?????????????? ???????????????? ?????????????????????? ????????????")
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
    @DisplayName("???????????????????? ????????????")
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
    @DisplayName("?????????????? ???????????????????? ?????????????????????????????? ????????????")
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
                "?????????? ?????? ??????????",
                "controller/tariffs/request/not_valid_tariff_without_name.json",
                "Following validation errors occurred:\nField: 'name', message: 'must not be blank'"
            ),
            Triple.of(
                "?????????? ?? ???????????? ????????????",
                "controller/tariffs/request/not_valid_tariff_with_empty_name.json",
                "Following validation errors occurred:\nField: 'name', message: 'must not be blank'"
            ),
            Triple.of(
                "?????????? ?????? ???????????? ????????????????",
                "controller/tariffs/request/not_valid_tariff_without_delivery_method.json",
                "Following validation errors occurred:\nField: 'deliveryMethod', message: 'must not be null'"
            ),
            Triple.of(
                "?????????? ?????? ???????????????????????????? ????????????????",
                "controller/tariffs/request/not_valid_tariff_without_parnter_id.json",
                "Following validation errors occurred:\nField: 'partnerId', message: 'must not be null'"
            ),
            Triple.of(
                "?????????? ?? ???????????????????????? ?????????????? (???????????? 3?? ????????????????)",
                "controller/tariffs/request/not_valid_tariff_with_invalid_currency_1.json",
                "Following validation errors occurred:\n" +
                    "Field: 'currency', message: 'must match \"[A-Z]{3}\"'"
            ),
            Triple.of(
                "?????????? ?? ???????????????????????? ?????????????? (???????????? 3?? ????????????????)",
                "controller/tariffs/request/not_valid_tariff_with_invalid_currency_2.json",
                "Following validation errors occurred:\n" +
                    "Field: 'currency', message: 'must match \"[A-Z]{3}\"'"
            ),
            Triple.of(
                "?????????? ?? ???????????????????????? ?????????????? (???????????????????????? ??????????????)",
                "controller/tariffs/request/not_valid_tariff_with_invalid_currency_3.json",
                "Following validation errors occurred:\n" +
                    "Field: 'currency', message: 'must match \"[A-Z]{3}\"'"
            ),
            Triple.of(
                "?????????? ?????? ????????",
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
