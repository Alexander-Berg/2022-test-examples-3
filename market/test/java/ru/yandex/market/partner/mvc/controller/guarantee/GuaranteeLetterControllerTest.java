package ru.yandex.market.partner.mvc.controller.guarantee;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.io.Files;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.guarantee.GuaranteeLetter;
import ru.yandex.market.core.guarantee.GuaranteeLetterService;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link GuaranteeLetterController}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "GuaranteeLetterControllerTest.before.csv")
class GuaranteeLetterControllerTest extends FunctionalTest {

    private static final String BASE_PATH = "/guaranteeLetter";
    private static final String BASE_URL = "mds.net";
    private static final String FILE_PREFIX = "/bucket/guarantee-letter/";

    @Value("${mbi.mds.s3.bucket}:default-bucket")
    private String bucket;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private GuaranteeLetterService guaranteeLetterService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    private File tempDir;

    private static URL getMdsUrl(final String path) throws MalformedURLException {
        return new URL("https", BASE_URL, FILE_PREFIX + path);
    }

    @BeforeEach
    void setUp() {
        tempDir = Files.createTempDir();
    }

    @DbUnitDataSet(after = "GuaranteeLetterControllerTest.create.after.csv")
    @Test
    @DisplayName("Тест на загрузку новых гарантийных писем")
    void testCreate() throws IOException {
        String keyWithPrefix = "key";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, keyWithPrefix));
        doReturn(getMdsUrl("12/guarantee-letter-12.png")).when(mdsS3Client)
                .getUrl(Mockito.any(ResourceLocation.class));
        final File file = new File(tempDir, "our-shiny-guarantee-12.png");
        file.createNewFile();
        FunctionalTestHelper.post(getUrl(1012), createHttpEntity(file));
        checkUrl(12, "png");

        Mockito.verify(mdsS3Client).upload(Mockito.any(), Mockito.any());

        //проверяем, что добавление новых писем устанавливает статус фичи FeatureType.ALCOHOL в NEW
        Assertions.assertEquals(ParamCheckStatus.NEW, featureService.getFeature(12, FeatureType.ALCOHOL).getStatus());
    }

    @DbUnitDataSet(after = "GuaranteeLetterControllerTest.update.after.csv")
    @Test
    @DisplayName("Тест на обновление имеющихся гарантийных писем")
    void testUpdate() throws IOException {
        String keyWithPrefix = "key";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, keyWithPrefix));
        doReturn(getMdsUrl("10/guarantee-letter-10.pdf")).when(mdsS3Client)
                .getUrl(Mockito.any(ResourceLocation.class));
        final File file10 = new File(tempDir, "our-shiny-guarantee-10.pdf");
        file10.createNewFile();
        FunctionalTestHelper.post(getUrl(1010), createHttpEntity(file10));
        checkUrl(10, "pdf");

        //проверяем, что мы не удаляли письмо, потому что оно совпадает по имени со старым и просто перезаписалось в хранилище
        Mockito.verify(mdsS3Client, Mockito.never()).delete(Mockito.any());

        doReturn(getMdsUrl("11/guarantee-letter-11.jpg"))
                .when(mdsS3Client).getUrl(Mockito.any(ResourceLocation.class));
        final File file11 = new File(tempDir, "our-shiny-guarantee-11-new.jpg");
        file11.createNewFile();
        FunctionalTestHelper.post(getUrl(1011), createHttpEntity(file11));
        checkUrl(11, "jpg");

        //проверяем, что при добавлении письма с другим расширением старое письмо удалилось - не совпало по имени
        Mockito.verify(mdsS3Client).delete(Mockito.any());

        //проверяем, что оба письма загрузились в mds
        Mockito.verify(mdsS3Client, Mockito.times(2)).upload(Mockito.any(), Mockito.any());

        //проверяем, что перезапись писем устанавливает статус фичи FeatureType.ALCOHOL в NEW
        Assertions.assertEquals(ParamCheckStatus.NEW, featureService.getFeature(10, FeatureType.ALCOHOL).getStatus());
        Assertions.assertEquals(ParamCheckStatus.NEW, featureService.getFeature(11, FeatureType.ALCOHOL).getStatus());
    }

    @Test
    @DisplayName("Тест на обновление гарантийного письма, если статус фичи уже был NEW")
    void testUpdateWithNewFeatureStatus() throws IOException {
        String keyWithPrefix = "key";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, keyWithPrefix));
        doReturn(getMdsUrl("13/guarantee-letter-13.pdf"))
                .when(mdsS3Client).getUrl(Mockito.any(ResourceLocation.class));
        final File file = new File(tempDir, "our-shiny-guarantee-13.pdf");
        file.createNewFile();
        FunctionalTestHelper.post(getUrl(1013), createHttpEntity(file));
        checkUrl(13, "pdf");

        //проверяем, что перезапись письма оставила статус фичи FeatureType.ALCOHOL в NEW
        Assertions.assertEquals(ParamCheckStatus.NEW, featureService.getFeature(13, FeatureType.ALCOHOL).getStatus());
    }

    @DbUnitDataSet(after = "GuaranteeLetterControllerTest.delete.after.csv")
    @Test
    @DisplayName("Тест на удаление гарантийных писем")
    void testDelete() {
        String keyWithPrefix = "key";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, keyWithPrefix));
        FunctionalTestHelper.delete(getUrl(1010));
        Mockito.verify(mdsS3Client).delete(Mockito.any());
        Assertions.assertNull(guaranteeLetterService.getGuaranteeLetter(10));
        Assertions.assertEquals(ParamCheckStatus.DONT_WANT, featureService.getFeature(10, FeatureType.ALCOHOL).getStatus());
    }

    @Test
    @DisplayName("Тест на удаление гарантийных писем, если статус фичи уже был DONT_WANT")
    void testDeleteWithDontWantFeatureStatus() {
        String keyWithPrefix = "key";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, keyWithPrefix));
        FunctionalTestHelper.delete(getUrl(1014));
        Mockito.verify(mdsS3Client).delete(Mockito.any());
        Assertions.assertNull(guaranteeLetterService.getGuaranteeLetter(14));
        Assertions.assertEquals(ParamCheckStatus.DONT_WANT, featureService.getFeature(14, FeatureType.ALCOHOL).getStatus());
    }

    @Test
    @DisplayName("Тест на ошибку при удалении несуществующего гарантийного письма")
    void testDeleteNotFound() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(getUrl(1012)));
        Mockito.verifyZeroInteractions(mdsS3Client);
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "campaign_id", "INVALID")));
    }

    @DbUnitDataSet(after = "GuaranteeLetterControllerTest.before.csv")
    @Test
    @DisplayName("Тест на невозможность загрузить файлы с расширениями, отсутствующими в white-list'е расширений")
    void testInvalidFileExtension() {
        HttpClientErrorException httpException =
                Assertions.assertThrows(HttpClientErrorException.class,
                        () -> FunctionalTestHelper.post(getUrl(1010), createHttpEntity(File.createTempFile("filename", ".docx", tempDir))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, httpException.getStatusCode());
        httpException =
                Assertions.assertThrows(HttpClientErrorException.class,
                        () -> FunctionalTestHelper.post(
                                getUrl(1011), createHttpEntity(File.createTempFile("filename", "filename", tempDir))));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, httpException.getStatusCode());
    }

    @Test
    @DisplayName("Тест на ошибку при попытке загрузить письмо, не передавая файл в запросе")
    void testPostWithoutFile() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getUrl(1012))
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "file", "MISSING")));
        ;
    }

    @Test
    @DisplayName("Тест на получение информации о ГП")
    void testGetInfo() throws MalformedURLException {
        String keyWithPrefix = "key";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, keyWithPrefix));
        doReturn(getMdsUrl("10/guarantee-letter-10.pdf"))
                .when(mdsS3Client).getUrl(Mockito.any(ResourceLocation.class));
        ResponseEntity<String> entity = FunctionalTestHelper.get(getUrl(1010));
        JsonTestUtil.assertEquals(entity, getClass(), "GuaranteeLetterControllerTest.getInfo.json");
    }

    @Test
    @DisplayName("Тест на ошибку при попытке получить несуществующее ГП")
    void testGetInfoNotFound() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getUrl(1012))
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "campaign_id", "INVALID")));
        ;
    }

    @Nonnull
    private String getUrl(int campaignId) {
        return baseUrl + BASE_PATH + "?campaign_id=" + campaignId;
    }

    private void checkUrl(int shopId, String extension) {
        GuaranteeLetter letter = guaranteeLetterService.getGuaranteeLetter(shopId);
        Assertions.assertNotNull(letter);
        Assertions.assertEquals(getLetterUrl(shopId, extension), guaranteeLetterService.buildUrl(letter));
    }

    private HttpEntity createHttpEntity(File file) {
        return FunctionalTestHelper.createMultipartHttpEntity(
                "file",
                new FileSystemResource(file),
                (params) -> params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()))
        );
    }

    private String getLetterUrl(long partnerId, String extension) {
        return String.format("https://%s%s%d/guarantee-letter-%d.%s", BASE_URL, FILE_PREFIX, partnerId, partnerId, extension);
    }
}
