package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

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
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.downloadTemplate;

@DisplayName("Скачивание шаблона расписания работы конечной точки")
class AdminDeliveryIntervalFileControllerDownloadTemplateTest extends AbstractContextualTest {

    @Test
    @DisplayName("Скачивание шаблона csv-файла расписания работы конечной точки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void downloadTemplateSuccess() throws Exception {
        mockMvc.perform(downloadTemplate())
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=delivery-schedule-day-template.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(
                TestUtil.fileContent("data/controller/admin/scheduleDay/response/delivery-schedule-day-template.csv")
            );
    }
}
