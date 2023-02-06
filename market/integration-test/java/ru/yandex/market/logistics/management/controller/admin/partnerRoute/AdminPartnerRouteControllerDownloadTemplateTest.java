package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.downloadTemplate;

@DisplayName("Скачивание шаблона csv-файла расписания магистралей партнеров")
class AdminPartnerRouteControllerDownloadTemplateTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void downloadTemplateSuccess() throws Exception {
        mockMvc.perform(downloadTemplate())
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partner-routes.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(TestUtil.fileContent("data/controller/admin/partnerRoute/response/partner-routes-template.csv"));
    }
}
