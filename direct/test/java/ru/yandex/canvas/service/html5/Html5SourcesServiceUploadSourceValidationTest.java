package ru.yandex.canvas.service.html5;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.canvas.exceptions.SourceValidationError;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.model.validation.Html5SizeValidator;
import ru.yandex.canvas.repository.html5.SourcesRepository;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.MDSService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.screenshooters.Html5SourceScreenshooterHelperService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.CPM_PRICE;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE;
import static ru.yandex.direct.feature.FeatureName.ALLOW_PROPORTIONALLY_LARGER_IMAGES;
import static ru.yandex.direct.feature.FeatureName.WIDE_MARKET;

@RunWith(SpringJUnit4ClassRunner.class)
public class Html5SourcesServiceUploadSourceValidationTest {
    private static final Logger logger = LoggerFactory.getLogger(Html5SourcesServiceUploadSourceValidationTest.class);
    private String filename = "dummy.jpg";
    private byte[] bytesContent400 = new byte[400 * 1024];
    private byte[] bytesContent = "dummy stuff".getBytes();
    private String properImageMimeType = "image/jpeg";
    private HashMap<String, Object> metadataInfo;
    private StillageFileInfo fileInfo;
    private Long clientId = 123L;

    @Autowired
    SessionParams sessionParams;

    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    Html5SourcesService html5SourcesService;

    @MockBean
    StillageService stillageService;

    @MockBean
    DirectService directService;

    @MockBean
    MDSService mdsService;

    @MockBean
    PhantomJsCreativesValidator phantomJsCreativesValidator;

    @Autowired
    Html5SizeValidator html5SizeValidator;

    MultipartFile file = new MockMultipartFile(filename, bytesContent);
    MultipartFile file16;
    MultipartFile file400 = new MockMultipartFile(filename, bytesContent400);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        metadataInfo = Maps.newHashMap();
        metadataInfo.put("width", 200);
        metadataInfo.put("height", 400);

        fileInfo = new StillageFileInfo();
        fileInfo.setId("aabb");
        fileInfo.setUrl("http://ya.ru/tst.jpg");
        fileInfo.setFileSize(1024 * 1024 * 149);
        fileInfo.setMd5Hash("aa");
        fileInfo.setMimeType(properImageMimeType);
        fileInfo.setMetadataInfo(metadataInfo);

        LocaleContextHolder.setLocale(new Locale("ru", "RU"));
        logger.info("locale is set to {}", LocaleContextHolder.getLocale());

        file16 = new MockMultipartFile("img1600.jpg", "img1600.jpg", "",
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/img16.jpg").readAllBytes());

        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);
        Mockito.when(directService.getFeatures(any(), any())).thenReturn(Collections.emptySet());

        var ppcProperty = Mockito.mock(PpcProperty.class);
        Mockito.when(ppcProperty.getOrDefault(any(Integer.class))).then(returnsFirstArg());
        Mockito.when(ppcPropertiesSupport.get(any(), any())).thenReturn(ppcProperty);
    }

    @Test
    public void uploadSourceImproperSizeMessage() {
        when(stillageService.uploadFile(filename, bytesContent)).thenReturn(fileInfo);
        exception.expect(SourceValidationError.class);
        exception.expectMessage(
                "Креатив должен иметь размер 1000x120, 160x600, 240x400, 240x600, 300x250, 300x300, 300x500, 300x600,"
                        + " 320x100, 320x480, 320x50, 336x280, 480x320, 728x90, 970x250");
        logger.info("locale is {}", LocaleContextHolder.getLocale());
        html5SourcesService.uploadImage(clientId, file, HTML5_CPM_BANNER);
    }

    @Test
    public void uploadImageFileSizeMessageYndxFrontpage() {
        exception.expect(SourceValidationError.class);
        exception.expectMessage("Размер файла img1600.jpg больше 1 МБ");
        logger.info("locale is {}", LocaleContextHolder.getLocale());
        html5SourcesService.uploadSource(file16, clientId, HTML5_CPM_YNDX_FRONTPAGE);
    }

    @Test
    public void uploadImageFileSizeCpmPrice() {
        exception.expect(SourceValidationError.class);
        exception.expectMessage("Размер файла img1600.jpg больше 1 МБ");
        logger.info("locale is {}", LocaleContextHolder.getLocale());
        html5SourcesService.uploadSource(file16, clientId, CPM_PRICE);
    }

    @Test
    public void uploadImageFileSizeCpmBanner() {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of(ALLOW_PROPORTIONALLY_LARGER_IMAGES.getName()));
        when(stillageService.uploadFile(filename, bytesContent)).thenReturn(fileInfo);

        exception.expect(SourceValidationError.class);
        exception.expectMessage("Размер файла " + file16.getName() + " больше 512 КБ");
        logger.info("locale is {}", LocaleContextHolder.getLocale());
        html5SourcesService.uploadSource(file16, clientId, HTML5_CPM_BANNER);
    }


    @TestConfiguration
    public static class Html5SourcesServiceConfiguration {

        @MockBean
        SessionParams sessionParams;

        @Bean
        Html5SizeValidator html5SizeValidator(SessionParams sessionParams) {
            return new Html5SizeValidator(sessionParams, ppcPropertiesSupport);
        }

        @MockBean
        SourcesRepository sourcesRepository;

        @MockBean
        PpcPropertiesSupport ppcPropertiesSupport;

        @MockBean
        ParallelFetcherFactory parallelFetcherFactory;

        @MockBean
        ScreenshooterService screenshooterService;

        @MockBean
        MovieServiceInterface movieService;

        @MockBean
        Html5SourceScreenshooterHelperService html5ScreenshooterHelperService;

        @Bean
        LocaleContext localeContext() {
            return () -> Locale.forLanguageTag("ru");
        }

        @Bean
        public Html5SourcesService html5SourcesService(StillageService stillageService,
                                                       MDSService mdsService,
                                                       SourcesRepository sourcesRepository,
                                                       ParallelFetcherFactory parallelFetcherFactory,
                                                       PhantomJsCreativesValidator phantomJsCreativesValidator,
                                                       Html5SourceScreenshooterHelperService html5ScreenshooterHelperService,
                                                       Html5SizeValidator html5SizeValidator,
                                                       DirectService directService) {
            return new Html5SourcesService(stillageService, mdsService, sourcesRepository,
                    parallelFetcherFactory, directService, movieService, phantomJsCreativesValidator,
                    html5ScreenshooterHelperService, html5SizeValidator);
        }
    }

    @Test
    public void uploadSourceDuration() throws IOException {
        StillageFileInfo fileInfo = new StillageFileInfo();
        fileInfo.setUrl("http://ya.ru/tst.mp4");
        HashMap<String, Object> metadataInfo = Maps.newHashMap();
        metadataInfo.put("duration", 302.1);
        fileInfo.setMetadataInfo(metadataInfo);
        when(stillageService.uploadFile("video1.mp4", new byte[]{1, 2, 3, 4, 5})).thenReturn(fileInfo);

        String index = IOUtils.toString(
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/cpm_price_index.html"),
                StandardCharsets.UTF_8);
        String extended = IOUtils.toString(
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/cpm_price_extended.html"),
                StandardCharsets.UTF_8);

        // Needed for Html5SourcesService.checkForExternalRequests()
        var stubFileInfo = new StillageFileInfo();
        var stubStillageUrl = "http://storage.mds.yandex.net/test";
        stubFileInfo.setUrl(stubStillageUrl);
        when(stillageService.uploadFile("logo.png", new byte[]{1, 2, 3, 4, 5})).thenReturn(stubFileInfo);
        when(stillageService.uploadFile("index.html", index.getBytes())).thenReturn(stubFileInfo);
        when(stillageService.uploadFile("extended.html", extended.getBytes())).thenReturn(stubFileInfo);
        when(phantomJsCreativesValidator.checkForExternalRequests(anyList())).thenReturn(List.of());

        exception.expect(SourceValidationError.class);
        exception.expectMessage("Максимальная продолжительность видеоролика — 5 минут");

        Html5Zip.Builder zipBuilder = Html5Zip.builder();
        zipBuilder.addFile("video1.mp4", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("logo.png", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.html", index.getBytes());
        zipBuilder.addFile("extended.html", extended.getBytes());
        Html5Zip zip = zipBuilder.build();
        MultipartFile file = new MockMultipartFile("zip.zip", "zip.zip", null, zip.toArchive());
        html5SourcesService.uploadSource(file, clientId, CPM_PRICE);
    }

    private StillageFileInfo getMarketFileInfo() {
        HashMap<String, Object> metadataInfo = Maps.newHashMap();
        metadataInfo.put("width", 800);
        metadataInfo.put("height", 120);

        StillageFileInfo fileInfo = new StillageFileInfo();
        fileInfo.setId("aabb");
        fileInfo.setUrl("http://ya.ru/tst.jpg");
        fileInfo.setFileSize(1024 * 1024 * 149);
        fileInfo.setMd5Hash("aa");
        fileInfo.setMimeType(properImageMimeType);
        fileInfo.setMetadataInfo(metadataInfo);
        return fileInfo;
    }

    private StillageFileInfo getProportionallyLargerFileInfo() {
        fileInfo = getMarketFileInfo();
        fileInfo.getMetadataInfo().put("width", 602);
        fileInfo.getMetadataInfo().put("height", 502);
        return fileInfo;
    }

    @Test
    public void marketFeatureOff() {
        when(stillageService.uploadFile(filename, bytesContent)).thenReturn(getMarketFileInfo());
        exception.expect(SourceValidationError.class);
        exception.expectMessage(
                "Креатив должен иметь размер 1000x120, 160x600, 240x400, 240x600, 300x250, 300x300, 300x500, 300x600,"
                        + " 320x100, 320x480, 320x50, 336x280, 480x320, 728x90, 970x250");
        logger.info("locale is {}", LocaleContextHolder.getLocale());
        html5SourcesService.uploadImage(clientId, file, HTML5_CPM_BANNER);
    }

    @Test
    public void marketFeatureOn() throws MalformedURLException {
        when(stillageService.uploadFile(any(), any(byte[].class))).thenReturn(getMarketFileInfo());
        when(directService.getFeatures(any(), any())).thenReturn(Set.of(WIDE_MARKET.getName()));
        MDSService.MDSDir dir = mock(MDSService.MDSDir.class);
        when(dir.getDirUrl()).thenReturn("http://mds.yandex.ru");
        when(mdsService.uploadMultiple(any())).thenReturn(dir);
        var src = html5SourcesService.uploadImage(clientId, file, HTML5_CPM_BANNER);
        assertEquals(Integer.valueOf(120), src.getHeight());
    }

    @Test
    public void allowProportionallyLargerImagesOn() throws MalformedURLException {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of(
                WIDE_MARKET.getName(),
                ALLOW_PROPORTIONALLY_LARGER_IMAGES.getName()
        ));
        when(stillageService.uploadFile(any(), any(byte[].class))).thenReturn(getProportionallyLargerFileInfo());

        MDSService.MDSDir dir = mock(MDSService.MDSDir.class);
        when(dir.getDirUrl()).thenReturn("http://mds.yandex.ru");
        when(mdsService.uploadMultiple(any())).thenReturn(dir);

        var src = html5SourcesService.uploadImage(clientId, file400, HTML5_CPM_BANNER);
        var sa = new SoftAssertions();
        sa.assertThat(src.getHeight()).isEqualTo(250);
        sa.assertThat(src.getWidth()).isEqualTo(300);
        sa.assertThat(src.getSourceImageInfo().getMetadataInfo().get("width")).isEqualTo(602);
        sa.assertThat(src.getSourceImageInfo().getMetadataInfo().get("height")).isEqualTo(502);
        sa.assertAll();
    }

    @Test
    public void allowProportionallyLargerImagesOnYndxFrontpageNotAllowed() throws MalformedURLException {
        Set<String> features = Set.of(
                WIDE_MARKET.getName(),
                ALLOW_PROPORTIONALLY_LARGER_IMAGES.getName()
        );
        when(directService.getFeatures(any(), any())).thenReturn(features);
        var fileInfo = getMarketFileInfo();
        fileInfo.getMetadataInfo().put("width", 640 * 2);
        fileInfo.getMetadataInfo().put("height", 134 * 2);
        when(stillageService.uploadFile(any(), any(byte[].class))).thenReturn(fileInfo);

        MDSService.MDSDir dir = mock(MDSService.MDSDir.class);
        when(dir.getDirUrl()).thenReturn("http://mds.yandex.ru");
        when(mdsService.uploadMultiple(any())).thenReturn(dir);
        var sizes = html5SizeValidator.validSizesByProductTypeString(features);

        exception.expect(SourceValidationError.class);
        exception.expectMessage("Креатив должен иметь размер " + sizes);

        html5SourcesService.uploadImage(clientId, file400, HTML5_CPM_YNDX_FRONTPAGE);
    }

    @Test
    public void allowProportionallyLargerImagesOffLargeFile() throws MalformedURLException {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of(
                WIDE_MARKET.getName()
        ));
        when(stillageService.uploadFile(any(), any(byte[].class))).thenReturn(getMarketFileInfo());

        MDSService.MDSDir dir = mock(MDSService.MDSDir.class);
        when(dir.getDirUrl()).thenReturn("http://mds.yandex.ru");
        when(mdsService.uploadMultiple(any())).thenReturn(dir);

        exception.expect(SourceValidationError.class);
        exception.expectMessage("Размер файла  больше 150 КБ");
        html5SourcesService.uploadImage(clientId, file400, HTML5_CPM_BANNER);
    }

    @Test
    public void allowProportionallyLargerImagesOff() throws MalformedURLException {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of(
                WIDE_MARKET.getName()
        ));
        when(stillageService.uploadFile(any(), any(byte[].class))).thenReturn(getProportionallyLargerFileInfo());

        MDSService.MDSDir dir = mock(MDSService.MDSDir.class);
        when(dir.getDirUrl()).thenReturn("http://mds.yandex.ru");
        when(mdsService.uploadMultiple(any())).thenReturn(dir);

        var sa = new SoftAssertions();
        sa.assertThatCode(() -> html5SourcesService.uploadImage(clientId, file, HTML5_CPM_BANNER))
                .isInstanceOf(SourceValidationError.class);
        sa.assertAll();
    }
}
