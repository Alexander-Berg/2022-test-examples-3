package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullTextBannerForAdd;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты с пустыми или полностью заполненными группами
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexTextAddCommonsTest extends ComplexTextAddTestBase {

    @Test
    public void oneFullAdGroup() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void emptyAdGroup_IdsReturnedInResult() {
        ComplexTextAdGroup complexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        assertThat(mapList(result.getResult(), Result::getResult), contains(notNullValue()));
    }

    @Test
    public void oneEmptyAdGroup() {
        ComplexTextAdGroup complexAdGroup = emptyTextAdGroup();
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void twoFullAdGroups_FullySuccessful() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        ComplexTextAdGroup complexAdGroup2 = fullAdGroup();
        addAndCheckComplexAdGroups(asList(complexAdGroup, complexAdGroup2));
    }

    @Test
    public void twoFullAdGroupsFromDifferentCampaigns() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCampaign(campaign.getClientInfo());
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        ComplexTextAdGroup complexAdGroup2 = fullAdGroup(campaignInfo.getCampaignId());
        addAndCheckComplexAdGroups(asList(complexAdGroup, complexAdGroup2));
    }

    @Test
    public void oneEmptyAndOneFullAdGroup() {
        ComplexTextAdGroup emptyComplexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroup complexAdGroup = fullAdGroup(asList(fullTextBannerForAdd(),
                emptyBannerForAdd(), fullTextBannerForAdd()));
        addAndCheckComplexAdGroups(asList(emptyComplexAdGroup, complexAdGroup));
    }

}
