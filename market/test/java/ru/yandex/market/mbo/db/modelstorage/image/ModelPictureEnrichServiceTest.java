package ru.yandex.market.mbo.db.modelstorage.image;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.test.InjectResource;
import ru.yandex.market.mbo.test.InjectResources;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author dergachevfv
 * @since 10/11/19
 */
public class ModelPictureEnrichServiceTest {

    private static final String IMAGE_JPEG = "image/jpeg";

    @Rule
    public InjectResources resource = new InjectResources(this);

    @InjectResource("/mbo-core/test-image-1.jpeg")
    private byte[] picturebytes;

    private CommonModel model;
    private Picture picture;
    private ModelPictureEnrichService imageInitService;

    @Before
    public void setUp() throws IOException {
        picture = new Picture();
        picture.setUrlOrig("url");
        picture.setIsWhiteBackground(null);
        model = new CommonModel();
        model.setPictures(Collections.singletonList(picture));

        ImageDownloader imageDownloader = Mockito.mock(ImageDownloader.class);
        Mockito.when(imageDownloader.downloadImage(Mockito.anyString()))
            .thenReturn(new ImageData(picturebytes, IMAGE_JPEG));

        imageInitService = new ModelPictureEnrichService(imageDownloader);
    }

    @Test
    public void testPictureInitializationOk() {
        List<Picture> pics = imageInitService.encrichModelPictures(model);
        Assertions.assertThat(picture.isWhiteBackground()).isNotNull();
        Assertions.assertThat(pics).containsExactly(picture);
    }

    @Test
    public void testDoNotInitializeAlreadyInitialized() {
        picture.setIsWhiteBackground(true);
        Picture pictureSpied = Mockito.spy(picture);
        List<Picture> pics = imageInitService.encrichModelPictures(model);
        Assertions.assertThat(picture.isWhiteBackground()).isTrue();
        Mockito.verify(pictureSpied, Mockito.never()).setIsWhiteBackground(Mockito.anyBoolean());
        Assertions.assertThat(pics).isEmpty();
    }
}
