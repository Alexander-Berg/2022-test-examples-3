package ru.yandex.market.wms.reporter.controller;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.reporter.config.ReporterTestConfig;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = ReporterTestConfig.class)
@ActiveProfiles(Profiles.TEST)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@AutoConfigureMockMvc
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"scprdConnection"})
public class PrinterControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Test
    @DatabaseSetup(value = "/printer/before.xml", connection = "scprdConnection")
    public void listPrintersTest() throws Exception {
        mockMvc.perform(get("/printer"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent(
                                "printer/printers.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/printer/before.xml", connection = "scprdConnection")
    public void getPrinterTest() throws Exception {
        mockMvc.perform(get("/printer/P01"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent(
                                "printer/P01.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/printer/before.xml", connection = "scprdConnection")
    public void getUnknownPrinterTest() throws Exception {
        mockMvc.perform(get("/printer/printer"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();
    }

}
