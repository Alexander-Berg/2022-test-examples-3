package ru.yandex.market.ff.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.ff.util.FileContentUtils;

/**
 * @author kotovdv 08/08/2017.
 */
public abstract class MvcIntegrationTest extends IntegrationTest {

    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext wac;

    @BeforeEach
    public void setupMockMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .build();
    }

    protected <T> String toJson(T t) throws JsonProcessingException {
        return objectMapper.writeValueAsString(t);
    }

    protected Workbook getWorkbook(MvcResult mvcResult) throws IOException {
        return WorkbookFactory.create(new ByteArrayInputStream(mvcResult.getResponse().getContentAsByteArray()));
    }

    protected Sheet getSheet(final MvcResult mvcResult) throws IOException {
        return getWorkbook(mvcResult).getSheetAt(0);
    }

    protected void assertResponseEquals(MvcResult result, String path) throws IOException {
        String responseBody = FileContentUtils.getFileContent(path);
        JSONAssert.assertEquals(responseBody, result.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

}
