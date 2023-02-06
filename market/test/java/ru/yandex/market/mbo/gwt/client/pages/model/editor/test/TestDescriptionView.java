package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.DescriptionBlockView;

/**
 * @author s-ermakov
 */
public class TestDescriptionView extends AbstractTest {

    @Test
    public void testHiddenView() throws Exception {
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        assertView(view.getDescriptionView(), "", "");
    }

    @Test
    public void testHiddenViewWithInvalidToken() throws Exception {
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("invalid", "entity-id=1")));

        assertView(view.getDescriptionView(), "", "");
    }

    @Test
    public void testViewWithText() throws Exception {
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1&text=aaa")));

        assertView(view.getDescriptionView(), "aaa", "");
    }

    @Test
    public void testViewWithDescription() throws Exception {
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1&descr=bbb")));

        assertView(view.getDescriptionView(), "", "bbb");
    }

    @Test
    public void testViewWithTextAndDescription() throws Exception {
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1&text=aaa&descr=bbb")));

        assertView(view.getDescriptionView(), "aaa", "bbb");
    }

    @Test
    public void testViewWithCreationAnchor() throws Exception {
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelCreator", "entity-id=1&text=aaa&descr=bbb")));

        assertView(view.getDescriptionView(), "aaa", "bbb");
    }

    @Test
    public void testCorrectDecodeSymbols() throws Exception {
        String encodedString = "%D0%BC%D0%B0%D0%BC%D0%B0%2C%D0%BF%D0%B0%D0%BF%D0%B0" +
            "%2C%D1%8F%3D%D1%81%D1%87%D0%B0%D1%81%D1%82%D0%BB%D0%B8" +
            "%D0%B2%D0%B0%D1%8F%20%D1%81%D0%B5%D0%BC%D1%8C%D1%8F%3F";

        String decodedString = "мама,папа,я=счастливая семья?";

        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1&descr=" + encodedString)));

        assertView(view.getDescriptionView(), "", decodedString);
    }

    private void assertView(DescriptionBlockView descriptionBlockView, String text, String description) {
        Assert.assertEquals(text, descriptionBlockView.getText());
        Assert.assertEquals(description, descriptionBlockView.getDescription());
    }
}
