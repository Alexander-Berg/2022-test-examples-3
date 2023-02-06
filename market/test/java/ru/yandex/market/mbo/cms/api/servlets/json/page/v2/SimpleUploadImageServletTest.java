package ru.yandex.market.mbo.cms.api.servlets.json.page.v2;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.market.mbo.cms.core.helpers.PermissionCheckHelper;
import ru.yandex.market.mbo.cms.core.image.thumbnail.ImageStorageService;
import ru.yandex.market.mbo.cms.core.json.api.response.JsonPageApiResponse;
import ru.yandex.market.mbo.cms.core.json.service.exceptions.JsonPageUnexpectedParameterValueException;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.ImageDescription;
import ru.yandex.market.mbo.cms.core.service.CmsUsersManagerMock;
import ru.yandex.market.mbo.cms.core.service.user.UserRole;
import ru.yandex.market.mbo.cms.core.utils.http.HttpUploadImageHelper;
import ru.yandex.market.mbo.cms.core.utils.http.HttpUploadImageProcessor;
import ru.yandex.market.mbo.cms.core.utils.http.RequestCredentials;

import static org.apache.commons.fileupload.FileUploadBase.MULTIPART_FORM_DATA;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class SimpleUploadImageServletTest {
    private static final long USER_ID = 100;
    private static final String USER_NAME = "TEST_USER_NAME";
    private static final String BOUNDARY = "TEST_BOUNDARY_VALUE";
    private static final String PIC_FILE = "pic.png";
    private static final String DUMMY_CONTENT = "text";
    private static final String CRLF = "\r\n";
    private static final String CONTENT_TYPE = MULTIPART_FORM_DATA + "; BOUNDARY=" + BOUNDARY;

    private static final int SIZE = 10;
    private static final ImageDescription DEFAULT_RESULT = new ImageDescription(SIZE, SIZE, "dummy");

    @Test
    public void testAllOk() throws IOException {
        ImageStorageServiceMock iss = new ImageStorageServiceMock(DEFAULT_RESULT);
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestCredentials.putCredentials(request, USER_ID, USER_NAME, Arrays.asList(UserRole.values()), Map.of());
        request.setMethod("POST");
        request.setContentType(CONTENT_TYPE);
        request.setContent(buildContent("image.png", "200x200", picAsBytes(PIC_FILE)));
        SimpleUploadImageServlet servlet =
            new SimpleUploadImageServlet(
                new HttpUploadImageHelper(
                    new HttpUploadImageProcessor(iss, Mockito.mock(MetricsLogger.class)),
                    new CmsUsersManagerMock(USER_ID),
                    Mockito.mock(PermissionCheckHelper.class)
                ),
                Mockito.mock(PermissionCheckHelper.class)
            );
        JsonPageApiResponse result = servlet.process(request);
        Assert.assertNotNull(result.getJsonObject());
        Assert.assertThat(result.getJsonObject(), instanceOf(SimpleUploadImageServlet.UploadResult.class));
        SimpleUploadImageServlet.UploadResult uploadImageResult =
            (SimpleUploadImageServlet.UploadResult) result.getJsonObject();
        Assert.assertEquals(uploadImageResult.getUrl(), "dummy");
        Assert.assertNull(uploadImageResult.getErrorMessage());
    }

    @Test
    public void testWrongFormat() {
        ImageStorageServiceMock iss = new ImageStorageServiceMock(DEFAULT_RESULT);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType(CONTENT_TYPE);
        request.setContent(buildContent("image.str", null, DUMMY_CONTENT.getBytes()));
        SimpleUploadImageServlet servlet =
            new SimpleUploadImageServlet(
                new HttpUploadImageHelper(new HttpUploadImageProcessor(iss, Mockito.mock(MetricsLogger.class)),
                    new CmsUsersManagerMock(USER_ID), Mockito.mock(PermissionCheckHelper.class)),
                Mockito.mock(PermissionCheckHelper.class));
        JsonPageApiResponse result = servlet.process(request);
        Assert.assertNotNull(result.getJsonObject());
        Assert.assertThat(result.getJsonObject(), instanceOf(SimpleUploadImageServlet.UploadResult.class));
        SimpleUploadImageServlet.UploadResult uploadImageResult =
            (SimpleUploadImageServlet.UploadResult) result.getJsonObject();
        Assert.assertNull(uploadImageResult.getUrl());
        Assert.assertNotNull(uploadImageResult.getErrorMessage());
    }

    @Test(expected = JsonPageUnexpectedParameterValueException.class)
    public void testMultipleThumbs() {
        ImageStorageServiceMock iss = new ImageStorageServiceMock(DEFAULT_RESULT);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType(CONTENT_TYPE);
        request.setContent(buildContent("image.str", "200x200,330x330", DUMMY_CONTENT.getBytes()));
        SimpleUploadImageServlet servlet =
            new SimpleUploadImageServlet(
                new HttpUploadImageHelper(new HttpUploadImageProcessor(iss, Mockito.mock(MetricsLogger.class)),
                    new CmsUsersManagerMock(USER_ID), Mockito.mock(PermissionCheckHelper.class)),
                Mockito.mock(PermissionCheckHelper.class));
        servlet.process(request);
    }

    private static class ImageStorageServiceMock implements ImageStorageService {
        private final ImageDescription result;
        final List<String> uploaded = new ArrayList<>();

        ImageStorageServiceMock(ImageDescription result) {
            this.result = result;
        }

        @Override
        public ImageDescription uploadImage(byte[] data, String extension, long userId) {
            uploaded.add(new String(data));
            return result;
        }

        @Override
        public void addImage(ImageDescription imageDescription, byte[] data, String extension, long userId) {
            ImageDescription image = uploadImage(data, extension, userId);
            imageDescription.setUrl(image.getUrl());
            imageDescription.setWidth(image.getWidth());
            imageDescription.setHeight(image.getHeight());
        }
    }

    private static byte[] buildContent(String fileName, String thumb, byte[] content) {

        StringBuilder sb = new StringBuilder();

        if (thumb != null) {
            sb.append("--").append(BOUNDARY).append(CRLF);
            sb.append("Content-Disposition: form-data; name=\"thumb\"").append(CRLF);
            sb.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
            sb.append(CRLF).append(thumb).append(CRLF);
        }

        sb.append("--").append(BOUNDARY).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"file\"");
        if (fileName != null) {
            sb.append("; filename=\"").append(fileName).append('"');
        }
        sb.append(CRLF);
        sb.append("Content-Type: image/jpeg").append(CRLF);
        sb.append("Content-Transfer-Encoding: base64").append(CRLF);
        sb.append(CRLF).append(encodeBase64(content)).append(CRLF);

        // End of multipart/form-data.
        sb.append("--").append(BOUNDARY).append("--").append(CRLF);

        return sb.toString().getBytes();
    }

    private static byte[] picAsBytes(String picFile) throws IOException {
        return IOUtils.toByteArray(SimpleUploadImageServletTest.class.getClassLoader()
            .getResourceAsStream("pics/" + picFile));
    }

    private static String encodeBase64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }
}
