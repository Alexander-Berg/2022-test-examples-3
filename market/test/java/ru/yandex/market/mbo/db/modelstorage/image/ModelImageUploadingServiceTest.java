package ru.yandex.market.mbo.db.modelstorage.image;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.image.ImageUploadContext;
import ru.yandex.market.mbo.image.ModelImageService;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 16.02.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelImageUploadingServiceTest {

    private static final int ERROR_LIST_INDEX = 3;
    private static final int SEED = 481231037;

    private ModelImageUploadingService uploadingService;

    @Mock
    private ModelImageService imageService;

    @Mock
    private ImageDownloader imageDownloader;

    private Random random;

    @Before
    public void before() throws IOException {
        uploadingService = new ModelImageUploadingService(
            imageService,
            null,
            imageDownloader
        );

        when(imageDownloader.downloadImage(anyString())).then((Answer<ImageData>) invocation -> createJpegData());
        random = new Random(SEED);
    }

    @Test
    public void testReuploadPicture() throws IOException {
        Picture picture = createPictureWithOrig("xl_picture", XslNames.XL_PICTURE);
        Picture uploadedPicture = PictureBuilder.newBuilder().setUrl("http://xl_picture/uploaded").build();
        CommonModel model = modelWith(picture);

        when(imageService.uploadPicture(any(), any(), any(), any())).thenReturn(uploadedPicture);

        OperationStatus operation = new OperationStatus(OperationStatusType.OK, OperationType.UPLOAD_IMAGE,
            model.getId());

        Picture reuploadPicture = uploadingService.reuploadPicture(new ImageUploadContext(),
            picture.getUrlOrig(), picture.getUrlSource(), picture.getXslName(), operation
        );

        assertThat(reuploadPicture).isNotNull();
        assertThat(reuploadPicture.getUrl()).isEqualTo(uploadedPicture.getUrl());
        assertThat(reuploadPicture.getXslName()).isEqualTo(XslNames.XL_PICTURE);
        assertThat(reuploadPicture.getUrlSource()).isEqualTo(picture.getUrlSource());
        assertThat(operation.getStatus()).isEqualTo(OperationStatusType.OK);
    }

    @Test
    public void testReuploadPictureError() throws IOException {
        Picture picture = createPictureWithOrig("xl_picture");
        CommonModel model = modelWith(picture);

        when(imageService.uploadPicture(any(), any(), any(), anyList())).thenAnswer(invocation -> {
            List<ModelValidationError> errorList = invocation.getArgument(ERROR_LIST_INDEX);
            errorList.add(new ModelValidationError(model.getId(), ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT));
            return null;
        });

        OperationStatus operation = new OperationStatus(OperationStatusType.OK, OperationType.UPLOAD_IMAGE,
            model.getId());
        Picture reuploadPicture = uploadingService.reuploadPicture(new ImageUploadContext(),
            picture.getUrlOrig(), picture.getUrlSource(), picture.getXslName(), operation
        );

        assertThat(reuploadPicture).isNull();
        assertThat(operation.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
        assertThat(operation.getValidationErrors()).hasSize(1);
        assertThat(operation.getValidationErrors().get(0).getType())
            .isEqualTo(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT);
    }

    private static Picture createPicture(String urlId, String xslName) {
        Picture picture = new Picture();
        String baseUrl = "http://" + urlId;
        picture.setUrl(baseUrl);
        picture.setXslName(xslName);
        picture.setUrlSource(baseUrl + "/source");

        return picture;
    }

    private static Picture createPictureWithOrig(String urlId, String xslName) {
        Picture picture = createPicture(urlId, xslName);
        picture.setUrlOrig(picture.getUrl() + "/orig");
        return picture;
    }

    private static Picture createPictureWithOrig(String urlId) {
        return createPictureWithOrig(urlId, null);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private ImageData createJpegData() {
        byte[] data = new byte[5];
        random.nextBytes(data);
        return new ImageData(data, "image/jpeg");
    }

    private CommonModel modelWith(Picture... pictures) {
        CommonModelBuilder<Object> builder = modelBuilder();
        Stream.of(pictures).forEach(builder::picture);
        return builder.getModel();
    }

    private CommonModelBuilder<Object> modelBuilder() {
        int id = 0;
        return ParametersBuilder.startParameters(CommonModelBuilder::model)
            .startParameter()
            .id(++id)
            .xsl(XslNames.XL_PICTURE)
            .type(Param.Type.STRING)
            .endParameter()
            .startParameter()
            .id(++id)
            .xsl(XslNames.XL_PICTURE + "_2")
            .type(Param.Type.STRING)
            .endParameter()
            .endParameters();
    }
}
