package ru.yandex.direct.core.entity.contentpromotion.type.collection;

import org.junit.Test;

import ru.yandex.direct.env.EnvironmentType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionUtils.convertToPreviewUrl;

public class ContentPromotionCollectionUtilsTest {

    @Test
    public void convertToPreviewUrlTest() {
        String thumbId = "1995089/0c2b5fb3-1791-4788-a141-1cad182391e8";
        String expectedTestingPreviewUrl = "https://avatars-int.mds.yandex.net/" +
                "get-pdb-teasers-test/1995089/0c2b5fb3-1791-4788-a141-1cad182391e8/thumb";
        assertThat(convertToPreviewUrl(thumbId, EnvironmentType.TESTING), is(expectedTestingPreviewUrl));
    }

    @Test
    public void convertToPreviewUrlProductionTest() {
        String thumbId = "1995089/0c2b5fb3-1791-4788-a141-1cad182391e8";
        String expectedTestingPreviewUrl = "https://avatars.mds.yandex.net/" +
                "get-pdb-preview/1995089/0c2b5fb3-1791-4788-a141-1cad182391e8/thumb";
        assertThat(convertToPreviewUrl(thumbId, EnvironmentType.PRODUCTION), is(expectedTestingPreviewUrl));
    }
}
