package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.multipartFile;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.uploadAdd;

@DisplayName("Загрузка файла и создание расписания работы конечных точек")
@DatabaseSetup("/data/controller/admin/scheduleDay/before/prepare_data.xml")
public class AdminDeliveryIntervalFileControllerUploadAddTest extends AbstractContextualTest {

    @Test
    @DisplayName("Загрузка файла и создание расписания работы конечной точки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/scheduleDay/after/upload_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvAddSuccess() throws Exception {
        mockMvc.perform(uploadAdd()
            .file(multipartFile(
                TestUtil.pathToJson("data/controller/admin/scheduleDay/request/upload_add_success.csv"))))
            .andExpect(status().isOk());
    }

}
