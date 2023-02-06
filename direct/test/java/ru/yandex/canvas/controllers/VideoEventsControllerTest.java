package ru.yandex.canvas.controllers;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.configs.CanvasAuthInterceptor;
import ru.yandex.canvas.configs.auth.AuthorizeBy;
import ru.yandex.canvas.configs.auth.QueryStringAuthorizer;
import ru.yandex.canvas.configs.auth.SandboxCallbackAuthorizer;
import ru.yandex.canvas.controllers.video.VideoFilesController;
import ru.yandex.canvas.controllers.video.VideoFilesModifyingController;
import ru.yandex.canvas.model.avatars.AvatarsPutCanvasResult;
import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.SandBoxService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.AudioService;
import ru.yandex.canvas.service.video.CmsConversionStatusUpdateService;
import ru.yandex.canvas.service.video.InBannerVideoFilesService;
import ru.yandex.canvas.service.video.MovieService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.VhService;
import ru.yandex.canvas.service.video.VideoFileUploadService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.VideoSoundTrackService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.direct.rotor.client.RotorClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.configs.auth.AuthorizeBy.AuthType.TRUSTED_QUERY_STRING;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_CLIENT;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesModifyingController.class)
@Import(ControllerTestConfiguration.class)
public class VideoEventsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvatarsService avatarsService;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @TestConfiguration
    public static class TestConf {
        @MockBean(name = ROTOR_CLIENT)
        private RotorClient rotorClient;

        @MockBean
        private VhService vhClient;

        @MockBean
        DirectService directService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        InBannerVideoFilesService inBannerVideoFilesService;

        @Autowired
        StillageInfoConverter stillageInfoConverter;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean
        public CanvasAuthInterceptor canvasAuthInterceptor(AuthRequestParams authRequestParams) {
            CanvasAuthInterceptor.Builder builder = CanvasAuthInterceptor.builder();

            return builder
                    .defaultAuth(List.of(TRUSTED_QUERY_STRING))
                    .register(TRUSTED_QUERY_STRING, new QueryStringAuthorizer(authRequestParams))
                    .register(AuthorizeBy.AuthType.SANDBOX_SECRET, new SandboxCallbackAuthorizer("hookSecret"))
                    .build();
        }

        @Bean
        public VideoFileUploadServiceInterface videoFileUploadService(SandBoxService sandBoxService,
                                                                      VideoFilesRepository videoFilesRepository,
                                                                      VideoLimitsService videoLimitsService,
                                                                      AvatarsService avatarsService,
                                                                      StillageService stillageService) {
            return new VideoFileUploadService(sandBoxService, "hookSecret", "http://va.url/",
                    videoFilesRepository, videoLimitsService, avatarsService, directService, videoPresetsService,
                    stillageService, stillageInfoConverter, vhClient, videoGeometryService);
        }

        @Bean
        public VideoFilesController videoFilesController(StockMoviesService stockMoviesService,
                                                         AuthService authService,
                                                         VideoFilesRepository videoFilesRepository,
                                                         MovieServiceInterface movieService,
                                                         PackshotService packshotService,
                                                         AudioService audioService,
                                                         VideoSoundTrackService videoSoundTrackService,
                                                         SessionParams sessionParams) {
            return new VideoFilesController(stockMoviesService, authService, videoFilesRepository,
                    movieService, packshotService, audioService, videoSoundTrackService, sessionParams,
                    inBannerVideoFilesService, directService);
        }

        @Bean
        MovieServiceInterface movieServiceInterface(VideoFilesRepository videoFilesRepository,
                                                    StockMoviesService stockMoviesService,
                                                    VideoFileUploadServiceInterface videoFileUploadService,
                                                    StillageService stillageService,
                                                    MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                    VideoLimitsService videoLimitsService) {
            return new MovieService(videoFilesRepository, stockMoviesService, videoFileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }

    private static final String exampleVideoFile = "{\n"
            + "\t\"id\" : \"59e06670a980ee031ba6cd0c\",\n"
            + "\t\"clientId\" : 33266013,\n"
            + "\t\"name\" : \"ezgif-1-499b5cf3f6.mp4\",\n"
            + "\t\"stillageId\" : 6464600,\n"
            + "\t\"type\" : \"video\",\n"
            + "\t\"overlayColor\" : \"#000000\",\n"
            + "\t\"date\" : \"2017-10-13T10:08:32.415Z\",\n"
            + "\t\"subCategories\" : [ ],\n"
            + "\t\"url\" : \"https://storage.mds.yandex.net/get-bstor/15200/a032c580-a2a4-4800-887e-6bfe7222addc"
            + ".mp4\",\n"
            + "\t\"stillageFileInfo\" : {\n"
            + "\t\t\"mimeType\" : \"video/mp4\",\n"
            + "\t\t\"metadataInfo\" : {\n"
            + "\t\t\t\"duration\" : 14.92,\n"
            + "\t\t\t\"videoStreams\" : [\n"
            + "\t\t\t\t{\n"
            + "\t\t\t\t\t\"profile\" : \"High\",\n"
            + "\t\t\t\t\t\"index\" : 0,\n"
            + "\t\t\t\t\t\"level\" : \"31\",\n"
            + "\t\t\t\t\t\"colorSpace\" : \"\",\n"
            + "\t\t\t\t\t\"frameRate\" : 25,\n"
            + "\t\t\t\t\t\"height\" : 540,\n"
            + "\t\t\t\t\t\"width\" : 960,\n"
            + "\t\t\t\t\t\"pixelFormat\" : \"yuv420p\",\n"
            + "\t\t\t\t\t\"codec\" : \"h264\",\n"
            + "\t\t\t\t\t\"duration\" : 14.92,\n"
            + "\t\t\t\t\t\"bitrate\" : 661085,\n"
            + "\t\t\t\t\t\"colorRange\" : \"\"\n"
            + "\t\t\t\t}\n"
            + "\t\t\t],\n"
            + "\t\t\t\"bitrate\" : 665426,\n"
            + "\t\t\t\"audioStreams\" : [ ]\n"
            + "\t\t},\n"
            + "\t\t\"url\" : \"https://storage.mds.yandex.net/get-bstor/15200/a032c580-a2a4-4800-887e-6bfe7222addc"
            + ".mp4\",\n"
            + "\t\t\"md5Hash\" : \"1BKiS1GkKKb/NzJ0RPHoAg==\",\n"
            + "\t\t\"fileSize\" : 1241020,\n"
            + "\t\t\"contentGroup\" : \"VIDEO\",\n"
            + "\t\t\"id\" : 6464600\n"
            + "\t},\n"
            + "\t\"archive\" : false,\n"
            + "\t\"status\" : \"converting\",\n"
            + "\t\"convertionTaskId\" : 246809441,\n"
            + "\t\"formats\" : [\n"
            + "\t\t{\n"
            + "\t\t\t\"url\" : \"\",\n"
            + "\t\t\t\"delivery\" : \"progressive\",\n"
            + "\t\t\t\"type\" : \"application/vnd.apple.mpegurl\",\n"
            + "\t\t\t\"id\" : \"\"\n"
            + "\t\t},\n"
            + "\t\t{\n"
            + "\t\t\t\"delivery\" : \"progressive\",\n"
            + "\t\t\t\"width\" : \"640\",\n"
            + "\t\t\t\"url\" : \"https://strm.yandex"
            + ".ru/vh-canvas-converted/get-canvas/video_59e06670a980ee031ba6cd0c_169_360p.mp4\",\n"
            + "\t\t\t\"type\" : \"video/mp4\",\n"
            + "\t\t\t\"bitrate\" : 733,\n"
            + "\t\t\t\"id\" : \"video_59e06670a980ee031ba6cd0c_169_360p.mp4\",\n"
            + "\t\t\t\"height\" : \"360\"\n"
            + "\t\t},\n"
            + "\t\t{\n"
            + "\t\t\t\"delivery\" : \"progressive\",\n"
            + "\t\t\t\"width\" : \"852\",\n"
            + "\t\t\t\"url\" : \"https://strm.yandex"
            + ".ru/vh-canvas-converted/get-canvas/video_59e06670a980ee031ba6cd0c_169_480p.mp4\",\n"
            + "\t\t\t\"type\" : \"video/mp4\",\n"
            + "\t\t\t\"bitrate\" : 1247,\n"
            + "\t\t\t\"id\" : \"video_59e06670a980ee031ba6cd0c_169_480p.mp4\",\n"
            + "\t\t\t\"height\" : \"480\"\n"
            + "\t\t},\n"
            + "\t\t{\n"
            + "\t\t\t\"delivery\" : \"progressive\",\n"
            + "\t\t\t\"width\" : \"640\",\n"
            + "\t\t\t\"url\" : \"https://strm.yandex"
            + ".ru/vh-canvas-converted/get-canvas/video_59ee45f220b929f6d53b2d57_169_360p.ogv\",\n"
            + "\t\t\t\"type\" : \"video/ogg\",\n"
            + "\t\t\t\"bitrate\" : 1555,\n"
            + "\t\t\t\"id\" : \"video_59ee45f220b929f6d53b2d57_169_360p.ogv\",\n"
            + "\t\t\t\"height\" : \"360\"\n"
            + "\t\t},"
            + "\t\t{\n"
            + "\t\t\t\"delivery\" : \"progressive\",\n"
            + "\t\t\t\"width\" : \"640\",\n"
            + "\t\t\t\"url\" : \"https://strm.yandex"
            + ".ru/vh-canvas-converted/get-canvas/video_59ee45f220b929f6d53b2d57_169_360p.webm\",\n"
            + "\t\t\t\"type\" : \"video/webm\",\n"
            + "\t\t\t\"bitrate\" : 638,\n"
            + "\t\t\t\"id\" : \"video_59ee45f220b929f6d53b2d57_169_360p.webm\",\n"
            + "\t\t\t\"height\" : \"360\"\n"
            + "\t\t},"
            + "\t\t{\n"
            + "\t\t\t\"delivery\" : \"progressive\",\n"
            + "\t\t\t\"width\" : \"432\",\n"
            + "\t\t\t\"url\" : \"https://strm.yandex"
            + ".ru/vh-canvas-converted/get-canvas/video_59e06670a980ee031ba6cd0c_169_240p.mp4\",\n"
            + "\t\t\t\"type\" : \"video/mp4\",\n"
            + "\t\t\t\"bitrate\" : 373,\n"
            + "\t\t\t\"id\" : \"video_59e06670a980ee031ba6cd0c_169_240p.mp4\",\n"
            + "\t\t\t\"height\" : \"240\"\n"
            + "\t\t}\n"
            + "\t],\n"
            + "\t\"strmPrefix\" : \"video_59e06670a980ee031ba6cd0c\",\n"
            + "\t\"thumbnailUrl\" : \"https://avatars.mds.yandex"
            + ".net/get-canvas/209571/2a00000163a7b1e39ee5c11c9c3d61dc597c/orig\",\n"
            + "\t\"thumbnail\" : {\n"
            + "\t\t\"url\" : \"https://avatars.mds.yandex"
            + ".net/get-canvas/209571/2a00000163a7b1e39ee5c11c9c3d61dc597c/orig\",\n"
            + "\t\t\"width\" : 960,\n"
            + "\t\t\"preview\" : {\n"
            + "\t\t\t\"url\" : \"https://avatars.mds.yandex"
            + ".net/get-canvas/209571/2a00000163a7b1e39ee5c11c9c3d61dc597c/preview480p\",\n"
            + "\t\t\t\"width\" : 852,\n"
            + "\t\t\t\"height\" : 479\n"
            + "\t\t},\n"
            + "\t\t\"height\" : 540\n"
            + "\t}\n"
            + "}";


    void doRequest(String request) throws Exception {
        AvatarsPutCanvasResult avatarsPutCanvasResult = new AvatarsPutCanvasResult();
        AvatarsPutCanvasResult.SizesInfo sizesInfo = new AvatarsPutCanvasResult.SizesInfo();
        AvatarsPutCanvasResult.SizeInfo origInfo = new AvatarsPutCanvasResult.SizeInfo();
        origInfo.setHeight(1024);
        origInfo.setUrl("http://avatars.ru/orig.jpg");
        origInfo.setWidth(768);

        AvatarsPutCanvasResult.SizeInfo preview = new AvatarsPutCanvasResult.SizeInfo();
        preview.setWidth(360);
        preview.setHeight(720);
        preview.setUrl("http://avatars.ru/preview.jpg");

        sizesInfo.setOrig(origInfo);
        sizesInfo.setPreview480p(preview);

        avatarsPutCanvasResult.setSizes(sizesInfo);

        when(avatarsService
                .upload("https://strm.yandex.ru/vh-canvas-converted/get_canvas/video_5c4b2ddb3bbdd16d033271fe_001.jpg"))
                .thenReturn(avatarsPutCanvasResult);

        VideoFiles rec = new ObjectMapper().readValue(exampleVideoFile, VideoFiles.class);

        when(videoFilesRepository.findByIdAndQuery(eq("1221abs"), any())).thenReturn(rec);
        when(videoFilesRepository.findById(eq("1221abs"))).thenReturn(rec);

        mockMvc.perform(post("/video/files/1221abs/event")
                .content(request)
                .param("secret", "hookSecret")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is(200));
    }

    @Test
    public void correctAnswerTest() throws Exception {
        String request = "{\"status\": \"THUMBNAILS_UPLOADED\", \"task_id\": 368971541, \"thumbnail_urls\":\n"
                + "    \"[\\\"https://strm.yandex"
                + ".ru/vh-canvas-converted/get_canvas/video_5c4b2ddb3bbdd16d033271fe_001.jpg\\\"]\"}";
        doRequest(request);
    }

    @Test
    public void requestWithUnknownFieldsTest() throws Exception {
        String request =
                "{\"status\": \"THUMBNAILS_UPLOADED\", \"unknownField\":12,  \"task_id\": 368971541, "
                        + "\"thumbnail_urls\":\n"
                        + "    \"[\\\"https://strm.yandex"
                        + ".ru/vh-canvas-converted/get_canvas/video_5c4b2ddb3bbdd16d033271fe_001.jpg\\\"]\"}";
        doRequest(request);
    }

    @Test
    public void requestWithUnknownFieldsInFormatsTest() throws Exception {
        String request = "{\"formats\": \"[{\\\"foo\\\": \\\"bar\\\"}]\"}";
        doRequest(request);
    }

    @Test
    public void requestWithUnknownFieldsInRawFormatsTest() throws Exception {
        String request = "{\"formats\": [{\"foo\": \"bar\", \"bitrate\": 124}]}";
        doRequest(request);
    }
}
