package ru.yandex.market.logistics.management.controller.admin.partnerTransport;

import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.uploadAdd;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.uploadReplace;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/data/controller/admin/partnerTransport/before/setup.xml")
public class AdminPartnerTransportationControllerFileTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешно скачать транспорты")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void downloadCsv() throws Exception {
        mockMvc.perform(Helper.download())
            .andExpect(status().isOk())
            .andExpect(content().string(
                extractFileContent("data/controller/admin/partnerTransport/after/download.csv")
            ));
    }

    @Test
    @DisplayName("Успешно скачать шаблон")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void downloadCsvTemplate() throws Exception {
        mockMvc.perform(Helper.downloadTemplate())
            .andExpect(status().isOk())
            .andExpect(content().string(
                extractFileContent("data/controller/admin/partnerTransport/after/template.csv")
            ));
    }

    @Test
    @DisplayName("Успешно заменить все")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerTransport/after/upload_replace_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceAllSuccess() throws Exception {
        doUpload("data/controller/admin/partnerTransport/before/upload_replace_all_success.csv", Map.of())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузить невалидные данные")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerTransport/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceViolateValidation() throws Exception {
        doUpload("data/controller/admin/partnerTransport/before/upload_replace_violate_validation.csv", Map.of())
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Не удалось заменить транспорты (400 BAD_REQUEST \"Обнаружены следующие " +
                "ошибки:\n" +
                "Строка: 2, Поле: partner, Сообщение: Linehaul partner must be of DELIVERY type\")"));
    }

    @Test
    @DisplayName("Успешно заменить с фильтром")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerTransport/after/upload_replace_by_filter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByPartnerIdSuccess() throws Exception {
        doUpload(
            "data/controller/admin/partnerTransport/before/upload_replace_by_filter_success.csv",
            Map.of("partner", "1")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно добавить без замены")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerTransport/after/upload_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvSuccess() throws Exception {
        mockMvc.perform(uploadAdd().file(Helper.file(TestUtil.pathToJson(
            "data/controller/admin/partnerTransport/before/upload_add_success.csv"
        ))))
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions doUpload(String pathToFile, Map<String, String> queryParams) throws Exception {
        return mockMvc.perform(
            uploadReplace().file(Helper.file(TestUtil.pathToJson(pathToFile)))
                .params(TestUtil.toMultiValueMap(queryParams))
        );

    }
}
