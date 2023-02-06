package ru.yandex.market.hrms.api.controller.operation;

import java.time.LocalDateTime;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.api.util.OperationMappingSheetContentExtractUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OperationGroupMappingControllerTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "OperationGroupMappingControllerTest.before.csv")
    public void shouldReturnMappingXlsx() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups/download-mapping")
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andDo(result -> {
                    var sheetBytes = result.getResponse().getContentAsByteArray();
                    var file = new MockMultipartFile(
                            "file",
                            "excel.xls",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            sheetBytes);

                    var content = OperationMappingSheetContentExtractUtil
                            .extract(file);

                    assertThat(content, is(Map.of(
                            "погрузка2", "общая погрузка",
                            "погрузка3", "общая погрузка",
                            "погрузка4", "общая погрузка",
                            "погрузка5", "",
                            "погрузка1", "общая погрузка"
                            )
                    ));
                });
    }

    @Test
    @DbUnitDataSet(before = "OperationGroupMappingControllerTest.before.csv",
                   after = "OperationGroupMappingControllerTest.after.csv")
    public void shouldUpdateMappings() throws Exception{
        mockClock(LocalDateTime.of(2021, 2, 1, 12, 0));
        var file = new MockMultipartFile(
                "file",
                "mapping.xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("OperationGroupMappingControllerTest.xls")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/operation-groups/upload-mapping")
                .file(file)
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "OperationGroupMappingControllerTestError.before.csv")
    public void shouldNotUpdate() throws Exception{
        mockClock(LocalDateTime.of(2021, 2, 1, 12, 0));
        var file = new MockMultipartFile(
                "file",
                "mapping.xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("OperationGroupMappingControllerTest.xls")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/operation-groups/uploadmapping")
                .file(file)
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().is4xxClientError());
    }
}
