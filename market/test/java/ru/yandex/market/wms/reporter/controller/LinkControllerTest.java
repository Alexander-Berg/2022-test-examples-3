package ru.yandex.market.wms.reporter.controller;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.reporter.config.ReporterTestConfig;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        ReporterTestConfig.class
})
@ActiveProfiles(Profiles.TEST)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"reporterConnection"})
class LinkControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Test
    @DatabaseSetup(value = "/report-link/before.xml")
    @ExpectedDatabase(value = "/report-link/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void createReportLink() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileContentUtils.getFileContent("report-link/request.json")))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DatabaseSetup(value = "/report-link/before.xml")
    @ExpectedDatabase(value = "/report-link/before.xml", assertionMode = NON_STRICT_UNORDERED)
    void getReportLinksByKey() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/links/ORDERKEY/123456")
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        JsonAssertUtils.assertFileEquals("report-link/response-1.json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup(value = "/report-link/before.xml")
    @ExpectedDatabase(value = "/report-link/before.xml", assertionMode = NON_STRICT_UNORDERED)
    void getReportLinksByKeys() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/links/ORDERKEY")
                        .queryParam("keys", "111444", "999000")
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        JsonAssertUtils.assertFileEquals("report-link/response-2.json",
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.LENIENT);
    }
}
