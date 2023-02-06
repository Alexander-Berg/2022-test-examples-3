package ru.yandex.canvas.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.io.Resources;
import one.util.streamex.StreamEx;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.canvas.Html5Constants;
import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.MDSService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.html5.PhantomJsCreativesValidator;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SourceControllerTest {

    private static byte[] zipArchive;
    private static byte[] realZipFromUser;
    private static byte[] htmlWithEventHandlersCCNoSS;
    private static byte[] demoZip;
    private static byte[] singleHtml;
    private static byte[] htmlWithEventHandlers;
    private static byte[] sampleHtml5CpmPriceSingle;
    private static byte[] sampleHtml5CpmPrice;

    @BeforeClass
    public static void init() {
        try {
            zipArchive = Resources.toByteArray(Resources.getResource("ru/yandex/canvas/controllers" +
                    "/html5CreativePreviewTest/zip_archive.zip"));
            realZipFromUser = Resources.toByteArray(Resources.getResource("test/real_user.zip"));
            htmlWithEventHandlersCCNoSS = Resources.toByteArray(Resources.getResource("test" +
                    "/sample_html5_728x90_adobe_animate_cc_noss.zip"));
            demoZip = Resources.toByteArray(Resources.getResource("test/sample_html5_728x90_demo.zip"));
            singleHtml = Resources.toByteArray(Resources.getResource("test/single_html.zip"));
            htmlWithEventHandlers = Resources.toByteArray(Resources.getResource("test" +
                    "/sample_html5_300x250_adobe_animate_cc_ss_dirs.zip"));
            sampleHtml5CpmPrice = Resources.toByteArray(Resources.getResource("test/sample_html5_cpm_price.zip"));
            sampleHtml5CpmPriceSingle = Resources.toByteArray(Resources.getResource("test" +
                    "/sample_html5_cpm_price_single.zip"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StillageService stillageService;

    @Autowired
    private PhantomJsCreativesValidator phantomJsCreativesValidator;

    @Autowired
    private MDSService mdsService;

    @Autowired
    private AuthService authService;

    @Autowired
    private SessionParams sessionParams;

    @Before
    public void setUp() {
        Mockito.reset(sessionParams);
    }

    @After
    public void tearDown() {
        Mockito.reset(sessionParams);
    }

    @Test
    public void uploadSourceSmokeTest() throws Exception {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_BANNER)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        MockMultipartFile mockMultipartFile = new MockMultipartFile("attachment", "attach.zip", "", zipArchive);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MDSService.MDSDir response = mock(MDSService.MDSDir.class);
        when(response.getDirUrl()).thenReturn("http://mds.yandex.net/");
        when(response.getURL()).thenReturn("http://mds.yandex.net/1234");

        when(mdsService.uploadMultiple(Mockito.any())).thenReturn(response);

        StillageFileInfo stillageFileInfo = new StillageFileInfo();
        stillageFileInfo.setUrl("https://zip.url");

        when(stillageService.uploadFile(Mockito.anyString(), Mockito.any(byte[].class))).thenReturn(stillageFileInfo);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/html5/source").file(mockMultipartFile)
                        .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(
                        json().when(IGNORING_EXTRA_FIELDS)
                                .isEqualTo("{\"name\":\"attach.zip\","
                                        + "\"archive\":false,\"width\":300,\"height\":300,\"url\":\"https://zip.url\","
                                        + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                                        + "\"preview_url\":\"http://mds.yandex.net/1234\","
                                        + "\"stillage_info\":{\"url\":\"https://zip.url\",\"fileSize\":0"
                                        + "},\"base_path\":\"http://mds.yandex.net/test_arc/\","
                                        + "\"html_filename\":\"test_arc/index.html\",\"html_replacements\":[]}")
                )
                .andExpect(status().is(201))
        ;

    }

    private void prepareJpgMock(int width, int height) throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MDSService.MDSDir response = mock(MDSService.MDSDir.class);
        when(response.getDirUrl()).thenReturn("http://mds.yandex.net/");
        when(response.getURL()).thenReturn("http://mds.yandex.net/1234");

        when(mdsService.uploadMultiple(Mockito.any())).thenReturn(response);

        StillageFileInfo stillageFileInfo = new StillageFileInfo();

        stillageFileInfo.setUrl("https://jpg.url");
        stillageFileInfo.setMimeType("image/jpeg");
        stillageFileInfo.setMetadataInfo(new HashMap<>());

        stillageFileInfo.getMetadataInfo().put("width", width);
        stillageFileInfo.getMetadataInfo().put("height", height);
        when(stillageService.uploadFile(Mockito.anyString(), Mockito.any(byte[].class))).thenReturn(stillageFileInfo);
    }

    @Test
    public void uploadJpgSourceSmokeTest() throws Exception {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_BANNER)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        MockMultipartFile mockMultipartFile = new MockMultipartFile("attachment", "attach.jpg", "", zipArchive);

        prepareJpgMock(240, 400);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/html5/source").file(mockMultipartFile)
                        .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().when(IGNORING_EXTRA_FIELDS)
                        .isEqualTo("{\"name\":\"attach.jpg\","
                                + "\"archive\":false,\"width\":240,\"height\":400,\"url\":\"https://jpg.url\","
                                + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                                + "\"preview_url\":\"http://mds.yandex.net/1234\","
                                + "\"stillage_info\":{\"url\":\"https://jpg.url\",\"fileSize\":0,"
                                + "\"mimeType\":\"image/jpeg\""
                                + "},\"base_path\":\"http://mds.yandex.net/\","
                                + "\"html_filename\":\"index.html\",\"html_replacements\":[],"
                                + "\"source_image_info\":{\"url\":\"https://jpg.url\",\"fileSize\":0,"
                                + "\"mimeType\":\"image/jpeg\",\"metadataInfo\":{\"width\":240,\"height\":400}"
                                + "}}"))
                .andExpect(status().is(201));

    }

    @Test
    public void uploadRealZipSmokeTest() throws Exception {
        checkCustomZipFileUploadingSuccess(realZipFromUser, "{\"name\":\"attach.zip\","
                + "\"archive\":false,\"width\":728,\"height\":90,\"url\":\"https://zip.url\","
                + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                + "\"preview_url\":\"http://mds.yandex.net/1234\","
                + "\"stillage_info\":{\"url\":\"https://zip.url\",\"fileSize\":0"
                + "},\"base_path\":\"http://mds.yandex.net/\","
                + "\"html_filename\":\"index.html\",\"html_replacements\":[]}");
    }

    @Test
    public void uploadDemoZipSmokeTest() throws Exception {
        checkCustomZipFileUploadingSuccess(demoZip,
                "{\"name\":\"attach.zip\","
                        + "\"archive\":false,\"width\":728,\"height\":90,\"url\":\"https://zip.url\","
                        + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                        + "\"preview_url\":\"http://mds.yandex.net/1234\","
                        + "\"stillage_info\":{\"url\":\"https://zip.url\",\"fileSize\":0"
                        + "},\"base_path\":\"http://mds.yandex.net/demo_html5/\","
                        + "\"html_filename\":\"demo_html5/index.htm\",\"html_replacements\":[]}");
    }

    @Test
    public void uploadSingleHtmlZip() throws Exception {
        checkCustomZipFileUploadingSuccess(singleHtml,
                "{\"name\":\"attach.zip\","
                        + "\"archive\":false,\"width\":728,\"height\":90,\"url\":\"https://zip.url\","
                        + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                        + "\"preview_url\":\"http://mds.yandex.net/1234\","
                        + "\"stillage_info\":{\"url\":\"https://zip.url\",\"fileSize\":0"
                        + "},\"base_path\":\"http://mds.yandex.net/\","
                        + "\"html_filename\":\"index.html\",\"html_replacements\":[]}");
    }

    @Test
    public void uploadZipWithEventHandlers_CpmBanner_Success() throws Exception {
        checkCustomZipFileUploadingSuccess(htmlWithEventHandlers,
                "{\"name\":\"attach.zip\","
                        + "\"archive\":false,\"width\":300,\"height\":250,\"url\":\"https://zip.url\","
                        + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                        + "\"preview_url\":\"http://mds.yandex.net/1234\","
                        + "\"stillage_info\":{\"url\":\"https://zip.url\",\"fileSize\":0"
                        + "},\"base_path\":\"http://mds.yandex.net/\","
                        + "\"html_filename\":\"300x250-1.html\",\"html_replacements\":[]}");
    }

    @Test
    public void uploadZipWithEventHandlers_ZipCCNoSS_CpmBanner_Success() throws Exception {
        checkCustomZipFileUploadingSuccess(htmlWithEventHandlersCCNoSS,
                "{\"name\":\"attach.zip\","
                        + "\"archive\":false,\"width\":728,\"height\":90,\"url\":\"https://zip.url\","
                        + "\"client_id\":1,\"screenshot_url\":\"http://my.screenshot.url\","
                        + "\"preview_url\":\"http://mds.yandex.net/1234\","
                        + "\"stillage_info\":{\"url\":\"https://zip.url\",\"fileSize\":0"
                        + "},\"base_path\":\"http://mds.yandex.net/\","
                        + "\"html_filename\":\"728_90.html\",\"html_replacements\":[]}");
    }

    @Test
    public void uploadZipWithEventHandlers_ZipCCNoSS_CpmYndxFrontpage_ValidationError() throws Exception {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_YNDX_FRONTPAGE)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_YNDX_FRONTPAGE);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        makeSourceControllerRequest(htmlWithEventHandlersCCNoSS)
                .andExpect(status().is(400))
                .andExpect(json().when(IGNORING_EXTRA_FIELDS, IGNORING_EXTRA_ARRAY_ITEMS)
                        .node("messages")
                        .matches(hasItem("HTML file contains forbidden handlers")));
    }

    @Test
    public void uploadZip_productTypeDoesNotSupport_returnError() throws Exception {
        SessionParams.SessionTag productType = StreamEx.of(SessionParams.Html5Tag.values())
                .map(SessionParams.Html5Tag::getSessionTag)
                .filter(e -> !Html5Constants.IS_ZIP_UPLOADING_SUPPORTED.contains(e))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Нет типов без поддержки ZIP, тест нужно отключить"));

        when(sessionParams.sessionIs(productType)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(productType);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        makeSourceControllerRequest(demoZip)
                .andExpect(json().when(IGNORING_EXTRA_FIELDS).node("messages")
                        .matches(Matchers.contains("This file format is not supported")))
                .andExpect(status().is(400));
        Mockito.reset(sessionParams);
    }

    @Test
    public void uploadGarbageTest() throws Exception {

        byte[] garbage = new byte[1024];
        garbage[0] = -123;
        garbage[2] = 123;

        MockMultipartFile mockMultipartFile = new MockMultipartFile("attachment", "attach.zip", "", garbage);

        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_BANNER)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/html5/source").file(mockMultipartFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().when(IGNORING_EXTRA_FIELDS).isEqualTo("{\"messages\":[\"There is no HTML "
                        + "file in banner\"]}"))
                .andExpect(status().is(400));
    }

    private ResultActions makeSourceControllerRequest(byte[] zip) throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile("attachment", "attach.zip", "", zip);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MDSService.MDSDir response = mock(MDSService.MDSDir.class);
        when(response.getDirUrl()).thenReturn("http://mds.yandex.net/");
        when(response.getURL()).thenReturn("http://mds.yandex.net/1234");

        when(mdsService.uploadMultiple(Mockito.any())).thenReturn(response);

        StillageFileInfo stillageFileInfo = new StillageFileInfo();
        stillageFileInfo.setUrl("https://zip.url");

        when(stillageService.uploadFile(Mockito.anyString(), Mockito.any(byte[].class))).thenReturn(stillageFileInfo);

        when(stillageService.uploadFile(Mockito.anyString(), (URL) Mockito.any())).thenReturn(stillageFileInfo);

        return mockMvc.perform(MockMvcRequestBuilders
                .multipart("/html5/source?user_id=2&client_id=1")
                .file(mockMultipartFile)
                .accept(MediaType.APPLICATION_JSON));
    }

    private void checkCustomZipFileUploadingSuccess(byte[] zip, String expectedJson) throws Exception {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_BANNER)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        makeSourceControllerRequest(zip)
                .andExpect(json().when(IGNORING_EXTRA_FIELDS).isEqualTo(expectedJson))
                .andExpect(status().is(201));
    }

    @Test
    public void testHtml5CpmPriceDouble() throws Exception {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_PRICE)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_PRICE);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();
        makeSourceControllerRequest(sampleHtml5CpmPrice)
                .andExpect(status().is(201));
    }

    @Test
    public void testHtml5CpmPriceSingle() throws Exception {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_PRICE)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_PRICE);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();
        makeSourceControllerRequest(sampleHtml5CpmPriceSingle)
                .andExpect(status().is(201));
    }

    private MockMultipartFile mockMultipartFile() {
        return new MockMultipartFile("attachment", "attach.jpg", "", zipArchive);
    }

    private void preparePriceSessionParams() {
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_PRICE)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_PRICE);
        when(sessionParams.getFilter()).thenReturn(new SessionParams.SharedDataFilter(List.of(
                new SessionParams.SharedDataFilterSize(1836, 572))));
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();
    }

    @Test
    public void uploadJpgPrice572() throws Exception {//Загружаем 1836x572 в прайсы успешно
        preparePriceSessionParams();
        prepareJpgMock(1836, 572);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/html5/source").file(mockMultipartFile())
                        .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(201));
    }

    @Test
    public void uploadJpgPriceWrongHeightValidationMessage() throws Exception {//Загружаем 42х42 в прайсы, валидация
        // требует 1836x572
        preparePriceSessionParams();
        prepareJpgMock(240, 400);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/html5/source").file(mockMultipartFile())
                        .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().when(IGNORING_EXTRA_FIELDS)
                        .isEqualTo("{\"messages\":[\"Creative must have one of the following dimensions: 1836x572\"]}"))
                .andExpect(status().is(400));
    }
}
