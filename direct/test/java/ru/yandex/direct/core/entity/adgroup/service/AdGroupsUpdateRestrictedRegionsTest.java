package ru.yandex.direct.core.entity.adgroup.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.restrictedRegions;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsUpdateRestrictedRegionsTest {
    @Autowired
    public AdGroupSteps adGroupsSteps;

    @Autowired
    public BannerSteps bannerSteps;

    @Autowired
    public GeoTreeFactory geoTreeFactory;

    @Autowired
    public AdGroupService adGroupService;

    private GeoTree geoTree;
    private AdGroupInfo adGroupInfo;

    @Before
    public void setUp() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        adGroupInfo = adGroupsSteps.createAdGroup(defaultTextAdGroup(null));
    }

    @Test
    public void testNoWarnings() {
        bannerSteps.createDefaultTextBannerWithMinusGeo(adGroupInfo, emptyList());

        MassResult<Long> result = adGroupService.updateAdGroupsPartialWithFullValidation(
                singletonList(
                        new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class)
                                .process(asList(Region.MOSCOW_REGION_ID, Region.UKRAINE_REGION_ID), AdGroup.GEO)),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                adGroupInfo.getUid(),
                adGroupInfo.getClientId());

        assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void testExpectWarning() {
        bannerSteps.createDefaultTextBannerWithMinusGeo(
                adGroupInfo, singletonList(Region.RUSSIA_REGION_ID));

        MassResult<Long> result = adGroupService.updateAdGroupsPartialWithFullValidation(
                singletonList(
                        new ModelChanges<>(adGroupInfo.getAdGroupId(), AdGroup.class)
                                .process(asList(Region.MOSCOW_REGION_ID, Region.UKRAINE_REGION_ID), AdGroup.GEO)),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                adGroupInfo.getUid(),
                adGroupInfo.getClientId());
        assumeThat(result.getValidationResult(), hasNoErrors());

        assertThat(result.getValidationResult()).is(
                matchedBy(
                        hasWarningWithDefinition(
                                validationError(
                                        path(index(0)), restrictedRegions()))));
    }
}
