package ru.yandex.market.markup2.utils.model;

import org.junit.Test;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author inenakhov
 */
public class ModelUtilsTest {
    @Test
    public void isModelSuit() throws Exception {
        ModelStorage.Model publishedModel = ModelTestUtils.createDummyGuruModel(true);
        ModelStorage.Model unpublishedModel = ModelTestUtils.createDummyGuruModel(false);

        assertTrue(ModelUtils.isModelSuit(PublishingValue.ALL, publishedModel));
        assertTrue(ModelUtils.isModelSuit(PublishingValue.PUBLISHED, publishedModel));
        assertFalse(ModelUtils.isModelSuit(PublishingValue.UNPUBLISHED, publishedModel));

        assertTrue(ModelUtils.isModelSuit(PublishingValue.ALL, unpublishedModel));
        assertFalse(ModelUtils.isModelSuit(PublishingValue.PUBLISHED, unpublishedModel));
        assertTrue(ModelUtils.isModelSuit(PublishingValue.UNPUBLISHED, unpublishedModel));
    }

    @Test
    public void hasXLPictures() throws Exception {
        assertFalse(ModelUtils.hasXLPictures(ModelTestUtils.createDummyGuruModel(false)));
        ModelStorage.Model modelWithPic =
            ModelTestUtils.createDummyGuruModel(1, true, "http:/main", new ArrayList<>());
        assertTrue(ModelUtils.hasXLPictures(modelWithPic));
    }
}
