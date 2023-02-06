package ru.yandex.canvas.service.html5;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.Html5Constants;
import ru.yandex.canvas.exceptions.SourceValidationError;
import ru.yandex.canvas.model.validation.Html5SizeValidator;
import ru.yandex.canvas.service.SessionParams;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static ru.yandex.canvas.Html5Constants.HTML5_VIDEO_ALLOWED_FEATURE;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE;

@RunWith(SpringJUnit4ClassRunner.class)
public class ValidateZipTest {

    public static final String ADOBE_FLASH_HTML_5_FILE_CONTENT_XML =
            "/ru/yandex/canvas/service/html5/adobeFlashHtml5FileContent.xml";

    Html5SizeValidator html5SizeValidator = mock(Html5SizeValidator.class);

    @Before
    public void setLocale() {
        Locale.setDefault(new Locale("unknown", "UK"));
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Mockito.when(html5SizeValidator.isSizesValid(anyList(), any(), any())).thenReturn(true);
    }

    @Test
    public void checkALotOfFilesZip() {
        Html5Zip.Builder zipBuilder = Html5Zip.builder();

        for (int i = 0; i < Html5Constants.MAX_FILES_COUNT + 1; i++) {
            zipBuilder.addFile("testFile" + i + ".jpg", new byte[]{1, 2, 3, 4, 5});
        }

        Html5Zip zip = zipBuilder.build();

        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(HTML5_CPM_BANNER, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertEquals("Exception was thrown", sourceValidationError.getMessage(),
                new SourceValidationError(ImmutableList.of("too_much_files_inside_zip", "no_html_file_found"))
                        .getMessage());

    }

    @Test
    public void checkBadFileNamesZip() {
        Html5Zip.Builder zipBuilder = Html5Zip.builder();

        zipBuilder.addFile("testFileИлюша.jpg", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.html", new byte[]{1, 2, 3, 4, 5});

        Html5Zip zip = zipBuilder.build();

        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(HTML5_CPM_BANNER, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertNotNull(sourceValidationError);
        assertThat(sourceValidationError.getMessage()).contains("wrong_characters_in_filenames");
    }

    @Test
    public void testUpperCaseExtensions() {
        Html5Zip.Builder zipBuilder = Html5Zip.builder();

        zipBuilder.addFile("testFile.diplodock.JPG", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.HTML", new byte[]{1, 2, 3, 4, 5});

        Html5Zip zip = zipBuilder.build();

        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(SessionParams.Html5Tag.HTML5_CPM_BANNER, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertEquals("Exception was thrown", sourceValidationError.getMessage(),
                new SourceValidationError(ImmutableList.of("no_ad_size_found", "no_click_urls_found")).getMessage());
    }

    @Test
    public void unknownFileExtension() {
        Html5Zip.Builder zipBuilder = Html5Zip.builder();

        zipBuilder.addFile("testFile.diplodock", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.HTML", new byte[]{1, 2, 3, 4, 5});

        Html5Zip zip = zipBuilder.build();

        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(SessionParams.Html5Tag.HTML5_CPM_BANNER, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertNotNull(sourceValidationError);
        assertThat(sourceValidationError.getMessage()).contains("not_allowed_file_extension");
    }

    @Test
    public void tooMuchHtmlFiles() {
        Html5Zip.Builder zipBuilder = Html5Zip.builder();

        zipBuilder.addFile("testFile.jpg", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("yet_another.html", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.html", new byte[]{1, 2, 3, 4, 5});

        Html5Zip zip = zipBuilder.build();

        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(SessionParams.Html5Tag.HTML5_CPM_BANNER, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertNotNull(sourceValidationError);
        assertThat(sourceValidationError.getMessage()).contains("the_html_file_should_be_only_one");
    }

    @Test
    public void tooBigTotalFileSize() throws IOException {
        byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/html5/too_big_total_file_size.zip"));
        var zip = new Html5Zip(bytes);
        var validator = new Html5Validator(zip, html5SizeValidator);

        try {
            validator.validateUserZipCreative(HTML5_CPM_YNDX_FRONTPAGE, emptySet());
            fail();
        } catch (SourceValidationError e) {
            assertEquals("Exception should be thrown", e.getMessage(),
                    new SourceValidationError(ImmutableList.of("file_is_too_big")).getMessage());
        }
    }

    @Test
    public void bigFileSizeHtml5VideoAllowedFeature() throws IOException {
        byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/html5/too_big_total_file_size.zip"));
        var zip = new Html5Zip(bytes);
        var validator = new Html5Validator(zip, html5SizeValidator);

        Html5Validator.Html5ValidationResult result =
                validator.validateUserZipCreative(HTML5_CPM_BANNER, Set.of(HTML5_VIDEO_ALLOWED_FEATURE));

        assertNotNull(result);
    }

    @Test
    public void bigFileSizeHtml5VideoAllowedFeatureYndxFrontpage() throws IOException {
        byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/html5/too_big_total_file_size.zip"));
        var zip = new Html5Zip(bytes);
        var validator = new Html5Validator(zip, html5SizeValidator);

        try {
            validator.validateUserZipCreative(HTML5_CPM_YNDX_FRONTPAGE, Set.of(HTML5_VIDEO_ALLOWED_FEATURE));
            fail();
        } catch (SourceValidationError e) {
            assertEquals("Exception should be thrown", e.getMessage(),
                    new SourceValidationError(ImmutableList.of("file_is_too_big")).getMessage());
        }
    }

    @Test
    public void checkSS() throws IOException {
        String adobeFlashProNossHtml5FileContent = IOUtils.toString(
                getClass().getResourceAsStream(ADOBE_FLASH_HTML_5_FILE_CONTENT_XML),
                StandardCharsets.UTF_8);

        Html5Zip.Builder zipBuilder = Html5Zip.builder();

        for (int i = 0; i < Html5Constants.IMAGE_COUNT_MERGE_TO_SPRITE + 1; i++) {
            zipBuilder.addFile("testFile" + i + ".jpg", new byte[]{1, 2, 3, 4, 5});
        }

        zipBuilder.addFile("index.html", adobeFlashProNossHtml5FileContent.getBytes());

        Html5Zip zip = zipBuilder.build();

        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(SessionParams.Html5Tag.HTML5_CPM_BANNER, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertEquals("Exception was thrown", sourceValidationError.getMessage(),
                new SourceValidationError(ImmutableList.of("need_to_join_images_into_list_of_sprites")).getMessage());

    }

    @Test
    //Видео разрешаем только одно. Нужно это валидировать во время загрузки zip
    public void check2videos() throws IOException {
        String index = IOUtils.toString(
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/cpm_price_index.html"),
                StandardCharsets.UTF_8);
        String extended = IOUtils.toString(
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/cpm_price_extended.html"),
                StandardCharsets.UTF_8);
        Html5Zip.Builder zipBuilder = Html5Zip.builder();
        zipBuilder.addFile("video1.mp4", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("video2.mp4", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("logo.png", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.html", index.getBytes());
        zipBuilder.addFile("extended.html", extended.getBytes());
        Html5Zip zip = zipBuilder.build();
        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);
        SourceValidationError sourceValidationError = null;

        try {
            validator.validateUserZipCreative(SessionParams.Html5Tag.CPM_PRICE, emptySet());
        } catch (SourceValidationError e) {
            sourceValidationError = e;
        }

        assertEquals("Exception was thrown", sourceValidationError.getMessage(),
                new SourceValidationError(ImmutableList.of("the_video_file_should_be_only_one")).getMessage());
    }

    @Test
    //Видео разрешаем только одно. Нужно это валидировать во время загрузки zip
    public void check1video() throws IOException {
        String index = IOUtils.toString(
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/cpm_price_index.html"),
                StandardCharsets.UTF_8);
        String extended = IOUtils.toString(
                getClass().getResourceAsStream("/ru/yandex/canvas/service/html5/cpm_price_extended.html"),
                StandardCharsets.UTF_8);
        Html5Zip.Builder zipBuilder = Html5Zip.builder();
        zipBuilder.addFile("video1.mp4", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("logo.png", new byte[]{1, 2, 3, 4, 5});
        zipBuilder.addFile("index.html", index.getBytes());
        zipBuilder.addFile("extended.html", extended.getBytes());
        Html5Zip zip = zipBuilder.build();
        Html5Validator validator = new Html5Validator(zip, html5SizeValidator);

        Html5Validator.Html5ValidationResult result =
                validator.validateUserZipCreative(SessionParams.Html5Tag.CPM_PRICE, emptySet());

        assertNotNull(result);
    }
}
