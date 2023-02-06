package ru.yandex.market.hrms.api.controller.staffscuser;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.employee.repo.ScUserChytRepo;
import ru.yandex.market.hrms.core.domain.employee.repo.StaffScUserDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StaffScUserControllerTest extends AbstractApiTest {

    @MockBean
    ScUserChytRepo scUserRepo;

    @Test
    @DbUnitDataSet(before = "StaffScUserController.before.csv")
    @DbUnitDataSet(after = "StaffScUserController.after.csv")
    void importStaffLoginsFromScYtTest() throws Exception {

        Mockito.when(scUserRepo.importData()).thenReturn(List.of(
                new StaffScUserDto(11L, "Петрович", "email1", "login1111", 12L),
                new StaffScUserDto(22L, "Иваныч", "email2", "login2", 21L),
                new StaffScUserDto(33L, "Копатыч", "email3", "login3", 21L)
                )
        );

        mockMvc.perform(get("/manual/import-sc-staff-users"))
                .andExpect(status().isOk());
    }
}
