package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import com.yandex.direct.api.v5.ads.AdTypeEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class GetRequestConverterConvertToContentPromotionTypesTest {

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {List.of(AdTypeEnum.CONTENT_PROMOTION_VIDEO_AD), List.of(ContentPromotionAdgroupType.VIDEO)},
                {List.of(AdTypeEnum.CONTENT_PROMOTION_COLLECTION_AD), List.of(ContentPromotionAdgroupType.COLLECTION)},
                {List.of(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD), List.of(ContentPromotionAdgroupType.SERVICE)},
                {List.of(AdTypeEnum.CONTENT_PROMOTION_EDA_AD), List.of(ContentPromotionAdgroupType.EDA)},
                {List.of(AdTypeEnum.values()), List.of(ContentPromotionAdgroupType.VIDEO,
                        ContentPromotionAdgroupType.SERVICE, ContentPromotionAdgroupType.COLLECTION,
                        ContentPromotionAdgroupType.EDA)},
        };
    }

    @Test
    @Parameters(method = "getParameters")
    public void convertToContentPromotionTypes(
            List<AdTypeEnum> typesEnum,
            List<ContentPromotionAdgroupType> expectedTypes
    ) {
        assertThat(GetRequestConverter.convertToContentPromotionTypes(typesEnum))
                .containsExactlyInAnyOrder(expectedTypes.toArray(new ContentPromotionAdgroupType[0]));
    }
}
