package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.image;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.AllModelPicturesLoadFailureEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.AllModelPicturesLoadSuccessEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueDeletedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueUpdatedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ModelPicturesAddedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ModelPicturesDeletedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.EditAllSkuPicturesRequestEvent;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.image.ModelPictureInfoUtils.modelPictureInfo;
import static ru.yandex.market.mbo.image.ModelPictureInfoUtils.picture;

/**
 * @author danfertev
 * @since 04.07.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelPictureInfoAddonTest extends BaseModelImageParamsAddonTest {
    private static final String XL_PICTURE_2 = "XL-Picture_2";
    private static final String XL_PICTURE_3 = "XL-Picture_3";

    private Picture xlPicture = picture(XslNames.XL_PICTURE, "xl_url", 101, 101, "xl_source", "xl_orig");
    private Picture xl2Picture = picture(XL_PICTURE_2, "xl2_url", 102, 102, "xl2_source", "xl2_orig");
    private Picture xl3Picture = picture(XL_PICTURE_3, "xl3_url", 103, 103, "xl3_source", "xl3_orig");

    private CommonModel ownSku;
    private CommonModel otherModif;
    private CommonModel otherSku;

    @Override
    public void model() {
        data.startModel()
            .title("Test model").id(10).category(11).vendorId(12)
            .currentType(CommonModel.Source.GURU)
            .modificationDate(new Date(1000000))
            .pictureParam(xlPicture)
            .pictureParam(xl3Picture)
            .picture(xlPicture)
            .picture(xl3Picture)
            .startParentModel()
            .title("Parent test model").id(11).category(11).vendorId(12)
            .currentType(CommonModel.Source.GURU)
            .modificationDate(new Date(1000000))
            .pictureParam(xl2Picture)
            .picture(xl2Picture)
            .endModel()

            .startModelRelation()
            .id(1001L)
            .categoryId(11L)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .startModel()
            .id(1001L)
            .currentType(CommonModel.Source.SKU)
            .category(11L)
            .vendorId(12L)
            .endModel()
            .endModelRelation()
            .endModel();

        ownSku = data.getModel().getRelation(1001L).get().getModel();
        otherSku = new CommonModel(ownSku);
        otherSku.setId(99L);
        otherSku.clearRelations();
        otherSku.addRelation(new ModelRelation(9L, 11L, ModelRelation.RelationType.SKU_PARENT_MODEL));

        otherModif = new CommonModel(data.getModel());
        otherModif.setId(9L);
        otherModif.clearRelations();
        ModelRelation toSku = new ModelRelation(99L, 11L, ModelRelation.RelationType.SKU_MODEL);
        toSku.setModel(otherSku);
        otherModif.addRelation(toSku);
    }

    @Test
    public void testLoadModelPictureInfoSuccess() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl2Picture, model.getParentModel()),
            modelPictureInfo(xlPicture, ownSku),
            modelPictureInfo(xl3Picture, otherSku)
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, new ArrayList<>()));
        assertThat(pictures).containsExactlyInAnyOrder(xl3Picture);

        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        assertThat(pictures).containsExactlyInAnyOrder(xl3Picture);
    }

    @Test
    public void testLoadModelPictureInfoSuccessCached() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Collections.emptyList());
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        assertThat(pictures).isEmpty();

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl2Picture, otherModif),
            modelPictureInfo(xlPicture, otherSku),
            modelPictureInfo(xl3Picture, otherSku)
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        assertThat(pictures).isEmpty();
    }

    @Test
    public void testLoadModelPictureInfoFailure() {
        MutableObject<String> message = new MutableObject<>();
        bus.subscribe(AllModelPicturesLoadFailureEvent.class, event -> {
            message.setValue(event.getMessage());
        });

        rpc.setModelPictureInfosThrowable(new RuntimeException("BOOM"));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(message.getValue()).isEqualTo("BOOM");
    }

    @Test
    public void testLoadModelPictureInfoFailureNotCached() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });
        MutableObject<String> message = new MutableObject<>();
        bus.subscribe(AllModelPicturesLoadFailureEvent.class, event -> {
            message.setValue(event.getMessage());
        });

        rpc.setModelPictureInfosThrowable(new RuntimeException("BOOM"));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).isEmpty();
        assertThat(message.getValue()).isEqualTo("BOOM");

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl2Picture, model.getParentModel()),
            modelPictureInfo(xlPicture, otherModif),
            modelPictureInfo(xl3Picture, otherSku)
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xlPicture, xl3Picture);
    }

    @Test
    public void testClearCachedAfterSuccessfulSave() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Collections.emptyList());
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).isEmpty();

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl2Picture, model.getParentModel()),
            modelPictureInfo(xlPicture, otherModif),
            modelPictureInfo(xl3Picture, otherSku)
        ));
        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest());
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xlPicture, xl3Picture);
    }

    @Test
    public void testPictureAddedAfterLoad() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Collections.singletonList(
            modelPictureInfo(xl2Picture, model.getParentModel())
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        assertThat(pictures).isEmpty();

        bus.fireEvent(new ModelPicturesAddedEvent(otherModif, Collections.singletonList(xlPicture)));
        bus.fireEvent(new ModelPicturesAddedEvent(otherSku, Collections.singletonList(xl3Picture)));
        bus.fireEvent(new ModelPicturesAddedEvent(model.getParentModel(), Collections.singletonList(xl2Picture)));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xlPicture, xl3Picture);
    }

    @Test
    public void testPictureAddedBeforeLoad() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Collections.singletonList(
            modelPictureInfo(xl2Picture, model.getParentModel())
        ));
        bus.fireEvent(new ModelPicturesAddedEvent(otherModif, Collections.singletonList(xlPicture)));
        bus.fireEvent(new ModelPicturesAddedEvent(otherSku, Collections.singletonList(xl3Picture)));
        bus.fireEvent(new ModelPicturesAddedEvent(model.getParentModel(), Collections.singletonList(xl2Picture)));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xlPicture, xl3Picture);
    }

    @Test
    public void testPictureDeleted() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl2Picture, model.getParentModel()),
            modelPictureInfo(xl3Picture, otherSku),
            modelPictureInfo(xl3Picture, otherModif)
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        bus.fireEvent(new ModelPicturesDeletedEvent(model, Collections.singletonList(xl3Picture)));
        bus.fireEvent(new ModelPicturesDeletedEvent(model.getParentModel(), Collections.singletonList(xl2Picture)));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xl3Picture);
    }

    @Test
    public void testPictureAddedOnPictureTabBeforeLoad() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl3Picture, model.getParentModel()),
            modelPictureInfo(xl2Picture, otherModif)
        ));
        bus.fireEvent(new ImageValueUpdatedEvent(xlPicture.getXslName(), xlPicture));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xl2Picture);
    }

    @Test
    public void testPictureAddedOnPictureTabAfterLoad() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl3Picture, model.getParentModel()),
            modelPictureInfo(xl2Picture, otherModif)
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        bus.fireEvent(new ImageValueUpdatedEvent(xlPicture.getXslName(), xlPicture));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xl2Picture);
    }

    @Test
    public void testPictureDeletedOnPictureTab() {
        List<Picture> pictures = new ArrayList<>();
        bus.subscribe(AllModelPicturesLoadSuccessEvent.class, event -> {
            pictures.clear();
            pictures.addAll(event.getAllModelPictures());
        });

        rpc.setModelPictureInfos(Arrays.asList(
            modelPictureInfo(xl2Picture, model.getParentModel()),
            modelPictureInfo(xlPicture, otherModif),
            modelPictureInfo(xl3Picture, otherSku)
        ));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));
        bus.fireEvent(new ImageValueDeletedEvent(xlPicture.getXslName()));
        bus.fireEvent(new EditAllSkuPicturesRequestEvent(false, Collections.singletonList(ownSku)));

        assertThat(pictures).containsExactlyInAnyOrder(xlPicture, xl3Picture);
    }
}
