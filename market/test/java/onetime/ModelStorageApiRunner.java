package onetime;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelCardAutoGenerationApiServiceStub;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;

import java.io.IOException;
import java.io.InputStream;

/**
 * Утилита (не авто-тест) для ручной проверки ручек ModelStorage и ModelCardAutoGenerationApi.
 */
@Ignore
public class ModelStorageApiRunner {
    private static final String MODEL_STORAGE_HOST =
        "http://cs-clusterizer01vt.market.yandex.net:33714/modelStorage/";
    private static final String MODEL_CARD_AUTO_GENERATION_API_HOST =
        "http://cs-clusterizer01vt.market.yandex.net:33714/ModelCardAutoGenerationApiService/";

    private static final long CATEGORY_ID = 91529; // Спорт и отдых/Велоспорт/Велосипеды
    private static final long VENDOR_MODEL_ID = 100100143829L;

    private static final ModelStorage.ModificationSource VALUE_SOURCE =
        ModelStorage.ModificationSource.VENDOR_OFFICE;
    private static final long USER_ID = 28027378;

    private static final long   NAME_PARAM_ID       = 7351771;
    private static final String NAME_PARAM_XSL_NAME = "name";

    private static final long   VENDOR_PARAM_ID       = 7893318;
    private static final String VENDOR_PARAM_XSL_NAME = "vendor";

    private static final int STRING_TYPE_ID = 4;
    private static final MboParameters.ValueType STRING_VALUE_TYPE = MboParameters.ValueType.STRING;

    private static final int ENUM_TYPE_ID = 1;
    private static final MboParameters.ValueType ENUM_VALUE_TYPE = MboParameters.ValueType.ENUM;

    private static final ModelStorageServiceStub MODEL_STORAGE_SERVICE =
        new ModelStorageServiceStub();
    static {
        MODEL_STORAGE_SERVICE.setHost(MODEL_STORAGE_HOST);
        MODEL_STORAGE_SERVICE.setUserAgent("local-dev-version");
    }

    private static final ModelCardAutoGenerationApiServiceStub MODEL_CARD_AUTO_GENERATION_API_SERVICE =
        new ModelCardAutoGenerationApiServiceStub();
    static {
        MODEL_CARD_AUTO_GENERATION_API_SERVICE.setHost(MODEL_CARD_AUTO_GENERATION_API_HOST);
        MODEL_STORAGE_SERVICE.setUserAgent("local-dev-version");
    }

    @Test
    public void updateModelPictures() {
        ModelStorage.Model.Builder model = loadModel();
        model.clearParameterValues();
        model.clearPictures();

        // Без этих двух параметров модель не сохраняется, хотя возвращает "OK"
        setMandatoryModelParameters(model);

        saveModel(model.build());

        ModelStorage.OperationResponse operationResponse = MODEL_STORAGE_SERVICE.uploadImages(
            ModelStorage.UploadImageRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setModificationSource(VALUE_SOURCE)
                .addUploadImageData(ModelStorage.UploadImageData.newBuilder()
                    .setModelId(model.getId())
                    .addImageData(ModelStorage.ImageData.newBuilder()
                        .setId(0)
                        .setUrl("000.ru/a.jpg")
                        .setContentType("image/jpeg")
                        .setContentBytes(loadPicture("a.jpg"))
                        .setImageParameter(ModelStorage.ImageParameter.XL_PICTURE)
                        .setSaveType(ModelStorage.ImageSaveType.APPEND)
                    )
                    .addImageData(ModelStorage.ImageData.newBuilder()
                        .setId(1)
                        .setUrl("000.ru/b.jpg")
                        .setContentType("image/jpeg")
                        .setContentBytes(loadPicture("b.jpg"))
                        .setImageParameter(ModelStorage.ImageParameter.XL_PICTURE)
                        .setSaveType(ModelStorage.ImageSaveType.APPEND)
                    )
                    .addImageData(ModelStorage.ImageData.newBuilder()
                        .setId(1)
                        .setUrl("000.ru/c.jpg")
                        .setContentType("image/jpeg")
                        .setContentBytes(loadPicture("c.jpg"))
                        .setImageParameter(ModelStorage.ImageParameter.XL_PICTURE)
                        .setSaveType(ModelStorage.ImageSaveType.APPEND)
                    )
                    .addImageData(ModelStorage.ImageData.newBuilder()
                        .setId(1)
                        .setUrl("000.ru/d.jpg")
                        .setContentType("image/jpeg")
                        .setContentBytes(loadPicture("d.jpg"))
                        .setImageParameter(ModelStorage.ImageParameter.XL_PICTURE)
                        .setSaveType(ModelStorage.ImageSaveType.APPEND)
                    )
                )
                .build()
        );

        System.out.println(operationResponse);
    }

    @Test
    public void saveModelClearParameters() {
        ModelStorage.Model.Builder model = loadModel();
        model.clearParameterValues();
        model.clearPictures();

        // Без этих двух параметров модель не сохраняется, хотя возвращает "OK"
        setMandatoryModelParameters(model);

        System.out.println(model.build());

        ModelStorage.OperationResponse operationResponse = saveModel(model.build());

        System.out.println(operationResponse);
    }


    @Test
    public void testLoadModel() throws IOException {
        ModelStorage.Model model = MODEL_STORAGE_SERVICE.getModels(
            ModelStorage.GetModelsRequest.newBuilder()
                .setCategoryId(765280)
                .addModelIds(7870852L)
                .build()
        ).getModels(0);
        JsonFormat.print(model, System.out);
    }

    private ModelStorage.Model.Builder loadModel() {
        return ModelStorage.Model.newBuilder(
            MODEL_STORAGE_SERVICE.getModels(
                ModelStorage.GetModelsRequest.newBuilder()
                    .setCategoryId(CATEGORY_ID)
                    .addModelIds(VENDOR_MODEL_ID)
                    .build()
            ).getModels(0)
        );
    }

    private ModelStorage.OperationResponse saveModel(ModelStorage.Model model) {
        return MODEL_CARD_AUTO_GENERATION_API_SERVICE.saveAutoGeneratedModels(
            ModelCardApi.SaveAutoGeneratedModelsRequest.newBuilder()
                .setUserId(USER_ID)
                .setCategoryId((int) model.getCategoryId())
                .addModels(model)
                .build()
        );
    }

    /**
     * Без этих двух параметров модель не сохраняется, хотя возвращает "OK".
     */
    private void setMandatoryModelParameters(ModelStorage.Model.Builder model) {
        model
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(NAME_PARAM_ID)
                .setXslName(NAME_PARAM_XSL_NAME)
                .setValueType(STRING_VALUE_TYPE)
                .setTypeId(STRING_TYPE_ID)
                .addStrValue(model.getTitles(0))
                .setValueSource(VALUE_SOURCE)
                .setUserId(USER_ID)
            )
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(VENDOR_PARAM_ID)
                .setXslName(VENDOR_PARAM_XSL_NAME)
                .setValueType(ENUM_VALUE_TYPE)
                .setTypeId(ENUM_TYPE_ID)
                .setOptionId((int) model.getVendorId())
                .setValueSource(VALUE_SOURCE)
                .setUserId(USER_ID)
            );
    }

    private ByteString loadPicture(String fileName) {
        try {
            InputStream imageStream = this.getClass().getClassLoader()
                .getResourceAsStream(fileName);
            return ByteString.copyFrom(ByteStreams.toByteArray(imageStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
