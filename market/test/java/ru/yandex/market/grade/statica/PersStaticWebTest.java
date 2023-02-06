package ru.yandex.market.grade.statica;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.verification.VerificationMode;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dinyat
 * 18/04/2017
 */
@Import({
    PersStaticTestConfiguration.class,
})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:/pers-static-test-application.properties")
@ActiveProfiles({"junit"})
public abstract class PersStaticWebTest extends AbstractPersWebTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("saasHttpClient")
    protected HttpClient saasHttpClientMock;

    protected MockMvc mockMvc;


    @Before
    public void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Before
    public void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }

    protected String invokeAndCheckResponse(
            String expectedResponseFilename,
            String mockedResponseFilename,
            String path) throws Exception {
        return invokeAndCheckResponse(expectedResponseFilename, mockedResponseFilename, path, null);
    }

    protected String invokeAndCheckResponse(
        String expectedResponseFilename,
        String mockedResponseFilename,
        String path,
        Map<String, String> parameters
    ) throws Exception {
        return invokeAndCheckResponse(expectedResponseFilename, mockedResponseFilename, path, parameters, Collections.emptyMap());
    }

    protected String invokeAndCheckResponse(
        String expectedResponseFilename,
        String mockedResponseFilename,
        String path,
        Map<String, String> parameters,
        Map<String, String> headers
    ) throws Exception {
        MultiValueMap<String, String> mparams = new LinkedMultiValueMap<>();
        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                mparams.add(entry.getKey(), entry.getValue());
            }
        }
        MultiValueMap<String, String> mheaders = new LinkedMultiValueMap<>();
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                mheaders.add(entry.getKey(), entry.getValue());
            }
        }
        return invokeAndCheckResponseMultiParam(expectedResponseFilename, mockedResponseFilename, path, mparams, mheaders);
    }

    protected String invokeAndCheckResponseMultiParam(
        String expectedResponseFilename,
        String mockedResponseFilename,
        String path,
        MultiValueMap<String, String> parameters
    ) throws Exception {
        return invokeAndCheckResponseMultiParam(expectedResponseFilename, mockedResponseFilename, path, parameters, null);
    }

    protected String invokeAndCheckResponseMultiParam(
        String expectedResponseFilename,
        String mockedResponseFilename,
        String path,
        MultiValueMap<String, String> parameters,
        MultiValueMap<String, String> headers
    ) throws Exception {
        String response = invokeAndCheckResponseMultiParam(
            mockedResponseFilename,
            path,
            parameters,
            headers,
            status().is2xxSuccessful());

        JSONAssert.assertEquals(
            IOUtils.readInputStream(
                getClass().getResourceAsStream(expectedResponseFilename)),
            response, JSONCompareMode.STRICT_ORDER);

        return response;
    }

    protected String invokeAndCheckResponseMultiParam(
        String mockedResponseFilename,
        String path,
        MultiValueMap<String, String> parameters,
        ResultMatcher resultMatcher
    ) throws Exception {
        return invokeAndCheckResponseMultiParam(mockedResponseFilename, path, parameters, null, resultMatcher);
    }

    protected String invokeAndCheckResponseMultiParam(
        String mockedResponseFilename,
        String path,
        MultiValueMap<String, String> parameters,
        MultiValueMap<String, String> headers,
        ResultMatcher resultMatcher
    ) throws Exception {
        setSaasResponse(mockedResponseFilename);
        MockHttpServletRequestBuilder requestBuilder = get(path);
        if (parameters != null && !parameters.isEmpty()) {
            requestBuilder = requestBuilder.params(parameters);
        }
        if (headers != null && !headers.isEmpty()) {
            requestBuilder = requestBuilder.headers(new HttpHeaders(headers));
        }

        return mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(resultMatcher)
                .andReturn().getResponse().getContentAsString();
    }

    protected void setSaasResponse(String filename) {
        HttpClientMockUtils.mockResponse(saasHttpClientMock,
                req -> getClass().getResourceAsStream(filename));
    }

    protected void setSaasResponse(String filename, ArgumentMatcher<HttpUriRequest> requestMatcher) {
        HttpClientMockUtils.mockResponse(saasHttpClientMock,
            req -> getClass().getResourceAsStream(filename), requestMatcher);
    }

    protected void verifySaasRequest(ArgumentMatcher<HttpUriRequest> requestMatcher) throws IOException {
        verifySaasRequest(requestMatcher, times(1));
    }

    protected void verifySaasRequest(ArgumentMatcher<HttpUriRequest> requestMatcher, VerificationMode times) throws IOException {
        verify(saasHttpClientMock, times).execute(argThat(requestMatcher));
    }

    protected void verifyBulkCountForKey(long expected, String modelId, String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        assertEquals(expected, json.getJSONObject(modelId).getLong("count"));
    }
}
