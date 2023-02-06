package ru.yandex.direct.core.entity.banner.type.language;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.direct.queryrec.QueryrecService;
import ru.yandex.direct.queryrec.model.Language;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentLanguageWithGeo;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class BannerLanguageConstraintTest {
    @Mock
    private QueryrecService queryrecService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GeoTreeFactory geoTreeFactory;

    private BannerLanguageConstraint languageConstraint;

    private final String bannerText = "не имеет значения, главное не null";
    private final List<Long> adGroupGeoIds = ImmutableList.of(5L, 6L, 7L);

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public Language bannerTextLanguage; // recognized by queryrec

    @Parameterized.Parameter(2)
    public Language bannerLanguage;

    @Parameterized.Parameter(3)
    public Language campaignLanguage;

    @Parameterized.Parameter(4)
    public boolean isGeoIncluded;   // geo tree response

    @Parameterized.Parameter(5)
    public Defect expected;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return asList(
                // direct-qa: ru.yandex.autotests.directapi.cases.TextGeoMatchingWithCampLangCases#getPositiveTestCases
                new TestCase().campaignIn(Language.KAZAKH).bannerTextIn(Language.UKRAINIAN).validGeo().positive(),
                new TestCase().campaignIn(Language.UKRAINIAN).bannerTextIn(Language.TURKISH).validGeo().positive(),
                new TestCase().campaignIn(Language.TURKISH).bannerTextIn(Language.KAZAKH).validGeo().positive(),
                new TestCase().campaignIn(Language.BELARUSIAN).bannerTextIn(Language.GERMAN).validGeo().positive(),
                new TestCase().campaignIn(Language.RUSSIAN).bannerTextIn(Language.BELARUSIAN).validGeo().positive(),

                // не уверен, правильно ли это, такого кейса (ни +, ни -) не было в автотестах, но он проходит
                new TestCase().campaignIn(Language.RUSSIAN).bannerTextIn(Language.BELARUSIAN).positive(),

                // direct-qa: ru.yandex.autotests.directapi.cases.TextGeoMatchingWithCampLangCases#getNegativeCases
                new TestCase().campaignIn(Language.BELARUSIAN).bannerTextIn(Language.UKRAINIAN)
                        .expect(inconsistentLanguageWithGeo(Language.BELARUSIAN)),
                new TestCase().campaignIn(Language.UKRAINIAN).bannerTextIn(Language.TURKISH)
                        .expect(inconsistentLanguageWithGeo(Language.UKRAINIAN)),
                new TestCase().campaignIn(Language.TURKISH).bannerTextIn(Language.KAZAKH)
                        .expect(inconsistentLanguageWithGeo(Language.TURKISH)),
                new TestCase().campaignIn(Language.KAZAKH).bannerTextIn(Language.BELARUSIAN)
                        .expect(inconsistentLanguageWithGeo(Language.KAZAKH)),

                new TestCase().campaignIn(Language.KAZAKH).bannerIn(Language.KAZAKH).bannerTextIn(Language.BELARUSIAN)
                        .positive(),
                new TestCase().campaignIn(Language.KAZAKH).bannerIn(Language.RUSSIAN).bannerTextIn(Language.KAZAKH)
                        .expect(inconsistentLanguageWithGeo(Language.KAZAKH)),
                new TestCase().campaignIn(Language.KAZAKH).bannerIn(Language.UNKNOWN).bannerTextIn(Language.UKRAINIAN)
                        .expect(inconsistentLanguageWithGeo(Language.KAZAKH)),
                new TestCase().campaignIn(Language.KAZAKH).bannerIn(Language.UNKNOWN).bannerTextIn(Language.KAZAKH)
                        .positive());
    }

    @Before
    public void init() {
        initMocks(this);

        assert bannerText != null;
        when(queryrecService.recognize(eq(bannerText), isNull(), isNull()))
                .thenReturn(bannerTextLanguage);
        when(geoTreeFactory.getRussianGeoTree().isRegionsIncludedIn(eq(adGroupGeoIds), any()))
                .thenReturn(isGeoIncluded);
        languageConstraint = BannerLanguageConstraint
                .newBannerLanguageConstraint(queryrecService, geoTreeFactory, campaignLanguage,
                        adGroupGeoIds, bannerLanguage, null, null);
    }

    @Test
    public void languageIsFromGeo_ConstraintGivesExpectedResult() {
        Defect actual = languageConstraint.apply(bannerText);

        assertThat(actual).isEqualTo(expected);
    }

    private static class TestCase {
        private Language langTextBanner = Language.UNKNOWN;
        private Language langBanner = Language.UNKNOWN;
        private Language langCampaign = Language.UNKNOWN;
        private boolean isGeoIncluded;

        /**
         * Язык текста баннера
         */
        TestCase bannerTextIn(Language language) {
            this.langTextBanner = language;
            return this;
        }

        /**
         * Язык баннера
         */
        TestCase bannerIn(Language language) {
            this.langBanner = language;
            return this;
        }

        TestCase campaignIn(Language language) {
            this.langCampaign = language;
            return this;
        }

        TestCase validGeo() {
            this.isGeoIncluded = true;
            return this;
        }

        Object[] positive() {
            return expect(null);
        }

        Object[] expect(@Nullable Defect error) {
            return new Object[]{
                    format("(%s) campaign: %s; banner: %s, bannerText: %s; geoValid: %b",
                            error == null ? "+" : "-", langCampaign.getName(), langTextBanner.getName(),
                            langBanner.getName(), isGeoIncluded),
                    langTextBanner, langBanner, langCampaign, isGeoIncluded, error
            };
        }
    }
}
