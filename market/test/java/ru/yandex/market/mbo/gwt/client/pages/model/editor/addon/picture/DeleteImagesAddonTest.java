package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.picture;

import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.image.DeleteImagesAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.DeleteAllImagesRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.DeletePicturesRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueDeletedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageValueWidgetDeleteRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test of {@link DeleteImagesAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class DeleteImagesAddonTest extends AbstractModelTest {
    private Set<String> deletedPictureXslNames = new HashSet<>();

    @Override
    public void model() {
        data.startModel()
            .title("Test model")
            .id(1).category(666).currentType(CommonModel.Source.GURU)
                .modificationDate(new Date(1000000))
                .picture("XL-Picture", "pic://1_2")
                .picture("XL-Picture_2", "pic://1_3")
                .startParameterValue()
                    .xslName("XL-Picture").paramId(1001).words("pic://1_2")
                .endParameterValue()
                .startParameterValue()
                    .xslName("XL-Picture_2").paramId(1002).words("pic://1_3")
                .endParameterValue()
            .endModel();
    }

    @Test
    public void testAllPicturesDeleted() {
        bus.subscribe(ImageValueDeletedEvent.class, event -> {
            deletedPictureXslNames.add(event.getXslName());
        });

        bus.fireEvent(new DeleteAllImagesRequestEvent());
        assertThat(deletedPictureXslNames).containsExactlyInAnyOrder("XL-Picture", "XL-Picture_2");
    }

    @Test
    public void testImageValueWidgetRequestSent() {
        bus.subscribe(ImageValueWidgetDeleteRequestEvent.class, event -> {
            deletedPictureXslNames.add(event.getXslName());
        });

        bus.fireEvent(new DeletePicturesRequestEvent(model,
            Arrays.asList(picture(XslNames.XL_PICTURE))));
        assertThat(deletedPictureXslNames).containsExactlyInAnyOrder(XslNames.XL_PICTURE);
    }

    @Test
    public void testImageValueWidgetRequestNotSentIfNotEditableModel() {
        bus.subscribe(ImageValueWidgetDeleteRequestEvent.class, event -> {
            deletedPictureXslNames.add(event.getXslName());
        });

        CommonModel sku = SkuBuilderHelper.getDefaultSkuBuilder().getModel();

        bus.fireEvent(new DeletePicturesRequestEvent(sku,
            Arrays.asList(picture(XslNames.XL_PICTURE))));
        assertThat(deletedPictureXslNames).isEmpty();
    }

    private Picture picture(String xslName) {
        Picture pic = new Picture();
        pic.setXslName(xslName);
        return pic;
    }
}
