package ru.yandex.market.mbo.cms.core.utils.http;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
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
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.ImageDescription;
import ru.yandex.market.mbo.cms.core.models.UploadImageResult;
import ru.yandex.market.mbo.cms.core.service.CmsUsersManagerMock;
import ru.yandex.market.mbo.cms.core.service.user.UserRole;

import static org.apache.commons.fileupload.FileUploadBase.MULTIPART_FORM_DATA;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class UploadImageServletTest {
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
        request.setContent(buildContent("image.png", null, picAsBytes(PIC_FILE)));
        UploadImageServlet servlet =
            new UploadImageServlet(
                new HttpUploadImageHelper(
                    new HttpUploadImageProcessor(iss, Mockito.mock(MetricsLogger.class)),
                    new CmsUsersManagerMock(USER_ID), Mockito.mock(PermissionCheckHelper.class)),
                Mockito.mock(PermissionCheckHelper.class)
            );
        JsonPageApiResponse result = servlet.process(request);
        Assert.assertNotNull(result.getJsonObject());
        Assert.assertThat(result.getJsonObject(), instanceOf(UploadImageResult.class));
        UploadImageResult uploadImageResult = (UploadImageResult) result.getJsonObject();
        List<ImageDescription> imageDescriptions = uploadImageResult.getResult();
        Assert.assertEquals(1, imageDescriptions.size());
        Assert.assertNotNull(imageDescriptions.get(0).getUrl());
        Assert.assertNull(imageDescriptions.get(0).getErrorMessage());
        Assert.assertEquals(Collections.singletonList(new String(picAsBytes(PIC_FILE))), iss.uploaded);
    }

    @Test
    public void testWrongFormat() {
        ImageStorageServiceMock iss = new ImageStorageServiceMock(DEFAULT_RESULT);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType(CONTENT_TYPE);
        request.setContent(buildContent("image.str", null, DUMMY_CONTENT.getBytes()));
        UploadImageServlet servlet =
            new UploadImageServlet(
                new HttpUploadImageHelper(new HttpUploadImageProcessor(iss, Mockito.mock(MetricsLogger.class)),
                    new CmsUsersManagerMock(USER_ID), Mockito.mock(PermissionCheckHelper.class)),
                Mockito.mock(PermissionCheckHelper.class));
        JsonPageApiResponse result = servlet.process(request);
        Assert.assertNotNull(result.getJsonObject());
        Assert.assertThat(result.getJsonObject(), instanceOf(UploadImageResult.class));
        UploadImageResult uploadImageResult = (UploadImageResult) result.getJsonObject();
        List<ImageDescription> imageDescriptions = uploadImageResult.getResult();
        Assert.assertEquals(1, imageDescriptions.size());

        Assert.assertNotNull(imageDescriptions.get(0).getErrorMessage());
        Assert.assertEquals(Collections.emptyList(), iss.uploaded);
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

    private static byte[] buildContent(String fileName, String param, byte[] content) {

        StringBuilder sb = new StringBuilder();

        if (param != null) {
            sb.append("--").append(BOUNDARY).append(CRLF);
            sb.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
            sb.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
            sb.append(CRLF).append(param).append(CRLF);
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
        return IOUtils.toByteArray(UploadImageServletTest.class.getClassLoader()
            .getResourceAsStream("pics/" + picFile));
    }

    private static String encodeBase64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }
}
