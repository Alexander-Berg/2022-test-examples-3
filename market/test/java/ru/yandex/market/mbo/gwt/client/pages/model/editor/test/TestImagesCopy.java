package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ImageCopyStartEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ImagesCopyRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ParamValuesChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelImages;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.CopyModelImagesPanelStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author york
 * @since 10.08.2017
 */
@SuppressWarnings({"checkstyle:magicnumber"})
public class TestImagesCopy extends AbstractImageTest {

    private static final String XL_PICTURE_2 = XslNames.XL_PICTURE + "_2";

    @Test
    public void testImagesCopy() {
        Picture xlPicture2 = new Picture();
        xlPicture2.setXslName(XL_PICTURE_2);
        xlPicture2.setWidth(100);
        xlPicture2.setHeight(200);
        xlPicture2.setUrl("BU1");
        xlPicture2.setUrlSource("BUS1");
        xlPicture2.setUrlOrig("BORIS1");

        Picture xlPicture = new Picture();
        xlPicture.setXslName(XslNames.XL_PICTURE);
        xlPicture.setWidth(101);
        xlPicture.setHeight(201);
        xlPicture.setUrl("URL1");
        xlPicture.setUrlSource("URLSOURCE1");
        xlPicture.setUrlOrig("ORIG1");

        CommonModel currentModification = createModelWithParentModelWithImages(2L, 1L,
            Arrays.asList(xlPicture), Arrays.asList(xlPicture2));

        Assert.assertTrue(view.isImageCopyVisible());

        CommonModel parent = currentModification.getParentModel();
        CommonModel anotherModification = new CommonModel(currentModification);
        anotherModification.setId(3);
        mutateParam(anotherModification, XslNames.XL_PICTURE, strMutator("XL2"));
        mutateParam(anotherModification, x(XslNames.XL_PICTURE), numMutator(102));
        mutateParam(anotherModification, y(XslNames.XL_PICTURE), numMutator(202));
        mutateParam(anotherModification, url(XslNames.XL_PICTURE), strMutator("UR2"));
        mutateParam(anotherModification, orig(XslNames.XL_PICTURE), strMutator("ORI2"));
        compare(currentModification, anotherModification, XslNames.XL_PICTURE, false);

        rpc.setGetModifications(Arrays.asList(currentModification, anotherModification), null);

        bus.fireEvent(new ImagesCopyRequestedEvent());

        CopyModelImagesPanelStub panel = (CopyModelImagesPanelStub) view.getPopupWidget();
        Assert.assertEquals(2, panel.getModelImages().size());
        for (ModelImages mi : panel.getModelImages()) {
            Assert.assertEquals(1, mi.getImages().size());
        }

        AtomicBoolean saveRequested = new AtomicBoolean();
        bus.subscribe(SaveModelRequest.class, event -> {
            saveRequested.set(true);
        });

        AtomicBoolean haveChanges = new AtomicBoolean();
        bus.subscribe(ParamValuesChangedEvent.class, event -> {
            haveChanges.set(true);
        });

        //copy bigpic from parent
        bus.fireEvent(new ImageCopyStartEvent(Collections.singletonMap(XL_PICTURE_2, parent)));
        Assert.assertFalse(saveRequested.get()); //assure not saved right now
        Assert.assertFalse(haveChanges.get()); //nothing changed cause big is inherited already
        bus.fireEvent(new ImageCopyStartEvent(Collections.singletonMap(XslNames.XL_PICTURE, anotherModification)));
        Assert.assertFalse(saveRequested.get()); //assure not saved right now
        Assert.assertTrue(haveChanges.get());

        rpc.setSaveModel(currentModification.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();
        compare(anotherModification, savedModel, XslNames.XL_PICTURE, true); //check xl changed
    }
}
