package ru.yandex.market.markup2.tasks.image_metrics;

import com.google.common.collect.Lists;
import org.junit.Test;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createModelWithPictures;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createPicture;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ImageUtilsTest {

    @Test(expected = IllegalArgumentException.class)
    public void getPicIndexFromXlsName() throws Exception {
//        assertEquals(0, ImageUtils.getPicIndexFromXlsName(ParamUtils.XL_PICTURE_XLS_NAME));
        assertEquals(11, ImageUtils.getPicIndexFromXlsName(ParamUtils.XL_PICTURE_XLS_NAME + "_" + 11));

        ImageUtils.getPicIndexFromXlsName(ParamUtils.XL_PICTURE_XLS_NAME + "_!ewe");
    }

    @Test
    public void getModelPicUrlByIndex() throws Exception {
        String mainUrl = "http://main";
        String baseUrl = "http://base_";
        ArrayList<String> otherPicUrls = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            otherPicUrls.add(baseUrl + (i + 1));
        }

        ModelStorage.Model dummyModel = ModelTestUtils.createDummyGuruModel(1, true,
                                                                                mainUrl, otherPicUrls);
        assertEquals(mainUrl, ImageUtils.getModelPicUrlByIndex(dummyModel, 0).get());

        for (int i = 0; i < 5; i++) {
            assertEquals(otherPicUrls.get(i), ImageUtils.getModelPicUrlByIndex(dummyModel, i + 1).get());
        }
    }

    @Test
    public void getPictureByIndex() throws Exception {
        ModelStorage.Model model = createModelWithPictures(100, 100L, 1L, "GURU", false,
            createPicture("XL-Picture", "http://image.url").build(),
            createPicture("XL-Picture_1", "http://image1.url").build()
        );

        assertEquals("XL-Picture", ImageUtils.getPictureByIndex(model, 0).get().getXslName());
        assertEquals("XL-Picture_1", ImageUtils.getPictureByIndex(model, 1).get().getXslName());
    }

    @Test
    public void getPreviewUrlNull() throws Exception {
        assertEquals(null, ImageUtils.getPreviewUrl(null));
    }

    @Test
    public void getPreviewUrlWithPrefix() throws Exception {
        String url = "https://avatars.mds.yandex.net/get-mpic/195452/img_id3609323044615421424/";
        assertEquals(url + "2hq", ImageUtils.getPreviewUrl(url + "orig"));
    }

    @Test
    public void getPreviewUrlWithNoPrefix() throws Exception {
        String url = "//avatars.mds.yandex.net/get-mpic/195452/img_id3609323044615421424/";
        assertEquals(url + "2hq", ImageUtils.getPreviewUrl(url + "orig"));
    }
}
