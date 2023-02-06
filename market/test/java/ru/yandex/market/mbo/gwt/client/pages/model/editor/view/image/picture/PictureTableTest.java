package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.image.picture;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 29.08.2018
 */
@RunWith(GwtMockitoTestRunner.class)
public class PictureTableTest {

    private PictureTable table;
    private Picture picture1;
    private Picture picture2;
    private Picture picture3;

    @Before
    public void before() {
        table = new PictureTable(true);
        table.pictureTable = new GridStub();

        picture1 = picture("picture-1");
        picture2 = picture("picture-2");
        picture3 = picture("picture-3");
    }

    @Test
    public void movePictureDown() {
        table.setValue(Arrays.asList(picture1, picture2, picture3));

        table.getPictureTableItems().get(0).moveDownButtonClick(null);

        assertThat(table.getValue()).containsExactly(picture2, picture1, picture3);
    }

    @Test
    public void movePictureUp() {
        table.setValue(Arrays.asList(picture1, picture2, picture3));

        table.getPictureTableItems().get(2).moveUpButtonClick(null);

        assertThat(table.getValue()).containsExactly(picture1, picture3, picture2);
    }

    @Test
    public void movingExistPicturesChangeModificationSource() {
        table.setValue(Arrays.asList(picture1, picture2, picture3));

        table.getPictureTableItems().get(1).moveUpButtonClick(null);

        assertThat(picture1.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_COPIED);
        assertThat(picture2.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_COPIED);
        assertThat(picture3.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void movingUploadedPictureDontChangeModificationSource() {
        picture2.setUploaded(true);
        table.setValue(Arrays.asList(picture1, picture2, picture3));

        table.getPictureTableItems().get(1).moveUpButtonClick(null);

        assertThat(picture1.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_COPIED);
        assertThat(picture2.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_FILLED);
        assertThat(picture3.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_FILLED);
    }

    private static Picture picture(String url) {
        return PictureBuilder.newBuilder()
            .setUrl(url)
            .setModificationSource(ModificationSource.OPERATOR_FILLED)
            .build();
    }
}
