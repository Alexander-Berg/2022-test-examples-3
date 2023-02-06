package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.downloadAll;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Скачивание csv-файла магистралей партнеров")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
class AdminPartnerRouteControllerDownloadAllTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void downloadTemplateSuccess() throws Exception {
        mockMvc.perform(downloadAll())
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partner-routes.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(content().string(
                extractFileContent("data/controller/admin/partnerRoute/after/download.csv")
            ));
    }
}
