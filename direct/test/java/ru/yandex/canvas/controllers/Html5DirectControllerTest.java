package ru.yandex.canvas.controllers;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.controllers.html5.Html5DirectController;
import ru.yandex.canvas.model.direct.DirectUploadResult;
import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.html5.Html5SourcesService;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.utils.JsonUtils;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//TODO import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers;

@CanvasTest
@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5DirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchesRepository batchesRepository;

    @MockBean
    private RTBHostExportService rtbHostExportService;

    @MockBean
    private Html5SourcesService html5SourcesService;

    @Autowired
    private MockWebServer mockDirectWebServer;

    @Autowired
    private AuthService authService;

    @Autowired
    private DirectService directService;

    @Autowired
    private Html5SourcesService html5SourceService;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private AuthService authService;

        @Bean
        public MockWebServer mockWebServer() throws Exception {
            MockWebServer mockWebServer = new MockWebServer();
            mockWebServer.start();
            return mockWebServer;
        }

        @Bean
        public DirectService directService(TvmIntegration tvmIntegration,
                                           AsyncHttpClient asyncHttpClient,
                                           MockWebServer mockWebServer) {
            return new DirectService("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort(),
                    "12161216", tvmIntegration, TvmService.DIRECT_INTAPI_TEST, asyncHttpClient);
        }
    }

    @Before
    public void setUp() {
        mockDirectWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("POST") &&
                                request.getPath().startsWith("/feature_dev/access")) {
                            return new MockResponse().setBody("{\"result\": {\"client_ids_features\": {}}}");
                        }
                        return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                    }
                });
    }

    public List<Batch> getMockedBatches() {
        Batch batch1 = new Batch();
        Batch batch2 = new Batch();
        batch1.setName("batch1 name");
        batch2.setName("batch2 name");
        batch1.setId("deadbeef");
        batch2.setId("cafebabe");

        Creative creative1 = new Creative();
        creative1.setId(1L);
        creative1.setName("creative1.name");
        creative1.setScreenshotUrl("https://screen.url/1");
        creative1.setArchiveUrl("https://screen.url/archive/1");
        creative1.setWidth(100);
        creative1.setHeight(200);
        creative1.setBasePath("/base_path/1/");
        creative1.setSource(new Source());
        creative1.getSource().setStillageInfo(new Source.ZipStillageInfo(new StillageFileInfo()));
        creative1.getSource().getStillageInfo().setId("1");

        Creative creative6 = new Creative();
        creative6.setId(6L);
        creative6.setName("creative6.name");
        creative6.setScreenshotUrl("https://screen.url/6");
        creative6.setArchiveUrl("https://screen.url/archive/6");
        creative6.setWidth(100);
        creative6.setHeight(200);
        creative6.setBasePath("/base_path/6/");
        creative6.setSource(new Source());
        creative6.getSource().setStillageInfo(new Source.ZipStillageInfo(new StillageFileInfo()));
        creative6.getSource().getStillageInfo().setId("6");

        Creative creative7 = new Creative();
        creative7.setId(7L);
        creative7.setName("creative7.name");
        creative7.setScreenshotUrl("https://screen.url/7");
        creative7.setArchiveUrl("https://screen.url/archive/7");
        creative7.setWidth(100);
        creative7.setHeight(200);
        creative7.setBasePath("/base_path/7/");
        creative7.setSource(new Source());
        creative7.getSource().setStillageInfo(new Source.ZipStillageInfo(new StillageFileInfo()));
        creative7.getSource().getStillageInfo().setId("7");

        batch1.setCreatives(
                ImmutableList.of(creative1)
        );

        batch2.setCreatives(
                ImmutableList.of(creative6, creative7)
        );

        return ImmutableList.of(batch1, batch2);
    }

    @Test
    public void sendCreativesToDirectWithoutPayloadTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        mockMvc.perform(post("/html5/direct/creatives")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void sendCreativesToDirectWithBadPayloadTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        mockMvc.perform(post("/html5/direct/creatives")
                .content("{\"batches\":[ { \"id\":12, \"creative_ids\":[] } ] }")
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void sendCreativesToDirectWithUnknownBatch() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));
        when(authService.getUserId()).thenReturn(2L);

        mockMvc.perform(post("/html5/direct/creatives")
                .content("{\"batches\":[ { \"id\":\"deadbeef12\", \"creative_ids\":[1,2,3,4] } ] }")
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void sendCreativesToDirectTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        Html5DirectController.UploadRequest request = new Html5DirectController.UploadRequest();
        Html5DirectController.UploadRequest.BatchSpec batchSpec1 = new Html5DirectController.UploadRequest.BatchSpec();
        Html5DirectController.UploadRequest.BatchSpec batchSpec2 = new Html5DirectController.UploadRequest.BatchSpec();

        batchSpec1.setId("deadbeef");
        batchSpec2.setId("cafebabe");

        batchSpec1.getCreativeIds().addAll(Arrays.asList(1L));
        batchSpec2.getCreativeIds().addAll(Arrays.asList(6L, 7L));

        ObjectMapper objectMapper = new ObjectMapper();

        request.getBatches().add(batchSpec1);
        request.getBatches().add(batchSpec2);

        DirectUploadResult rc = new DirectUploadResult();

        DirectUploadResult.DirectUploadCreativeResult b1 = new DirectUploadResult.DirectUploadCreativeResult();
        b1.setCreativeId(1L);
        b1.setStatus("OK");

        DirectUploadResult.DirectUploadCreativeResult b6 = new DirectUploadResult.DirectUploadCreativeResult();
        b6.setCreativeId(6L);
        b6.setStatus("OK");

        DirectUploadResult.DirectUploadCreativeResult b7 = new DirectUploadResult.DirectUploadCreativeResult();
        b7.setCreativeId(7L);
        b7.setStatus("OK");

        rc.setUploadResults(ImmutableList.of(b1, b6, b7));

        when(batchesRepository.getBatchesByIds(eq(1L), Mockito.anyCollection())).thenReturn(getMockedBatches());

        mockDirectWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("POST") &&
                                request.getPath().startsWith("/DisplayCanvas/upload_creatives")) {
                            return new MockResponse().setBody(JsonUtils.toJson(rc));
                        } else if (request.getMethod().equals("POST") &&
                                request.getPath().startsWith("/feature_dev/access")) {
                            return new MockResponse().setBody("{\"result\": {\"client_ids_features\": {}}}");
                        }
                        return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                    }
                });
        //when(directService.getCreativesService()).thenReturn(creativesService);

        //doCallRealMethod().when(rtbHostExportService).exportToRtbHost(Mockito.anyList());

        when(authService.getUserId()).thenReturn(2L);

        mockMvc.perform(post("/html5/direct/creatives")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().when(Option.IGNORING_ARRAY_ORDER).isEqualTo("[1,6,7]"))
                .andExpect(status().is2xxSuccessful());

        verify(html5SourceService).downloadZipContent(Mockito.anyList());
    }

    @Test
    public void getCreativesListTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));
        when(authService.getUserId()).thenReturn(2L);
        //when(creativesService.getCreatives(Mockito.anyList(), Mockito.anyLong())).thenCallRealMethod();
        when(batchesRepository.getBatchesByCreativeIds(eq(1L), Mockito.anyList())).thenReturn(getMockedBatches());

        //when(creativesService.toCreativeUploadData(any())).thenCallRealMethod();

        mockMvc.perform(get("/html5/direct/creatives")
                .param("ids", "1,  7,8")
                .param("client_id", String.valueOf(1L))
                .header(HttpHeaders.AUTHORIZATION, "12161216")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("[{\"message\":null,\"ok\":true,\"creativeId\":1,\"creative\":{\"width\":100,"
                        + "\"height\":200,\"duration\":null,\"creative_id\":1,\"creative_type\":\"html5\","
                        + "\"creative_name\":\"batch1 name\",\"preview_url\":\"https://screen.url/1\","
                        + "\"expanded_preview_url\":null,"
                        + "\"live_preview_url\":\"https://canvas.preview.host/html5/deadbeef/1/preview\","
                        + "\"archive_url\":\"https://screen"
                        + ".url/archive/1\",\"composed_from\":null,\"stock_creative_id\":null,"
                        + "\"yabs_data\":{\"html5\":\"true\",\"basePath\":\"/base_path/1/\"},\"preset_id\":null,"
                        + "\"moderation_info\":{\"content_id\":1,\"html\":{},\"images\":[],\"texts\":null,"
                        + "\"videos\":null,\"sounds\":null,\"aspects\":null,\"bgrcolor\":null}, \"additional_data\":null,"
                        + "\"has_packshot\":null, \"is_adaptive\":null, \"is_brand_lift\":null}},{\"message\":null,\"ok\":true,\"creativeId\":7,"
                        + "\"creative\":{\"width\":100,\"height\":200,\"duration\":null,\"creative_id\":7,"
                        + "\"creative_type\":\"html5\",\"creative_name\":\"batch2 name\","
                        + "\"preview_url\":\"https://screen.url/7\","
                        + "\"expanded_preview_url\":null,"
                        + "\"live_preview_url\":\"https://canvas.preview.host/html5/cafebabe/7/preview\","
                        + "\"archive_url\":\"https://screen"
                        + ".url/archive/7\",\"composed_from\":null,\"stock_creative_id\":null,"
                        + "\"yabs_data\":{\"html5\":\"true\",\"basePath\":\"/base_path/7/\"},\"preset_id\":null,"
                        + "\"moderation_info\":{\"content_id\":7,\"html\":{},\"images\":[],\"texts\":null,"
                        + "\"videos\":null,\"sounds\":null,\"aspects\":null,\"bgrcolor\":null},\"additional_data\":null,"
                        + "\"has_packshot\":null, \"is_adaptive\":null, \"is_brand_lift\":null}},{\"ok\":false,\"message\":\"Not found\",\"creativeId\":8,\"creative\":null}]"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void getCreativesListWithoutIdsTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));
        when(authService.getUserId()).thenReturn(2L);

        mockMvc.perform(get("/html5/direct/creatives")
                .param("ids", "")
                .param("client_id", String.valueOf(1L))
                .header(HttpHeaders.AUTHORIZATION, "12161216")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    public void getCreativesListWithInvalidIdsTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));
        when(authService.getUserId()).thenReturn(2L);

        mockMvc.perform(get("/html5/direct/creatives")
                .param("ids", ",1,2,3")
                .param("client_id", String.valueOf(1L))
                .header(HttpHeaders.AUTHORIZATION, "12161216")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

}
