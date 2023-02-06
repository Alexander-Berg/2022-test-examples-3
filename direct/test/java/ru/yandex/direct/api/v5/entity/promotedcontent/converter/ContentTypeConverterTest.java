package ru.yandex.direct.api.v5.entity.promotedcontent.converter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.promotedcontent.AddTypeEnum;
import com.yandex.direct.api.v5.promotedcontent.GetTypeEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class ContentTypeConverterTest {

    @Parameterized.Parameters(name = "Convert {0} to ContentPromotionContentType")
    private static Object[][] addTypeToCoreTypeParameters() {
        return new Object[][] {
                {null, null},
                {AddTypeEnum.VIDEO, ContentPromotionContentType.VIDEO},
                {AddTypeEnum.COLLECTION, ContentPromotionContentType.COLLECTION},
                {AddTypeEnum.SERVICE, ContentPromotionContentType.SERVICE},
                {AddTypeEnum.EDA, ContentPromotionContentType.EDA},
        };
    }

    @Test
    @Parameters(method = "addTypeToCoreTypeParameters")
    public void convertAddTypeToCoreType(
            @Nullable AddTypeEnum addTypeEnum,
            @Nullable ContentPromotionContentType expectedResult) {
        ContentPromotionContentType result = ContentTypeConverter.convertAddTypeToCoreType(addTypeEnum);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Parameterized.Parameters(name = "Convert {0} to GetTypeEnum")
    private static Object[][] coreTypeToGetTypeParameters() {
        return new Object[][] {
                {null, GetTypeEnum.UNKNOWN},
                {ContentPromotionContentType.VIDEO, GetTypeEnum.VIDEO},
                {ContentPromotionContentType.COLLECTION, GetTypeEnum.COLLECTION},
                {ContentPromotionContentType.SERVICE, GetTypeEnum.SERVICE},
                {ContentPromotionContentType.EDA, GetTypeEnum.EDA},
        };
    }

    @Test
    @Parameters(method = "coreTypeToGetTypeParameters")
    public void convertCoreTypeToGetType(
            @Nullable ContentPromotionContentType contentType,
            @Nullable GetTypeEnum expectedResult) {
        GetTypeEnum result = ContentTypeConverter.convertCoreTypeToGetType(contentType);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void convertAddTypeToCoreType_allTypesAreSupported() {
        for (AddTypeEnum addTypeEnum: AddTypeEnum.values()) {
            ContentPromotionContentType result = ContentTypeConverter.convertAddTypeToCoreType(addTypeEnum);
            assertThat(result).isNotNull();
        }
    }
}
