package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.adgroups.AdGroupSubtypeEnum;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.guessSubtype;

public class GetResponseConverterGuessSubtypeTest {

    @Test
    public void guessSubtype_textAdGroup_externalSubtypeNone() {
        assertThat(guessSubtype(new TextAdGroup())).isEqualTo(AdGroupSubtypeEnum.NONE);
    }

    @Test
    public void guessSubtype_dynamicTextAdGroup_externalSubtypeWebpage() {
        assertThat(guessSubtype(new DynamicTextAdGroup())).isEqualTo(AdGroupSubtypeEnum.WEBPAGE);
    }

    @Test
    public void guessSubtype_dynamicFeedAdGroup_externalSubtypeFeed() {
        assertThat(guessSubtype(new DynamicFeedAdGroup())).isEqualTo(AdGroupSubtypeEnum.FEED);
    }

    @Test
    public void guessSubtype_cpmBannerWithUserProfileAdGroup_externalSubtypeUserProfile() {
        assertThat(guessSubtype(new CpmBannerAdGroup().withCriterionType(CriterionType.USER_PROFILE)))
                .isEqualTo(AdGroupSubtypeEnum.USER_PROFILE);
    }

    @Test
    public void guessSubtype_cpmBannerWithKeywordsAdGroup_externalSubtypeKeywords() {
        assertThat(guessSubtype(new CpmBannerAdGroup().withCriterionType(CriterionType.KEYWORD)))
                .isEqualTo(AdGroupSubtypeEnum.KEYWORDS);
    }
}
