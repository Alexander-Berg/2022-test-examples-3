package ru.yandex.canvas.controllers;

import java.util.List;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.DraftCreativeBatch;
import ru.yandex.canvas.model.File;
import ru.yandex.canvas.model.Files;
import ru.yandex.canvas.service.AuthServiceHttpProxy;
import ru.yandex.canvas.service.DirectService;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


/**
 * Almost fully java functional tests on ideas. It starts real spring context with web server and request it with real http client.
 * The only exception is direct auth service.
 * Tests is ignored because our main concept is python functional tests. And test environment does not support this kind of tests.
 * Tests is not deleted because having local functional tests speeds up bug fixing, trouble shouting and development.
 * To run this tests configure test/application.properties or set up environment variables
 *
 * @author solovyev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Ignore
public class IdeaControllerTest {
    private static final long USER_ID = 1;
    private static final long CLIENT_ID = 2;
    private static final String QUERY_STRING = "user_id=" + USER_ID + "&client_id=" + CLIENT_ID;


    @MockBean
    private DirectService directService;

    @MockBean
    private AuthServiceHttpProxy authService;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private HttpHeaders headers;

    @Before
    public void setUp() {
        headers = new HttpHeaders();
        headers.add("Content-type", "application/json");
    }

    @Test
    public void smokeTest() throws Exception {
        JSONObject ideaGenerationRequest = new JSONObject();
        ideaGenerationRequest.put("url", "http://www.lego.ru");

        final String queryString = "user_id=" + USER_ID + "&client_id=" + CLIENT_ID;

        ResponseEntity<DraftCreativeBatch> draftCreativesResponse =
                testRestTemplate.exchange("/ideas?" + queryString, HttpMethod.POST,
                        new HttpEntity<>(ideaGenerationRequest.toString(), headers), DraftCreativeBatch.class);

        Assert.assertEquals(draftCreativesResponse.getStatusCode(), HttpStatus.CREATED);
    }

    @Test
    public void generateCreativesTest() throws Exception {
        JSONObject ideaGenerationRequest = new JSONObject();
        ideaGenerationRequest.put("url", "http://www.lego.ru");

        ResponseEntity<DraftCreativeBatch> draftCreativesResponse =
                testRestTemplate.exchange("/ideas?" + QUERY_STRING, HttpMethod.POST,
                        new HttpEntity<>(ideaGenerationRequest.toString(), headers), DraftCreativeBatch.class);

        Assert.assertEquals(draftCreativesResponse.getStatusCode(), HttpStatus.CREATED);
        String ideaId = draftCreativesResponse.getBody().getIdeaId();

        ResponseEntity<DraftCreativeBatch> filesResponse =
                testRestTemplate.exchange("/ideas/" + ideaId + "/presets/1" + "?" + QUERY_STRING, HttpMethod.GET,
                        new HttpEntity<>(null, headers), DraftCreativeBatch.class);
        Assert.assertEquals(filesResponse.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void ideaGalleryTest() throws Exception {
        JSONObject ideaGenerationRequest = new JSONObject();
        ideaGenerationRequest.put("url", "http://www.lego.ru");

        ResponseEntity<DraftCreativeBatch> draftCreativesResponse =
                testRestTemplate.exchange("/ideas?" + QUERY_STRING, HttpMethod.POST,
                        new HttpEntity<>(ideaGenerationRequest.toString(), headers), DraftCreativeBatch.class);

        Assert.assertEquals(draftCreativesResponse.getStatusCode(), HttpStatus.CREATED);
        String ideaId = draftCreativesResponse.getBody().getIdeaId();

        ResponseEntity<Files> filesResponse =
                testRestTemplate.exchange("/files/idea/" + ideaId + "?" + QUERY_STRING, HttpMethod.GET,
                        new HttpEntity<>(null, headers), Files.class);
        Assert.assertEquals(filesResponse.getStatusCode(), HttpStatus.OK);
        List<File> files = filesResponse.getBody().getItems();
        Assert.assertFalse(files.isEmpty());
        File fileToUpload = files.get(files.size() - 1);


        ResponseEntity<File> fileResponse = testRestTemplate
                .exchange("/files/idea/" + ideaId + "/file/" + fileToUpload.getId() + "?" + QUERY_STRING,
                        HttpMethod.POST, new HttpEntity<>(null, null), File.class);
        Assert.assertEquals(fileResponse.getStatusCode(), HttpStatus.CREATED);
    }
}
