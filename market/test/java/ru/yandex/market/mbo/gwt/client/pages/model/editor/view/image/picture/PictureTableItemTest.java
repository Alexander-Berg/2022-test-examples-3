package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.image.picture;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 29.08.2018
 */
@RunWith(GwtMockitoTestRunner.class)
public class PictureTableItemTest {

    private PictureTableItem item;
    private Picture picture;

    @Before
    public void before() {
        item = new PictureTableItem(true);
        picture = new Picture();
        picture.setModificationSource(ModificationSource.OPERATOR_FILLED);

        item.setValue(picture);
        item.setIndex(0);
    }

    @Test
    public void dontChangeModificationSourceOnMoveUploadedPicture() {
        picture.setUploaded(true);
        item.setIndex(1);

        assertThat(picture.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void changeModificationSourceOnMoveExistPicture() {
        item.setIndex(1);

        assertThat(picture.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_COPIED);
    }

}
