package ru.yandex.market.logistics.management.controller.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.legalInfo.LegalInfoNewDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@DisplayName("Работа с юридической информацией через панель администратора")
class AdminLegalInfoControllerTest extends AbstractContextualTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchLegalInfoData")
    @DisplayName("Страница со списком юридической информации")
    @DatabaseSetup("/data/controller/admin/legalInfo/prepare_data.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LEGAL_INFO_EDIT})
    void getLegalInfoGrid(
        String displayName,
        MultiValueMap<String, String> params,
        String responsePath,
        String metaResponsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/legal-info")
            .params(params))
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
        mockMvc.perform(get("/admin/lms/legal-info/meta")
            .params(params))
            .andExpect(status().isOk())
            .andExpect(testJson(metaResponsePath));
    }

    private static Stream<Arguments> searchLegalInfoData() {
        return Stream.of(
            Arguments.of(
                "Получить всю юридическую информацию",
                new LinkedMultiValueMap<>(),
                "data/controller/admin/legalInfo/response/get_all_legal_infos.json",
                "data/controller/admin/legalInfo/response/meta/get_all_legal_infos.json"
            ),
            Arguments.of(
                "Найти юридическую информацию по ИНН",
                new LinkedMultiValueMap<>(Map.of(
                    "inn", List.of("010203")
                )),
                "data/controller/admin/legalInfo/response/get_legal_info_by_inn.json",
                "data/controller/admin/legalInfo/response/meta/get_legal_info_by_inn.json"
            ),
            Arguments.of(
                "Найти юридическую информацию по ОГРН",
                new LinkedMultiValueMap<>(Map.of(
                    "ogrn", List.of("777555")
                )),
                "data/controller/admin/legalInfo/response/get_legal_info_by_ogrn.json",
                "data/controller/admin/legalInfo/response/meta/get_legal_info_by_ogrn.json"
            ),
            Arguments.of(
                "Найти юридическую информацию по наименованию организации",
                new LinkedMultiValueMap<>(Map.of(
                    "nameSearchQuery", List.of("Kopyta")
                )),
                "data/controller/admin/legalInfo/response/get_legal_info_by_name.json",
                "data/controller/admin/legalInfo/response/meta/get_legal_info_by_name.json"
            )
        );
    }

    @Test
    @DisplayName("Страница просмотра юридической информации")
    @DatabaseSetup("/data/controller/admin/legalInfo/prepare_data.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LEGAL_INFO_EDIT})
    void getLegalInfoDetail() throws Exception {
        mockMvc.perform(get("/admin/lms/legal-info/101"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/legalInfo/response/get_legal_info_detail.json"));
    }

    @Test
    @DisplayName("Страница создания юридической информации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LEGAL_INFO_EDIT})
    void getNewLegalInfoForm() throws Exception {
        mockMvc.perform(get("/admin/lms/legal-info/new"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/legalInfo/response/new_legal_info_form.json"));
    }

    @Test
    @DisplayName("Создание новой юридической информации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LEGAL_INFO_EDIT})
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/legalInfo/created_legal_info.xml"
    )
    void createNewLegalInfo() throws Exception {
        mockMvc.perform(post("/admin/lms/legal-info")
            .content(pathToJson("data/controller/admin/legalInfo/request/create_legal_info.json"))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", endsWith("/admin/lms/legal-info/1")));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("createInvalidLegalInfoData")
    @DisplayName("Попытка создать невалидную юридичеую информацию")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LEGAL_INFO_EDIT})
    void createInvalidLegalInfo(
        String displayName,
        String requestJson,
        String messageParameter
    ) throws Exception {
        mockMvc.perform(post("/admin/lms/legal-info")
            .content(requestJson)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(
                json()
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .when(Option.IGNORING_EXTRA_ARRAY_ITEMS)
                    .when(Option.IGNORING_ARRAY_ORDER)
                    .isEqualTo(String.format(
                        pathToJson("data/controller/admin/legalInfo/response/invalid_response_template.json"),
                        messageParameter
                    ))
            );
    }

    private static Stream<Arguments> createInvalidLegalInfoData() throws IOException {
        return Stream.of(
            Arguments.of(
                "Не заполнено наименование организации",
                prepareCreateRequest(dto -> dto.setIncorporation(null)),
                "incorporation"
            ),
            Arguments.of(
                "Не заполнена форма собственности",
                prepareCreateRequest(dto -> dto.setLegalForm(null)),
                "legalForm"
            ),
            Arguments.of(
                "Не заполнен ИНН",
                prepareCreateRequest(dto -> dto.setInn(null)),
                "inn"
            ),
            Arguments.of(
                "Не заполнен ОГРН",
                prepareCreateRequest(dto -> dto.setOgrn(null)),
                "ogrn"
            ),
            Arguments.of(
                "Не заполнен телефон",
                prepareCreateRequest(dto -> dto.setPhone(null)),
                "phone"
            ),
            Arguments.of(
                "Не заполнен БИК",
                prepareCreateRequest(dto -> dto.setBik(null)),
                "bik"
            ),
            Arguments.of(
                "Не заполнен банковский аккаунт",
                prepareCreateRequest(dto -> dto.setAccount(null)),
                "account"
            ),
            Arguments.of(
                "Не заполнен адрес электронной почты",
                prepareCreateRequest(dto -> dto.setEmail(null)),
                "email"
            ),
            Arguments.of(
                "Не заполнен почтовый индекс юридического адреса",
                prepareCreateRequest(dto -> dto.getLegalAddress().setPostCode(null)),
                "legalAddress.postCode"
            ),
            Arguments.of(
                "Не заполнен почтовый индекс почтового адреса",
                prepareCreateRequest(dto -> dto.getPostAddress().setPostCode(null)),
                "postAddress.postCode"
            )
        );
    }

    private static String prepareCreateRequest(Consumer<LegalInfoNewDto> dtoConsumer) throws IOException {
        LegalInfoNewDto dto = MAPPER.readValue(
            pathToJson("data/controller/admin/legalInfo/request/create_legal_info.json"),
            LegalInfoNewDto.class
        );
        dtoConsumer.accept(dto);
        return MAPPER.writeValueAsString(dto);
    }
}
