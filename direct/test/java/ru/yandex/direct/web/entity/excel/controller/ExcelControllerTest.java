package ru.yandex.direct.web.entity.excel.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.excel.ExcelTestUtils;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportMode;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.web.entity.excel.controller.ExcelController.EXCEL_FILE_PARAM;
import static ru.yandex.direct.web.entity.excel.controller.ExcelController.EXCEL_MAPPER_READ_EXCEPTION_TO_CODE;
import static ru.yandex.direct.web.entity.excel.controller.ExcelController.IMPORT_MODE_PARAM;
import static ru.yandex.direct.web.entity.excel.controller.ExcelController.INTERNAL_AD_UPLOAD_EXCEL_FILE_FOR_IMPORT_AND_GET_DATA_CONTROLLER;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class ExcelControllerTest {

    private static final String UPLOAD_CONTROLLER_PATH =
            "/excel" + INTERNAL_AD_UPLOAD_EXCEL_FILE_FOR_IMPORT_AND_GET_DATA_CONTROLLER;
    private static final String FILE_WITH_INVALID_FORMAT = "excel/file_with_invalid_format.xlsx";

    @Autowired
    private ExcelController controller;

    @Autowired
    private MockMvcCreator mockMvcCreator;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private DirectWebAuthenticationSource directWebAuthenticationSource;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder requestBuilder;

    @Before
    public void initTestData() {
        mockMvc = mockMvcCreator.setup(controller).build();

        UserInfo user = steps.userSteps().createDefaultUserWithRole(RbacRole.INTERNAL_AD_ADMIN);
        testAuthHelper.setOperatorAndSubjectUser(user.getUid());
        TestAuthHelper.setSecurityContextWithAuthentication(directWebAuthenticationSource.getAuthentication());

        MockMultipartFile multipartFile = ExcelTestUtils.createMockExcelFile(EXCEL_FILE_PARAM,
                ClassLoader.getSystemResourceAsStream(FILE_WITH_INVALID_FORMAT));

        requestBuilder = MockMvcRequestBuilders.multipart(UPLOAD_CONTROLLER_PATH)
                .file(multipartFile)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(IMPORT_MODE_PARAM, InternalAdImportMode.ONLY_AD_GROUPS.name());
    }


    @Test
    public void excelMapperReadExceptionHandlerTest() throws Exception {
        String responseContent = mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var response = JsonUtils.fromJson(responseContent, Map.class);
        var webDefect = new WebDefect()
                .withCode(EXCEL_MAPPER_READ_EXCEPTION_TO_CODE.get(CantReadFormatException.class))
                .withPath("B1")
                .withParams(List.of("Кампания"));
        var expectedResponse = new ValidationResponse(new WebValidationResult().addErrors(List.of(webDefect)));

        //noinspection unchecked
        assertThat(response)
                .is(matchedBy(beanDiffer(JsonUtils.getObjectMapper().convertValue(expectedResponse, Map.class))));
    }

}
