package ru.yandex.market.delivery.partnerapimock.steps;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import ru.yandex.market.delivery.partnerapimock.component.JsonMatcher;
import ru.yandex.market.delivery.partnerapimock.util.XmlMatcher;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MockSteps {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMatcher jsonMatcher;

    private String contextPath;
    private static final String MOCK_URL = "/mock";
    private static final String CHECK_MOCK_URL = MOCK_URL + "/check";
    private static final String DO_NOTHING_URL = CHECK_MOCK_URL + "/doNothing";


    public void createMock(String createMockRequest) throws Exception {
        createMock(createMockRequest, "Добавлена новая запись");
    }

    public void createMock(String createMockRequest, String expectedResponse) throws Exception {
        mvc.perform(post(contextPath + MOCK_URL).contentType(APPLICATION_XML).content(createMockRequest))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(expectedResponse +
                    "\nmockId: ")));
    }

    public void verifyCreateMockFailed(String createMockRequest) throws Exception {
        mvc.perform(post(contextPath + MOCK_URL).contentType(APPLICATION_XML).content(createMockRequest))
            .andExpect(status().isBadRequest());
    }

    public void verifyCheckMockSuccess(String checkMockRequest) throws Exception {
        mvc.perform(post(contextPath + CHECK_MOCK_URL).contentType(APPLICATION_XML).content(checkMockRequest))
            .andExpect(status().isOk());
    }

    public void verifyCheckMockWhenUrlIsSuffixSuccess(String checkMockRequest) throws Exception {
        mvc.perform(post(CHECK_MOCK_URL + contextPath).contentType(APPLICATION_XML).content(checkMockRequest))
            .andExpect(status().isOk());
    }

    public void verifyCheckMockByPutSuccess(String checkMockRequest) throws Exception {
        mvc.perform(put(CHECK_MOCK_URL + contextPath).contentType(APPLICATION_XML).content(checkMockRequest))
            .andExpect(status().isOk());
    }

    public void verifyCheckMockFailed(String checkMockRequest) throws Exception {
        mvc.perform(post(contextPath + CHECK_MOCK_URL).contentType(APPLICATION_XML).content(checkMockRequest))
            .andExpect(status().isNotFound());
    }

    public void verifyCheckMockByPutFailed(String checkMockRequest) throws Exception {
        mvc.perform(put(CHECK_MOCK_URL + contextPath).contentType(APPLICATION_XML).content(checkMockRequest))
            .andExpect(status().isNotFound());
    }

    public void verifyCheckMockResponseForXml(String checkMockRequest, String expectedResponse) throws Exception {
        String response = mvc.perform(post(contextPath + CHECK_MOCK_URL)
            .contentType(APPLICATION_XML).content(checkMockRequest))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString().replaceAll("\\s+", " ").trim();
        Assert.assertTrue("Check result is incorrect",
            XmlMatcher.isXmlsEqual(expectedResponse.replaceAll("\\s+", " ").trim(), response));
    }

    public void verifyCheckMockResponseForJson(String checkMockRequest, String expectedResponse) throws Exception {
        String response = mvc.perform(post(contextPath + CHECK_MOCK_URL)
            .contentType(APPLICATION_JSON).content(checkMockRequest))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString().trim();
        Assert.assertTrue("Check result is incorrect", jsonMatcher.isJsonEquals(expectedResponse, response));
    }

    public void deleteMockForXml(String deleteMockRequest) throws Exception {
        mvc.perform(delete(contextPath + MOCK_URL).contentType(APPLICATION_XML).content(deleteMockRequest))
            .andExpect(status().isOk());
    }

    public void deleteMockForJson(String deleteMockRequest) throws Exception {
        mvc.perform(delete(contextPath + MOCK_URL).contentType(APPLICATION_JSON).content(deleteMockRequest))
            .andExpect(status().isOk());
    }

    public void verifyDeleteMockFailed(String deleteMockRequest) throws Exception {
        mvc.perform(delete(contextPath + MOCK_URL).contentType(APPLICATION_XML).content(deleteMockRequest))
            .andExpect(status().isNotFound());
    }

    public void verifyGetMocksResponse(String expectedResponse) throws Exception {
        String response = mvc.perform(get(MOCK_URL))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Assert.assertEquals("Get mocks result is incorrect",
            StringUtils.trimAllWhitespace(expectedResponse), StringUtils.trimAllWhitespace(response));
    }

    public void verifyGetMocksSortedByCreatedAscResponse(String expectedResponse) throws Exception {
        String response = mvc.perform(get(MOCK_URL + "?sort=created,asc"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Assert.assertEquals("Get mocks result sorted by created ASC is incorrect",
            StringUtils.trimAllWhitespace(expectedResponse), StringUtils.trimAllWhitespace(response));
    }

    public void verifyGetMocksByContextPathResponse(String expectedResponse) throws Exception {
        String response = mvc.perform(get(contextPath + MOCK_URL))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Assert.assertEquals("Get mocks by context path result is incorrect",
            StringUtils.trimAllWhitespace(expectedResponse), StringUtils.trimAllWhitespace(response));
    }

    public void verifyGetMockByIdResponse(Long id, String expectedResponse) throws Exception {
        String response = mvc.perform(get(MOCK_URL + "/" + id))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Assert.assertEquals("Get mock by id result is incorrect",
            StringUtils.trimAllWhitespace(expectedResponse), StringUtils.trimAllWhitespace(response));
    }

    public void verifyDoNothingWorksCorrect() throws Exception {
        mvc.perform(post(DO_NOTHING_URL + contextPath))
            .andExpect(status().isOk());
        mvc.perform(put(DO_NOTHING_URL + contextPath))
            .andExpect(status().isOk());
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
