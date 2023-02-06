package ru.yandex.market.logistics.management.controller.capacity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Скачивание шаблона капасити партнера и загрузка csv-файла")
@DatabaseSetup("/data/controller/admin/partnerCapacity/before/prepare_data.xml")
public class AdminPartnerCapacityFileControllerTest extends AbstractContextualTest {

    private static final String URL = "/admin/lms/partner-capacity";

    @Test
    @DisplayName("Скачивание csv-файла капасити партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void downloadTemplateSuccess() throws Exception {
        mockMvc.perform(get(URL + "/download/template"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partner-capacity-template.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(content().string(TestUtil.pathToJson(
                "data/controller/admin/partnerCapacity/after/download_template.csv")));
    }

    @Test
    @DisplayName("Загрузка файла и создание капасити партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvSuccess() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/add")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/" +
                        "partner_capacity_with_empty_delivery_type_upload_success.csv"
                )))
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Валидация не проходит на мувменте")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @DatabaseSetup(
        value = "/data/controller/admin/partnerCapacity/before/prepare_for_migration.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/nothing_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validationMovementTest() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/add").file(multipartFile(TestUtil.pathToJson(
                "data/controller/admin/partnerCapacity/request/partner_capacity_with_bad_movement_delivery_type.csv"
            ))))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "ERROR: incorrect capacities for partner 4, "
                    + "parent region from 1 has delivery_type pickup, but child with location from 101060 doesnt"
            ));
    }

    @Test
    @DisplayName("Валидация не проходит на лайнхоле")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @DatabaseSetup(
        value = "/data/controller/admin/partnerCapacity/before/prepare_for_migration.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/nothing_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validationLinehaulTest() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/add").file(multipartFile(TestUtil.pathToJson(
                "data/controller/admin/partnerCapacity/request/partner_capacity_with_bad_linehaul_delivery_type.csv"
            ))))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "ERROR: incorrect capacities for partner 4, "
                    + "parent region to 1 has delivery_type pickup, but child with location to 213 doesnt"
            ));
    }

    @Test
    @DisplayName("Валидация не проходит на деливери")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @DatabaseSetup(
        value = "/data/controller/admin/partnerCapacity/before/prepare_for_migration.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/nothing_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validationDeliveryTest() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/add").file(multipartFile(TestUtil.pathToJson(
            "data/controller/admin/partnerCapacity/request/partner_capacity_with_bad_locations_and_capacity_service.csv"
        ))))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "Обнаружены следующие ошибки:\n"
                    + "Строка: 3, Сообщение: Некорректное значение location_from"
                    + " для капасити с типом Доставка. Допустимое значение: 225\n"
                    + "Строка: 5, Сообщение: Некорректное значение location_from"
                    + " для капасити с типом Доставка. Допустимое значение: 225"
            ));
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по партнеру")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByPartner() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?partnerIds=1")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по платформе")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_platform.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByPlatform() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?platformClientIds=2")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по Id локации откуда")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_location_from.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByLocationFrom() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?locationsFrom=225")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по Id локации куда")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_location_to.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByLocationTo() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?locationsTo=213")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по сервису капасити")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_capacity_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByCapacityService() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?capacityServices=SHIPMENT")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по дню")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByDay() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?days=2020-05-03")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по значению")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCapacity/after/upload_replace_by_value.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByValue() throws Exception {
        mockMvc.perform(multipart(URL + "/upload/replace?values=200")
                .file(multipartFile(TestUtil.pathToJson(
                    "data/controller/admin/partnerCapacity/request/partner_capacity_upload_success.csv"
                ))))
            .andExpect(status().isOk());
    }

    @NotNull
    @SneakyThrows
    public static MockMultipartFile multipartFile(String content) {
        return new MockMultipartFile(
            "request",
            "originalFileName.csv",
            MediaTypes.TEXT_CSV_UTF8_VALUE,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
    }

}
