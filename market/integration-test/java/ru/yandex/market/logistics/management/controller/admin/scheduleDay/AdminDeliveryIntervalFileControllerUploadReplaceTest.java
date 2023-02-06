package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.uploadReplace;

@DisplayName("Загрузка файла и обновление расписания работы конечных точек")
@DatabaseSetup("/data/controller/admin/scheduleDay/before/prepare_data.xml")
public class AdminDeliveryIntervalFileControllerUploadReplaceTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешно заменить все")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/scheduleDay/after/upload_replace_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceAllSuccess() throws Exception {
        doUpload("data/controller/admin/scheduleDay/request/upload_replace_all_success.csv", Map.of())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по партнеру")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/scheduleDay/after/upload_replace_by_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByPartnerIdSuccess() throws Exception {
        doUpload(
            "data/controller/admin/scheduleDay/request/upload_replace_by_partner_success.csv",
            Map.of("partner", "3000")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по региону")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/scheduleDay/after/upload_replace_by_location.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByLocationFromSuccess() throws Exception {
        doUpload(
            "data/controller/admin/scheduleDay/request/upload_replace_by_location_success.csv",
            Map.of("location", "163")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешно заменить с фильтром по дню недели")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/scheduleDay/after/upload_replace_by_week_day_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceByLocationToSuccess() throws Exception {
        doUpload(
            "data/controller/admin/scheduleDay/request/upload_replace_by_week_day_success.csv",
            Map.of("dayOfWeek", "MONDAY")
        )
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions doUpload(String pathToFile, Map<String, String> queryParams) throws Exception {
        return mockMvc.perform(
            uploadReplace().file(ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper
                .multipartFile(TestUtil.pathToJson(pathToFile)))
                .params(TestUtil.toMultiValueMap(queryParams))
        );
    }

}
