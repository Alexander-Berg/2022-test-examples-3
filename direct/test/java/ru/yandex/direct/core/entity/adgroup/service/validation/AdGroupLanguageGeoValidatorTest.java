package ru.yandex.direct.core.entity.adgroup.service.validation;

import java.util.List;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.text.BannerTextExtractor;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.queryrec.QueryrecService;
import ru.yandex.direct.queryrec.model.Language;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.inconsistentGeoWithBannerLanguages;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupLanguageGeoValidatorTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    private static final Long ADGROUP_ID = 1L;
    private static final Long BANNER_ID = 2L;
    private static final Set<Long> ADGROUPS_WITH_CHANGED_GEO_IDS = singleton(ADGROUP_ID);

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public BannerTextExtractor bannerTextExtractor;

    @Autowired
    public QueryrecService queryrecService;

    @Autowired
    public GeoTreeFactory geoTreeFactory;

    private AdGroupLanguageGeoValidator languageGeoValidator;

    private static AdGroup newTestAdGroup() {
        return new AdGroup()
                .withId(ADGROUP_ID)
                .withBanners(
                        singletonList(
                                new TextBanner()
                                        .withId(BANNER_ID)));
    }

    @Before
    public void setUp() {
        queryrecService = spy(queryrecService);

        languageGeoValidator = new AdGroupLanguageGeoValidator(bannerTextExtractor, queryrecService, geoTreeFactory);
    }

    @Test
    public void validateGeoNotChanged() {
        Constraint<AdGroup, Defect> constraint = languageGeoValidator.createConstraint(
                emptySet(), adGroupId -> null, null, null);

        assertThat(constraint.apply(newTestAdGroup())).isEqualTo(null);
    }

    private Object getLanguageGeoPairs() {
        return new Object[]{
                new Object[]{Language.RUSSIAN, singletonList(Region.RUSSIA_REGION_ID), null},

                new Object[]{Language.UKRAINIAN, singletonList(Region.UKRAINE_REGION_ID), null},

                new Object[]{Language.UKRAINIAN, singletonList(Region.CRIMEA_REGION_ID), null},

                new Object[]{
                        Language.UKRAINIAN,
                        singletonList(Region.RUSSIA_REGION_ID),
                        inconsistentGeoWithBannerLanguages(Language.UKRAINIAN, BANNER_ID)},

                new Object[]{Language.KAZAKH, singletonList(Region.KAZAKHSTAN_REGION_ID), null},
                new Object[]{
                        Language.KAZAKH,
                        singletonList(Region.RUSSIA_REGION_ID),
                        inconsistentGeoWithBannerLanguages(Language.KAZAKH, BANNER_ID)},

                new Object[]{Language.TURKISH, singletonList(Region.TURKEY_REGION_ID), null},
                new Object[]{
                        Language.TURKISH,
                        singletonList(Region.RUSSIA_REGION_ID),
                        inconsistentGeoWithBannerLanguages(Language.TURKISH, BANNER_ID)},

                new Object[]{Language.BELARUSIAN, singletonList(Region.BY_REGION_ID), null},
                new Object[]{
                        Language.BELARUSIAN,
                        singletonList(Region.RUSSIA_REGION_ID),
                        inconsistentGeoWithBannerLanguages(Language.BELARUSIAN, BANNER_ID)}
        };
    }

    @Test
    @Parameters(method = "getLanguageGeoPairs")
    public void validateByBannerLanguage(
            Language recognizedLanguage, List<Long> newGeoIds, Defect expectedError) {
        when(queryrecService.recognize(any(), nullable(ClientId.class), nullable(Long.class))).thenReturn(recognizedLanguage);

        Constraint<AdGroup, Defect> constraint = languageGeoValidator.createConstraint(
                ADGROUPS_WITH_CHANGED_GEO_IDS,
                adGroupId -> Language.UNKNOWN, null, null);

        assertThat(constraint.apply(newTestAdGroup().withGeo(newGeoIds)))
                .isEqualTo(expectedError);
    }

    @Test
    @Parameters(method = "getLanguageGeoPairs")
    public void validateByCampaignLanguage(
            Language recognizedLanguage, List<Long> newGeoIds, Defect expectedError) {
        Constraint<AdGroup, Defect> constraint = languageGeoValidator.createConstraint(
                ADGROUPS_WITH_CHANGED_GEO_IDS,
                adGroupId -> recognizedLanguage, null, null);

        assertThat(constraint.apply(newTestAdGroup().withGeo(newGeoIds)))
                .isEqualTo(expectedError);
    }
}
