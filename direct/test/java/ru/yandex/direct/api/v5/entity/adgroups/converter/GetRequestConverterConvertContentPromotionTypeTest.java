package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.adgroups.PromotedContentTypeEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class GetRequestConverterConvertContentPromotionTypeTest {

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {PromotedContentTypeEnum.VIDEO, ContentPromotionAdgroupType.VIDEO},
                {PromotedContentTypeEnum.COLLECTION, ContentPromotionAdgroupType.COLLECTION},
                {PromotedContentTypeEnum.SERVICE, ContentPromotionAdgroupType.SERVICE},
        };
    }

    @Test
    @Parameters(method = "getParameters")
    public void convertContentPromotionType(
            PromotedContentTypeEnum typeEnum,
            ContentPromotionAdgroupType expectedType
    ) {
        assertThat(GetRequestConverter.convertContentPromotionType(typeEnum)).isEqualTo(expectedType);
    }

    @Test
    public void convertContentPromotionType_allTypesAreSupported() {
        for (PromotedContentTypeEnum addTypeEnum: PromotedContentTypeEnum.values()) {
            ContentPromotionAdgroupType result = GetRequestConverter.convertContentPromotionType(addTypeEnum);
            assertThat(result).isNotNull();
        }
    }
}
