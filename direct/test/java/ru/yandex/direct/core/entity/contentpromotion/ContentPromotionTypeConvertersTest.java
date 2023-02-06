package ru.yandex.direct.core.entity.contentpromotion;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.contentpromotion.ContentPromotionTypeConverters.contentPromotionContentTypeToContentPromotionAdgroupType;

@RunWith(Parameterized.class)
public class ContentPromotionTypeConvertersTest {
    @Parameterized.Parameters(name = "content_type = {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {ContentPromotionContentType.COLLECTION, ContentPromotionAdgroupType.COLLECTION},
                {ContentPromotionContentType.VIDEO, ContentPromotionAdgroupType.VIDEO},
                {ContentPromotionContentType.SERVICE, ContentPromotionAdgroupType.SERVICE},
        });
    }

    @Parameterized.Parameter
    public ContentPromotionContentType contentPromotionContentType;

    @Parameterized.Parameter(1)
    public ContentPromotionAdgroupType contentPromotionAdgroupType;

    @Test
    public void contentPromotionContentTypeToContentPromotionAdgroupTypeTest() {
        assertThat(contentPromotionContentTypeToContentPromotionAdgroupType(contentPromotionContentType),
                is(contentPromotionAdgroupType));
    }
}
