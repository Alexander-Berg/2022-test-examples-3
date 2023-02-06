package ru.yandex.direct.core.entity.adgroup.service.complex.mcbanner;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.ComplexMcBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexMcBannerAdGroupAddOperationTest {
    private static final CompareStrategy AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields()
            .forFields(newPath("statusBsSynced")).useMatcher(is(StatusBsSynced.NO))
            .forFields(newPath("statusShowsForecast")).useMatcher(is(StatusShowsForecast.NEW));

    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private ComplexAdGroupTestCommons commonChecks;
    @Autowired
    private Steps steps;
    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private int shard;
    private Long campaignId;
    private ClientInfo clientInfo;
    private GeoTree geoTree;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        var typedCampaignInfo = steps.mcBannerCampaignSteps().createDefaultCampaign(clientInfo);
        campaignId = typedCampaignInfo.getId();
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    public void oneEmptyMcBannerAdGroup() {
        ComplexMcBannerAdGroup complexAdGroup =
                new ComplexMcBannerAdGroup().withAdGroup(activeMcBannerAdGroup(campaignId));
        ComplexMcBannerAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup), clientInfo);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void mcBannerAdGroupWithValidationError() {
        ComplexMcBannerAdGroup complexMcBannerAdGroup = new ComplexMcBannerAdGroup()
                .withAdGroup(activeMcBannerAdGroup(-1L));
        ComplexMcBannerAdGroupAddOperation operation =
                createOperation(singletonList(complexMcBannerAdGroup), clientInfo);
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(McBannerAdGroup.CAMPAIGN_ID.name()));
        Defect error = new Defect<>(MUST_BE_VALID_ID);
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(errPath, error)));
    }

    private void checkComplexAdGroup(ComplexMcBannerAdGroup complexAdGroup) {
        AdGroup expectedAdGroup = complexAdGroup.getAdGroup();
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(expectedAdGroup.getId()));
        CompareStrategy compareStrategy = isEmpty(complexAdGroup.getKeywords()) ?
                onlyExpectedFields() : AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES;
        assertThat("группа успешно добавлена", adGroups, contains(beanDiffer(expectedAdGroup)
                .useCompareStrategy(compareStrategy)));

        Long adGroupId = complexAdGroup.getAdGroup().getId();
        var banners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(adGroupId));
        assertThat("в группе не должно быть баннеров", banners, empty());

        commonChecks.checkKeywords(complexAdGroup.getKeywords(), complexAdGroup.getAdGroup().getId(), shard);
        commonChecks.checkBidModifiers(complexAdGroup.getComplexBidModifier(), adGroupId, campaignId, shard);
    }

    private ComplexMcBannerAdGroupAddOperation createOperation(List<ComplexMcBannerAdGroup> complexAdGroups,
                                                               ClientInfo clientInfo) {
        return addOperationFactory.createMcBannerAdGroupAddOperation(true, complexAdGroups,
                geoTree, false, null, clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid());
    }

}
