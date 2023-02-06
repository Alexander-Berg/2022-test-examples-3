package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.yandex.direct.api.v5.adgroups.PromotedContentTypeGetEnum;
import com.yandex.direct.api.v5.general.AdGroupTypesEnum;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertContentPromotionAdGroupTypeToExternal;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertTypeToExternal;
import static ru.yandex.direct.core.entity.adgroup.container.AccessibleAdGroupTypes.API5_ALLOWED_AD_GROUP_TYPES;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class GetResponseConverterConvertTypeToExternalTest {

    @Test
    public void convertTypeToExternal_internalTypeBase_externalTypeTextAdGroup() {
        assertThat(convertTypeToExternal(AdGroupType.BASE)).isEqualTo(AdGroupTypesEnum.TEXT_AD_GROUP);
    }

    @Test
    public void convertTypeToExternal_internalTypeMobileContent_externalTypeMobileAppAdGroup() {
        assertThat(convertTypeToExternal(AdGroupType.MOBILE_CONTENT)).isEqualTo(AdGroupTypesEnum.MOBILE_APP_AD_GROUP);
    }

    @Test
    public void convertTypeToExternal_internalTypeDynamic_externalTypeDynamicTextAdGroup() {
        assertThat(convertTypeToExternal(AdGroupType.DYNAMIC)).isEqualTo(AdGroupTypesEnum.DYNAMIC_TEXT_AD_GROUP);
    }

    @Test
    public void convertTypeToExternal_internalTypeSmart_externalTypeSmartAdGroup() {
        assertThat(convertTypeToExternal(AdGroupType.PERFORMANCE)).isEqualTo(AdGroupTypesEnum.SMART_AD_GROUP);
    }

    @Test
    public void convertTypeToExternal_internalTypeContentPromotion_externalTypeContentPromotionAdGroup() {
        assertThat(convertTypeToExternal(AdGroupType.CONTENT_PROMOTION))
                .isEqualTo(AdGroupTypesEnum.CONTENT_PROMOTION_AD_GROUP);
    }

    @Test
    public void convertContentPromotionTypeToExternal_internalVideo_externalVideo() {
        assertThat(convertContentPromotionAdGroupTypeToExternal(ContentPromotionAdgroupType.VIDEO))
                .isEqualTo(PromotedContentTypeGetEnum.VIDEO);
    }

    @Test
    public void convertContentPromotionTypeToExternal_internalCollection_externalCollection() {
        assertThat(convertContentPromotionAdGroupTypeToExternal(ContentPromotionAdgroupType.COLLECTION))
                .isEqualTo(PromotedContentTypeGetEnum.COLLECTION);
    }

    @Test
    public void convertContentPromotionTypeToExternal_internalService_externalService() {
        assertThat(convertContentPromotionAdGroupTypeToExternal(ContentPromotionAdgroupType.SERVICE))
                .isEqualTo(PromotedContentTypeGetEnum.SERVICE);
    }

    @Test
    public void convertContentPromotionTypeToExternal_internalEda_externalUnknown() {
        assertThat(convertContentPromotionAdGroupTypeToExternal(ContentPromotionAdgroupType.EDA))
                .isEqualTo(PromotedContentTypeGetEnum.EDA);
    }

    @Test
    public void convertContentPromotionTypeToExternal_internalNull_externalUnknown() {
        assertThat(convertContentPromotionAdGroupTypeToExternal(null)).isEqualTo(PromotedContentTypeGetEnum.UNKNOWN);
    }

    @Test
    public void convertTypeToExternal_exceptionThrownOnNotSupportedType() {
        List<AdGroupType> nonApiAdGroupTypes = new ArrayList<>(Arrays.asList(AdGroupType.values()));
        nonApiAdGroupTypes.removeAll(API5_ALLOWED_AD_GROUP_TYPES);
        assumeThat(nonApiAdGroupTypes.size() > 0, is(true));
        Collections.shuffle(nonApiAdGroupTypes, ThreadLocalRandom.current());
        assertThatThrownBy(() -> convertTypeToExternal(nonApiAdGroupTypes.get(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported adgroup type: " + nonApiAdGroupTypes.get(0));
    }
}
