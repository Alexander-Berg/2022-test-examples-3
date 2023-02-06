package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.picture;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.util.Assert;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.image.ModelPicturesAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku.BaseSkuAddonTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.EditorTabSwitchedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.DeleteAllImagesRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.NewPicturesMovedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.PictureExportToPickerRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.PictureMoveRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.save.PopulateModelSaveSyncEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PicturesTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectParameterValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectPicturesList;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.Role;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModelUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.SubType;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестирование {@link ModelPicturesAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelPicturesAddonTest extends BaseSkuAddonTest {

    private Picture pic1;
    private Picture pic2;

    @Override
    public void model() {
        data.startModel()
            .title("Test model")
            .id(2).category(666).vendorId(777).currentType(CommonModel.Source.SKU)
            .picture("pic://2_1")
            .picture("pic://2_2")
            .parameterValues(1, "param1", 11)
            .parameterValues(101, "param101", 1011, 1012)
            .startModelRelation()
                .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .startModel()
                    .id(1).category(666).currentType(CommonModel.Source.GURU)
                    .picture("XL-Picture", "pic://1_2")
                    .picture("XL-Picture_2", "pic://1_3")
                    .startParameterValue()
                        .xslName("XL-Picture").paramId(1001).words("pic://1_2")
                    .endParameterValue()
                    .startParameterValue()
                        .xslName("XL-Picture_2").paramId(1002).words("pic://1_3")
                    .endParameterValue()
                    .startModelRelation()
                        .id(2).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Override
    public void parameters() {
        super.parameters();
        data.startParameters()
            .startParameter()
                .id(100).xsl("param100").type(Param.Type.ENUM).subType(SubType.IMAGE_PICKER).name("Enum100")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .level(CategoryParam.Level.OFFER)
                .option(1001, "Option1001")
                .option(1002, "Option1002")
                .option(1003, "Option1003")
            .endParameter()
            .startParameter()
                .id(101).xsl("param101").type(Param.Type.ENUM).subType(SubType.IMAGE_PICKER).name("Enum101")
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
                .level(CategoryParam.Level.OFFER)
                .multifield(true)
                .option(1011, "Option1011")
                .option(1012, "Option1012")
                .option(1013, "Option1013")
            .endParameter()
            .endParameters();
    }

    @Override
    protected void onModelLoaded(EditableModel editableModel) {
        super.onModelLoaded(editableModel);

        sku = editableModel.getModel();
        pic1 = sku.getPictures().stream().filter(p -> p.getUrl().equals("pic://2_1")).findFirst().get();
        pic2 = sku.getPictures().stream().filter(p -> p.getUrl().equals("pic://2_2")).findFirst().get();
    }

    @Test
    public void testTabCreated() {
        PicturesTab tab = view.getTab(EditorTabs.PICTURES.getDisplayName(), PicturesTab.class);

        assertThat(tab.getPictures().stream().map(Picture::getUrl))
            .containsExactly("pic://2_1", "pic://2_2");
    }

    @Test
    public void testPicturesCorrectlySaved() {
        Picture newPicture = new Picture();
        newPicture.setUrl("pic://new");

        PicturesTab tab = view.getTab(EditorTabs.PICTURES.getDisplayName(), PicturesTab.class);
        tab.setPictures(Stream.concat(tab.getPictures().stream(), Stream.of(newPicture)).collect(Collectors.toList()));

        CommonModel model = new CommonModel(editableModel.getModel());
        bus.fireEvent(new PopulateModelSaveSyncEvent(model));

        assertPictures(tab.getPictures(), "pic://2_1", "pic://2_2", "pic://new");
    }

    @Test
    public void testDeleteAllPicturesButtonIsVisibleOnlyOnPicturesTab() {
        assertThat(view.isDeleteAllImagesVisible()).isFalse();


        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteAllImagesVisible()).isFalse();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PICTURES.getDisplayName()));
        assertThat(view.isDeleteAllImagesVisible()).isTrue();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.ADDITIONAL.getDisplayName()));
        assertThat(view.isDeleteAllImagesVisible()).isFalse();
    }

    @Test
    public void testDeleteButtonVisibleOnlyOnParametersTab() {
        assertThat(view.isDeleteButtonVisible()).isFalse();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isTrue();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PICTURES.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isFalse();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.ADDITIONAL.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isFalse();
    }

    @Test
    public void testDeleteButtonVisibleOnlyForAdminAndSuperoperators() {
        assertThat(view.isDeleteButtonVisible()).isFalse();
        //  not enough rights to see the button
        view.getUser().setRole(Role.GUEST);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isFalse();
        view.getUser().setRole(Role.OPERATOR);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isFalse();
        view.getUser().setRole(Role.GWT_OPERATOR);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isFalse();
        view.getUser().setRole(Role.VISUAL_OPERATOR);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isFalse();

        //  admin
        view.getUser().setRole(Role.ADMIN);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isTrue();
        //  super operators
        view.getUser().setRole(Role.VISUAL_SUPER_OPERATOR);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isTrue();
        view.getUser().setRole(Role.GWT_SUPER_OPERATOR);
        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isDeleteButtonVisible()).isTrue();
    }

    @Test
    public void testImportPicturesButtonIsVisibleOnlyOnPicturesTab() {
        assertThat(view.isMovePicturesButtonVisible()).isFalse();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PARAMETERS.getDisplayName()));
        assertThat(view.isMovePicturesButtonVisible()).isFalse();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.PICTURES.getDisplayName()));
        assertThat(view.isMovePicturesButtonVisible()).isTrue();

        bus.fireEvent(new EditorTabSwitchedEvent(EditorTabs.ADDITIONAL.getDisplayName()));
        assertThat(view.isMovePicturesButtonVisible()).isFalse();
    }

    @Test
    public void testImportPicturesPopupOpen() {
        bus.fireEvent(new PictureMoveRequestEvent());

        SelectPicturesList selectPicturesList = (SelectPicturesList) view.getDialogWidget();
        List<Picture> pictures = selectPicturesList.getPictures();

        assertThat(pictures.stream().map(Picture::getUrl))
            .containsExactlyInAnyOrder("pic://1_2", "pic://1_3");
    }

    @Test
    public void testMovePictures() {
        CommonModel baseModel = CommonModelUtils.getBaseModel(data.getModel());
        Picture picture1 = baseModel.getPictures().get(0);
        Picture picture2 = baseModel.getPictures().get(1);

        bus.fireEvent(new NewPicturesMovedEvent(Arrays.asList(picture1, picture2)));

        // проверяем, что в виджете появились новые картинки
        PicturesTab tab = view.getTab(EditorTabs.PICTURES.getDisplayName(), PicturesTab.class);
        assertThat(tab.getPictures().stream().map(Picture::getUrl))
            .containsExactlyInAnyOrder("pic://2_1", "pic://2_2", "pic://1_2", "pic://1_3");
    }

    @Test
    public void testBaseModelWillBeAlsoSavedIfUserMovedPicturesFromItToSkuModel() {
        rpc.setSaveModel(2L, null);

        CommonModel baseModel = CommonModelUtils.getBaseModel(data.getModel());
        Picture picture1 = baseModel.getPictures().get(0);
        Picture picture2 = baseModel.getPictures().get(1);

        bus.fireEvent(new NewPicturesMovedEvent(Arrays.asList(picture1, picture2)));

        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();
        CommonModel savedBaseModel = CommonModelUtils.getBaseModel(savedModel);

        Assert.notNull(savedBaseModel);
        assertThat(savedModel.getPictures().stream().map(Picture::getUrl))
            .containsExactlyInAnyOrder("pic://2_1", "pic://2_2", "pic://1_2", "pic://1_3");
        assertThat(savedBaseModel.getPictures().stream().map(Picture::getUrl)).isEmpty();
    }

    @Test
    public void testModelWontResaveIfAllPicturesDelete() {
        rpc.setLoadModelFail(() -> new AssertionError("Load model don't expected to be call"));
        rpc.setSaveModelFail(() -> new AssertionError("Save model don't expected to be call"));
        rpc.setDeletePicturesFail(() -> new AssertionError("Delete pictures don't expected to be call"));

        bus.fireEvent(new DeleteAllImagesRequestEvent());

        rpc.setSaveModel(2L, null);
        rpc.setLoadModel(editableModel.getModel(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();
        assertThat(savedModel.getPictures()).isEmpty();
    }

    @Test
    public void testOpeningExportToPickerPopup() {
        Picture picture = model.getPictures().get(0);
        bus.fireEvent(new PictureExportToPickerRequestEvent(picture));

        SelectParameterValueWidget selectParameterValueWidget = (SelectParameterValueWidget) view.getDialogWidget();
        Assertions.assertThat(selectParameterValueWidget.getValues())
            .containsOnlyElementsOf(model.getParameterValues(101).getValues());
    }

    @Test
    public void testPicturesWontSyncForModelsWithoutParamPictures() {
        Picture newPic1 = new Picture(pic1);
        newPic1.setHeight(1000);

        Picture newPic2 = new Picture(pic2);
        newPic2.setWidth(1000);

        PicturesTab tab = view.getTab(EditorTabs.PICTURES.getDisplayName(), PicturesTab.class);
        tab.setPictures(Stream.of(newPic1, newPic2).collect(Collectors.toList()));

        rpc.setSaveModel(model.getId(), null);

        bus.fireEvent(new SaveModelRequest());
        CommonModel savedModel = rpc.getSavedModel();

        Assertions.assertThat(savedModel.getPictures()).containsExactly(
            picture("pic://2_1", CommonModelBuilder.PIC_SIZE, 1000, null, "pic://2_1"),
            picture("pic://2_2", 1000, CommonModelBuilder.PIC_SIZE, null, "pic://2_2")
        );
    }

    private void assertPictures(Collection<Picture> actualPictures, String... expectedUrls) {
        assertThat(actualPictures.stream().map(Picture::getUrl))
            .containsExactly(expectedUrls);
    }

    private Picture picture(String url, Integer width, Integer height, String urlSource, String urlOrig) {
        return PictureBuilder.newBuilder(url, width, height)
            .setUrlSource(urlSource)
            .setUrlOrig(urlOrig)
            .setModificationSource(ModificationSource.OPERATOR_FILLED)
            .build();
    }
}
