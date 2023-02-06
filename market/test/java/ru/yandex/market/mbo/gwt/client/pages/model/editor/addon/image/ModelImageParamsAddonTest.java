package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.image;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.AddPicturesRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueDeletedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueUpdatedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueWidgetDeleteRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueWidgetUpdateRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ModelPicturesAddedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ModelPicturesDeletedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты {@link ModelImageParamsAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelImageParamsAddonTest extends BaseModelImageParamsAddonTest {
    private static final String XL_PICTURE_3 = "XL-Picture_3";

    @Override
    public void model() {
        data.startModel()
            .title("Test model").id(10).category(11).vendorId(12)
            .currentType(CommonModel.Source.GURU)
            .modificationDate(new Date(1000000))
            .pictureParam(XslNames.XL_PICTURE, "xl_picture", 2, 2,
                          "source", "orig", 0.1, 0.3)
            .pictureParam(XL_PICTURE_3, "xl_picture_3", 3, 3, "source_3", "orig_3")
            .picture(PictureBuilder.newBuilder()
                    .setXslName(XslNames.XL_PICTURE)
                    .setUrl("xl_picture")
                    .setHeight(2)
                    .setWidth(2)
                    .setUrlSource("source")
                    .setUrlOrig("orig")
                    .setIsWhiteBackground(true))
            .picture(XL_PICTURE_3, "xl_picture_3", 3, 3, "source_3", "orig_3",
                     ModificationSource.OPERATOR_FILLED, 0.1, 0.3)
            .startParentModel()
                .title("Parent test model").id(11).category(11).vendorId(12)
                .currentType(CommonModel.Source.GURU)
                .modificationDate(new Date(1000000))
                .pictureParam(XL_PICTURE_2, "xl_picture_2", 2, 2,
                             "source_2", "orig_2")
                .picture(XL_PICTURE_2, "xl_picture_2", 2, 2,
                        "source_2", "orig_2")
            .endModel()
            .endModel();
    }

    @Test
    public void testPicturesWillSyncForModelsWithParamPictures() {
        // меняем размеры у xlPicture
        String xlPictureHeightXslName = ImageType.XL_PICTURE.getHeightParamName(XslNames.XL_PICTURE);
        ValueFieldStub<String> xlPictureHeightField = getValueFieldStub(xlPictureHeightXslName);

        xlPictureHeightField.setValueUserInput("1000");

        rpc.setSaveModel(model.getId(), null);

        bus.fireEvent(new SaveModelRequest());
        CommonModel savedModel = rpc.getSavedModel();

        assertThat(savedModel.getPictures()).containsExactly(
            picture(XslNames.XL_PICTURE, "xl_picture", 1000, 2, "source",
                    "orig", 0.1, 0.3, true),
            picture(XL_PICTURE_3, "xl_picture_3", 3, 3, "source_3",
                    "orig_3", null, null, null)
        );
    }

    @Test
    public void testImageWidgetUpdatedOnUpdateRequest() {
        bus.fireEventSync(new ImageValueWidgetUpdateRequestEvent(XslNames.XL_PICTURE,
            picture(XslNames.XL_PICTURE, "url1", 100, 100, "url_source",
                    "url_orig", null, null, null)));

        MboAssertions.assertThat(getWidgetValue(XslNames.XL_PICTURE)).values("url1");
    }

    @Test
    public void testImageWidgetUpdatedOnDeleteRequest() {
        bus.fireEventSync(new ImageValueWidgetDeleteRequestEvent(XslNames.XL_PICTURE));

        MboAssertions.assertThat(getWidgetValue(XslNames.XL_PICTURE)).isEmpty();
    }

    @Test
    public void testImageValueUpdateEventSent() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(ImageValueUpdatedEvent.class, event -> pictures.add(event.getPicture()));

        Picture picture1 = picture(XslNames.XL_PICTURE, "url", 100, 100,
                "url_source", "url_orig", null, null, null);
        Picture picture2 = picture(XL_PICTURE_2, "url1", 101, 101,
                "url_source1", "url_orig1", null, null, null);

        bus.fireEventSync(new ImageValueWidgetUpdateRequestEvent(XslNames.XL_PICTURE, picture1));
        bus.fireEventSync(new ImageValueWidgetUpdateRequestEvent(XL_PICTURE_2, picture2));

        assertThat(pictures).containsExactlyInAnyOrder(picture1, picture2);
    }

    @Test
    public void testImageValueDeleteEventSent() {
        List<String> xslNames = new ArrayList<>();
        bus.subscribe(ImageValueDeletedEvent.class, event -> xslNames.add(event.getXslName()));

        bus.fireEventSync(new ImageValueWidgetDeleteRequestEvent(XslNames.XL_PICTURE));
        bus.fireEventSync(new ImageValueWidgetDeleteRequestEvent(XL_PICTURE_3));

        assertThat(xslNames).containsExactlyInAnyOrder(XslNames.XL_PICTURE, XL_PICTURE_3);
    }

    @Test
    public void testRelatedDataUpdatedOnImageDelete() {
        bus.fireEventSync(new ImageValueDeletedEvent(XslNames.XL_PICTURE));

        MboAssertions.assertThat(getWidgetValue(ImageType.getHeightXslName(XslNames.XL_PICTURE)))
            .isEmpty();
        MboAssertions.assertThat(getWidgetValue(ImageType.getWidthXslName(XslNames.XL_PICTURE)))
            .isEmpty();
        MboAssertions.assertThat(getWidgetValue(ImageType.getUrlOrigXslName(XslNames.XL_PICTURE)))
            .isEmpty();

        assertThat(model.getPictures().stream().map(Picture::getXslName))
            .containsExactlyInAnyOrder(XL_PICTURE_3);
    }

    @Test
    public void testRelatedDataUpdatedOnImageUpdate() {
        bus.fireEventSync(new ImageValueWidgetDeleteRequestEvent(XslNames.XL_PICTURE));

        assertThat(model.getPictures().stream().map(Picture::getXslName))
            .containsExactlyInAnyOrder(XL_PICTURE_3);

        Picture pic = picture(XslNames.XL_PICTURE, "updated_url", 333, 666, "updated_source",
                "updated_orig", 0.1, 0.3, true);
        bus.fireEventSync(new ImageValueUpdatedEvent(XslNames.XL_PICTURE, pic));


        ImageType.getAllAttrNames(XslNames.XL_PICTURE).forEach(paramName -> {
            MboAssertions.assertThat(getWidgetValue(paramName))
                    .valuesWithTypeRecognition(getValueFromPictureByParamName(XslNames.XL_PICTURE, paramName, pic));
        });

        assertThat(model.getPictures().stream().map(Picture::getXslName))
            .containsExactlyInAnyOrder(XslNames.XL_PICTURE, XL_PICTURE_3);
    }

    @Test
    public void testSendImageUpdateRequest() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AddPicturesRequestEvent.class, event -> pictures.addAll(event.getPictures()));

        Picture picture1 = picture(XslNames.XL_PICTURE, "updated_url", 333, 666,
            "updated_source", "updated_orig", null, null, null);
        Picture picture2 = picture(XL_PICTURE_2, "updated_url2", 444, 777,
            "updated_source2", "updated_orig2", null, null, null);

        bus.fireEventSync(new AddPicturesRequestEvent(Arrays.asList(picture1, picture2)));

        assertThat(pictures).containsExactlyInAnyOrder(picture1, picture2);
    }

    @Test
    public void testSendModelPicturesDeletedEventFromModel() {
        List<CommonModel> updatedModels = new ArrayList<>();
        List<Picture> actualDeletedPictures = new ArrayList<>();
        bus.subscribe(ModelPicturesDeletedEvent.class, event -> {
            updatedModels.add(event.getModel());
            actualDeletedPictures.addAll(event.getPictures());
        });
        Picture deletedPicture = model.getPicture(XslNames.XL_PICTURE);
        bus.fireEventSync(new ImageValueDeletedEvent(XslNames.XL_PICTURE));

        assertThat(updatedModels).containsExactlyInAnyOrder(model);
        assertThat(actualDeletedPictures).containsExactlyInAnyOrder(deletedPicture);
    }

    @Test
    public void testSendModelPicturesDeletedEventFromParentModel() {
        List<CommonModel> updatedModels = new ArrayList<>();
        List<Picture> actualDeletedPictures = new ArrayList<>();
        bus.subscribe(ModelPicturesDeletedEvent.class, event -> {
            updatedModels.add(event.getModel());
            actualDeletedPictures.addAll(event.getPictures());
        });
        Picture deletedPicture = model.getParentModel().getPicture(XL_PICTURE_2);
        bus.fireEventSync(new ImageValueDeletedEvent(XL_PICTURE_2));

        assertThat(updatedModels).containsExactlyInAnyOrder(model.getParentModel());
        assertThat(actualDeletedPictures).containsExactlyInAnyOrder(deletedPicture);
    }

    @Test
    public void testSendModelPicturesAddedEvent() {
        List<CommonModel> updatedModels = new ArrayList<>();
        List<Picture> actualAddedPictures = new ArrayList<>();
        bus.subscribe(ModelPicturesAddedEvent.class, event -> {
            updatedModels.add(event.getModel());
            actualAddedPictures.addAll(event.getPictures());
        });
        bus.fireEventSync(new ImageValueDeletedEvent(XslNames.XL_PICTURE));

        Picture addedPicture = picture(XslNames.XL_PICTURE, "url_added", 111, 111, "url_source_added",
            "url_orig_added", null, null, null);
        bus.fireEventSync(new ImageValueUpdatedEvent(XslNames.XL_PICTURE, addedPicture));

        assertThat(updatedModels).containsExactlyInAnyOrder(model);
        assertThat(actualAddedPictures).containsExactlyInAnyOrder(addedPicture);
    }

    private Object getValueFromPictureByParamName(String xslName,
                                                  String paramName,
                                                  Picture picture) throws RuntimeException {
        if (ImageType.getUrlSourceXslName(xslName).equals(paramName)) {
            return picture.getUrlSource();
        } else if (ImageType.getUrlOrigXslName(xslName).equals(paramName)) {
            return picture.getUrlOrig();
        } else if (ImageType.getWidthXslName(xslName).equals(paramName)) {
            return picture.getWidth();
        } else if (ImageType.getHeightXslName(xslName).equals(paramName)) {
            return picture.getHeight();
        } else if (ImageType.getColornessXslName(xslName).equals(paramName)) {
            return picture.getColorness();
        } else if (ImageType.getColornessAvgXslName(xslName).equals(paramName)) {
            return picture.getColornessAvg();
        } else {
            throw new RuntimeException("Это поле не поддерживается. " +
                                       "Добавьте его в эту функцию и убедитесь, что поддержали во все тестах");
        }
    }

    private ParameterValues getWidgetValue(String xslName) {
        return editableModel.getEditableParameter(xslName).getEditableValues().getParameterValues();
    }

    @SuppressWarnings("unchecked")
    private ValueFieldStub<String> getValueFieldStub(String xslName) {
        return (ValueFieldStub) editableModel
            .getEditableParameter(xslName)
            .getEditableValues()
            .getEditableValue(0)
            .getValueWidget()
            .getValueField();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private Picture picture(String xslName, String url, Integer height, Integer width,
                           String urlSource, String urlOrig, Double colorness, Double colornessAvg,
                            Boolean isWhiteBackground) {
        return PictureBuilder.newBuilder(xslName, url, width, height)
            .setUrlSource(urlSource)
            .setUrlOrig(urlOrig)
            .setColorness(colorness)
            .setColornessAvg(colornessAvg)
            .setIsWhiteBackground(isWhiteBackground)
            .build();
    }
}
