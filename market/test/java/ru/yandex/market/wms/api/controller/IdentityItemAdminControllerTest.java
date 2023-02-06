package ru.yandex.market.wms.api.controller;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class)
@ActiveProfiles(Profiles.TEST)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"wmwhseConnection"})
@Transactional
class IdentityItemAdminControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;


    @Test
    @DatabaseSetup("/identity-admin/get/before.xml")
    @ExpectedDatabase(value = "/identity-admin/get/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getInfoBySerial() throws Exception {
        assertApiCall(200,
                null,
                "get/response.json",
                get("/item/getItemInfoBySerialNumber/3455505393"));
    }

    @Test
    @DatabaseSetup("/identity-admin/errors/before.xml")
    public void noSuchSerial() throws Exception {
        assertApiCall(404,
                null,
                "errors/no-such-serial-response.json",
                get("/item/getItemInfoBySerialNumber/NO_SUCH_SERIAL"));
    }

    @Test
    @DatabaseSetup("/identity-admin/errors/before.xml")
    public void noIdentityInfoProvided() throws Exception {
        assertApiCall(200,
                "errors/identity-info-not-provided-request.json",
                null,
                patch("/item/updateItemInfo"));
    }

    @Test
    @DatabaseSetup("/identity-admin/update/before.xml")
    @ExpectedDatabase(value = "/identity-admin/update/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void newIdentityInfoShouldBeWritten() throws Exception {
        assertApiCall(200,
                "update/request.json",
                null,
                patch("/item/updateItemInfo"));
    }

    @Test
    @DatabaseSetup("/identity-admin/new-identity-info/before.xml")
    @ExpectedDatabase(value = "/identity-admin/new-identity-info/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void newIdentityInfoShouldBeWrittenIfNoIdentityInfoWasStored() throws Exception {
        assertApiCall(200,
                "new-identity-info/request.json",
                null,
                patch("/item/updateItemInfo"));
    }

    @Test
    @DatabaseSetup("/identity-admin/errors/before.xml")
    @ExpectedDatabase(value = "/identity-admin/errors/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void badIdentityValueWhileUpdateDBStateShouldNotChange() throws Exception {
        assertApiCall(400,
                "errors/attempt-to-update-with-bad-value-request.json",
                "errors/attempt-to-update-with-bad-value-response.json",
                patch("/item/updateItemInfo"));
    }

    private void assertApiCall(int expectedStatus, String requestFile, String responseFile,
                               MockHttpServletRequestBuilder request) throws Exception {
        String path = "identity-admin/";
        MockHttpServletRequestBuilder requestBuilder = request.cookie(
                new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON);
        if (requestFile != null) {
            requestBuilder.content(getFileContent(path + requestFile));
        }
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
                .andExpect(status().is(expectedStatus))
                .andReturn();

        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(path + responseFile,
                    mvcResult.getResponse().getContentAsString());
        }
    }
}
