package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageReorderRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageReorderShiftEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.ImageReorderedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static ru.yandex.market.mbo.utils.MboAssertions.assertThat;

@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class TestImagesReordering extends AbstractImageTest {
    private static final Picture XL_PICTURE1 = picture(XL_PICTURE_1, 101, 201, "URL1", "URLSOURCE1", "ORIG1", 0.1, 0.2, false);
    private static final Picture XL_PICTURE2 = picture(XL_PICTURE_2, 102, 202, "URL2", "URLSOURCE2", "ORIG2", 0.1, 0.3, true);
    private static final Picture XL_PICTURE3 = picture(XL_PICTURE_3, 103, 203, "URL3", "URLSOURCE3", "ORIG3", 0.4, 0.2, null);
    private static final Picture XL_PICTURE4 = picture(XL_PICTURE_4, null, null, null, null, null, 0.6, 0.7, null);

    //TODO - fix test, it fails after moving to arcadia and didn't start in github because of bad regexp:
    //https://a.yandex-team.ru/arc_vcs/market/mbo/mbo-catalog/build.gradle?rev=r9351105#L178,
    @Test
    @Ignore
    public void testImageSwapping() {
        CommonModel model = createModelWithImages(1L, asList(XL_PICTURE1, XL_PICTURE2, XL_PICTURE3));
        BlockWidget imageBlock = (BlockWidget) ((ParamsTab) view.getTab(EditorTabs.PICTURES.getDisplayName())).getWidgetsAtLeft().get(0);
        Assert.assertEquals(15, imageBlock.getWidgets().size());
        assertDistinct(imageBlock.getWidgets());

        Object widget1value = getWidgetValue(XL_PICTURE_1);
        Object widget2value = getWidgetValue(XL_PICTURE_2);
        Object widget3value = getWidgetValue(XL_PICTURE_3);

        bus.fireEvent(new ImageReorderRequestedEvent(XL_PICTURE_1, XL_PICTURE_2,
            ImageReorderRequestedEvent.Direction.SWAP));

        Assert.assertEquals(widget1value, getWidgetValue(XL_PICTURE_2));
        Assert.assertEquals(widget2value, getWidgetValue(XL_PICTURE_1));
        Assert.assertEquals(widget3value, getWidgetValue(XL_PICTURE_3));

        // save model
        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();

        // check new values in model
        assertThat(savedModel, XL_PICTURE_1).values("URL2");
        assertThat(savedModel, XL_PICTURE_2).values("URL1");
        assertThat(savedModel, XL_PICTURE_3).values("URL3");
        assertThat(savedModel, x(XL_PICTURE_1)).values(102);
        assertThat(savedModel, y(XL_PICTURE_2)).values(201);
        assertThat(savedModel, orig(XL_PICTURE_3)).values("ORIG3");

        Assert.assertEquals("URL2", savedModel.getPicture(XL_PICTURE_1).getUrl());
        Assert.assertEquals(true, savedModel.getPicture(XL_PICTURE_1).isWhiteBackground());
        Assert.assertEquals("URL1", savedModel.getPicture(XL_PICTURE_2).getUrl());
        Assert.assertEquals(false, savedModel.getPicture(XL_PICTURE_2).isWhiteBackground());
        Assert.assertEquals("URL3", savedModel.getPicture(XL_PICTURE_3).getUrl());
    }

    @Test
    public void testSwapPictureWithEmptyPlace() {
        CommonModel model = createModelWithImages(1L, asList(XL_PICTURE1, XL_PICTURE2, XL_PICTURE3, XL_PICTURE4));
        BlockWidget imageBlock = (BlockWidget) ((ParamsTab) view.getTab(EditorTabs.PICTURES.getDisplayName())).getWidgetsAtLeft().get(0);
        Assert.assertEquals(20, imageBlock.getWidgets().size());
        assertDistinct(imageBlock.getWidgets());

        Object widget1value = getWidgetValue(XL_PICTURE_1);
        Object widget2value = getWidgetValue(XL_PICTURE_2);
        Object widget3value = getWidgetValue(XL_PICTURE_3);
        Object widget4value = getWidgetValue(XL_PICTURE_4);

        bus.fireEvent(new ImageReorderRequestedEvent(XL_PICTURE_1, XL_PICTURE_4,
            ImageReorderRequestedEvent.Direction.SWAP));

        Assert.assertEquals(widget4value, getWidgetValue(XL_PICTURE_1));
        Assert.assertEquals(widget2value, getWidgetValue(XL_PICTURE_2));
        Assert.assertEquals(widget3value, getWidgetValue(XL_PICTURE_3));
        Assert.assertEquals(widget1value, getWidgetValue(XL_PICTURE_4));

        // save model
        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();

        assertThat(savedModel, XL_PICTURE_1).notExists();
        assertThat(savedModel, XL_PICTURE_2).values("URL2");
        assertThat(savedModel, XL_PICTURE_3).values("URL3");
        assertThat(savedModel, XL_PICTURE_4).values("URL1");
        Assert.assertEquals("URL2", savedModel.getPicture(XL_PICTURE_2).getUrl());
        Assert.assertEquals("URL3", savedModel.getPicture(XL_PICTURE_3).getUrl());
        Assert.assertEquals("URL1", savedModel.getPicture(XL_PICTURE_4).getUrl());
        Assert.assertNull(savedModel.getPicture(XL_PICTURE_1));
    }

    @Test
    public void testImageShiftingWithoutEmptySpace() {
        CommonModel model = createModelWithImages(1L, asList(XL_PICTURE1, XL_PICTURE2, XL_PICTURE3));
        Assert.assertEquals(15, getWidgetsWithImageParams().size());
        assertDistinct(getWidgetsWithImageParams());

        Object widget1value = getWidgetValue(XL_PICTURE_1);
        Object widget2value = getWidgetValue(XL_PICTURE_2);
        Object widget3value = getWidgetValue(XL_PICTURE_3);

        // Attempting to shift widgets if there is no empty space
        AtomicInteger numberOfChangedWidgets = new AtomicInteger();

        bus.subscribe(ImageReorderedEvent.class, event -> {
            numberOfChangedWidgets.set(event.getReorderedImages().size());
        });

        bus.fireEvent(new ImageReorderRequestedEvent(XL_PICTURE_2, null,
            ImageReorderRequestedEvent.Direction.ALL_DOWN));

        // check that no widgets have changed
        Assert.assertEquals(0, numberOfChangedWidgets.get());

        Assert.assertEquals(widget1value, getWidgetValue(XL_PICTURE_1));
        Assert.assertEquals(widget2value, getWidgetValue(XL_PICTURE_2));
        Assert.assertEquals(widget3value, getWidgetValue(XL_PICTURE_3));

        // save model
        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();

        // check that model has not changed
        compare(model, savedModel, XL_PICTURE_1, true);
        compare(model, savedModel, XL_PICTURE_2, true);
        compare(model, savedModel, XL_PICTURE_3, true);
    }

    @Test
    public void testImageShiftingWithEmptySpace() {
        CommonModel model = createModelWithImages(1L, asList(XL_PICTURE1, XL_PICTURE2, XL_PICTURE3, XL_PICTURE4));
        Assert.assertEquals(20, getWidgetsWithImageParams().size());
        assertDistinct(getWidgetsWithImageParams());

        AtomicInteger numberOfChangedWidgets = new AtomicInteger();

        bus.subscribe(ImageReorderedEvent.class, event -> {
            numberOfChangedWidgets.set(event.getReorderedImages().size());
        });

        bus.fireEvent(new ImageReorderRequestedEvent(XL_PICTURE_1, null,
            ImageReorderRequestedEvent.Direction.ALL_DOWN));

        Assert.assertEquals(4, numberOfChangedWidgets.get());

        // check widget content changes
        Assert.assertNull(getWidgetValue(XL_PICTURE_1));
        Assert.assertNull(getWidgetValue(x(XL_PICTURE_1)));
        Assert.assertNull(getWidgetValue(y(XL_PICTURE_1)));

        Assert.assertEquals("URL1", getWidgetValue(XL_PICTURE_2));
        Assert.assertEquals("101", getWidgetValue(x(XL_PICTURE_2)));
        Assert.assertEquals("201", getWidgetValue(y(XL_PICTURE_2)));

        Assert.assertEquals("URL2", getWidgetValue(XL_PICTURE_3));
        Assert.assertEquals("102", getWidgetValue(x(XL_PICTURE_3)));
        Assert.assertEquals("202", getWidgetValue(y(XL_PICTURE_3)));

        Assert.assertEquals("URL3", getWidgetValue(XL_PICTURE_4));
        Assert.assertEquals("103", getWidgetValue(x(XL_PICTURE_4)));
        Assert.assertEquals("203", getWidgetValue(y(XL_PICTURE_4)));

        // save model
        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();

        assertThat(savedModel, XL_PICTURE_2).values("URL1");
        assertThat(savedModel, XL_PICTURE_3).values("URL2");
        assertThat(savedModel, XL_PICTURE_4).values("URL3");
        assertThat(savedModel, XL_PICTURE_1).notExists();
        Assert.assertEquals("URL1", savedModel.getPicture(XL_PICTURE_2).getUrl());
        Assert.assertEquals("URL2", savedModel.getPicture(XL_PICTURE_3).getUrl());
        Assert.assertEquals("URL3", savedModel.getPicture(XL_PICTURE_4).getUrl());
        Assert.assertNull(savedModel.getPicture(XL_PICTURE_1));

        assertThat(savedModel, orig(XL_PICTURE_2)).values("ORIG1");
        assertThat(savedModel, x(XL_PICTURE_3)).values(102);
        assertThat(savedModel, y(XL_PICTURE_4)).values(203);

        assertThat(savedModel, y(XL_PICTURE_1)).notExists();
        assertThat(savedModel, url(XL_PICTURE_1)).notExists();
        assertThat(savedModel, orig(XL_PICTURE_1)).notExists();
    }

    @Test
    public void testCollapseOnMoveToSku() {
        CommonModel model = createModelWithImages(1L, asList(XL_PICTURE1, XL_PICTURE2, XL_PICTURE3, XL_PICTURE4));
        Assert.assertEquals(20, getWidgetsWithImageParams().size());
        assertDistinct(getWidgetsWithImageParams());

        //Нарочно поменяем местами 2 и 4 пикчи, чтобы второй оказался пустым, якобы перенесён в ску.
        bus.fireEvent(new ImageReorderRequestedEvent(XL_PICTURE_2, XL_PICTURE_4,
            ImageReorderRequestedEvent.Direction.SWAP));

        Object widget1value = getWidgetValue(XL_PICTURE_1);
        Object widget3value = getWidgetValue(XL_PICTURE_3);
        Object widget4value = getWidgetValue(XL_PICTURE_4);

        //Делаем вид, что пикчу 2 скопировали в ску
        bus.fireEvent(new ImageReorderShiftEvent(Collections.singletonList(XL_PICTURE_2)));
        //На место второй картинки должны съехать 3 и 4
        Assert.assertNull(getWidgetValue(XL_PICTURE_4));
        Assert.assertNull(getWidgetValue(x(XL_PICTURE_4)));
        Assert.assertNull(getWidgetValue(y(XL_PICTURE_4)));

        Assert.assertEquals(widget1value, getWidgetValue(XL_PICTURE_1));
        Assert.assertEquals(widget3value, getWidgetValue(XL_PICTURE_2));
        Assert.assertEquals(widget4value, getWidgetValue(XL_PICTURE_3));

        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();

        assertThat(savedModel, XL_PICTURE_1).values("URL1");
        assertThat(savedModel, XL_PICTURE_2).values("URL3");
        assertThat(savedModel, XL_PICTURE_3).values("URL2");
        assertThat(savedModel, XL_PICTURE_4).notExists();
        Assert.assertEquals("URL1", savedModel.getPicture(XL_PICTURE_1).getUrl());
        Assert.assertEquals("URL3", savedModel.getPicture(XL_PICTURE_2).getUrl());
        Assert.assertEquals("URL2", savedModel.getPicture(XL_PICTURE_3).getUrl());
        Assert.assertNull(savedModel.getPicture(XL_PICTURE_4));
    }

    @Test
    public void testCollapseWithInheritedPictures() {
        CommonModel model = createModelWithParentModelWithImages(1L, 0L,
            asList(XL_PICTURE1, XL_PICTURE2, XL_PICTURE4),
            Collections.singletonList(XL_PICTURE3));

        Assert.assertEquals(20, getWidgetsWithImageParams().size());
        assertDistinct(getWidgetsWithImageParams());

        //Нарочно поменяем местами 2 и 4 пикчи, чтобы второй оказался пустым, якобы перенесён в ску.
        bus.fireEvent(new ImageReorderRequestedEvent(XL_PICTURE_2, XL_PICTURE_4,
            ImageReorderRequestedEvent.Direction.SWAP));

        Object widget1value = getWidgetValue(XL_PICTURE_1);
        Object widget3value = getWidgetValue(XL_PICTURE_3);
        Object widget4value = getWidgetValue(XL_PICTURE_4);

        //Делаем вид, что пикчу 2 скопировали в ску
        bus.fireEvent(new ImageReorderShiftEvent(Collections.singletonList(XL_PICTURE_2)));
        //На место второй картинки должна съехать 4, но 3 остаётся на месте, ибо наследуется.
        Assert.assertNull(getWidgetValue(XL_PICTURE_4));
        Assert.assertNull(getWidgetValue(x(XL_PICTURE_4)));
        Assert.assertNull(getWidgetValue(y(XL_PICTURE_4)));

        Assert.assertEquals(widget1value, getWidgetValue(XL_PICTURE_1));
        Assert.assertEquals(widget3value, getWidgetValue(XL_PICTURE_3));
        Assert.assertEquals(widget4value, getWidgetValue(XL_PICTURE_2));

        rpc.setSaveModel(model.getId(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();
        savedModel.getParentModel().updatePicturesFromParams();

        assertThat(savedModel, XL_PICTURE_1).values("URL1");
        assertThat(savedModel, XL_PICTURE_2).values("URL2");
        assertThat(savedModel.getParentModel(), XL_PICTURE_3).values("URL3");
        assertThat(savedModel, XL_PICTURE_4).notExists();
        Assert.assertEquals("URL1", savedModel.getPicture(XL_PICTURE_1).getUrl());
        Assert.assertEquals("URL2", savedModel.getPicture(XL_PICTURE_2).getUrl());
        Assert.assertEquals("URL3", savedModel.getParentModel().getPicture(XL_PICTURE_3).getUrl());
        Assert.assertNull(savedModel.getPicture(XL_PICTURE_4));
    }

    private Object getWidgetValue(String xslName) {
        Object value = getWidgetsWithImageParams().stream()
            .filter(w -> w.getParamMeta().getXslName().equals(xslName))
            .findFirst()
            .map(w -> w.getValuesWidget().getFirstValueWidget())
            .filter(Objects::nonNull)
            .map(ValueWidget::getValue)
            .orElse(null);
        if (value instanceof List) {
            if (((List) value).isEmpty()) {
                return null;
            }
            return ((List) value).get(0);
        } else {
            return value;
        }
    }

    private List<ParamWidget<?>> getWidgetsWithImageParams() {
        BlockWidget imageBlock = (BlockWidget) ((ParamsTab) view.getTab(EditorTabs.PICTURES.getDisplayName())).getWidgetsAtLeft().get(0);
        return imageBlock.getWidgets();
    }

    private static void assertDistinct(List objectList) {
        for (int i = 0; i < objectList.size() - 1; ++i) {
            for (int j = i + 1; j < objectList.size(); ++j) {
                Assert.assertNotEquals(objectList.get(i), objectList.get(j));
            }
        }
    }
}
