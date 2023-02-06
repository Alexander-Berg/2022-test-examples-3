package ru.yandex.direct.core.entity.adgroup.service.complex.mcbanner;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.ComplexMcBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexMcBannerAdGroupUpdateOperationTest {
    private static final CompareStrategy AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields();

    @Autowired
    private ComplexAdGroupUpdateOperationFactory updateOperationFactory;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private ComplexAdGroupTestCommons commonChecks;
    @Autowired
    private Steps steps;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;
    private AdGroupInfo adgroup;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        adgroup = steps.adGroupSteps().createActiveMcBannerAdGroup();
    }

    @Test
    public void oneEmptyMcBannerAdGroup() {
        ComplexMcBannerAdGroup complexAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(adgroup.getCampaignId()).withId(adgroup.getAdGroupId()));
        ComplexMcBannerAdGroupUpdateOperation operation =
                createOperation(singletonList(complexAdGroup), adgroup.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void mcBannerAdGroupWithValidationError() {
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(adgroup.getCampaignId()));
        ComplexMcBannerAdGroupUpdateOperation operation =
                createOperation(singletonList(complexMcBannerAdGroup), adgroup.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(McBannerAdGroup.ID.name()));
        Defect error = new Defect<>(CANNOT_BE_NULL);
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(errPath, error)));
    }

    private ComplexMcBannerAdGroupUpdateOperation createOperation(List<ComplexMcBannerAdGroup> complexAdGroups,
                                                                  ClientInfo clientInfo) {
        return updateOperationFactory.createMcBannerAdGroupUpdateOperation(complexAdGroups, geoTree,
                false, null,
                clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), false);
    }

}
