package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.adgroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Тесты на то, что обновление/добавление происходит для всех вложенных объектов.
 * Проверки нацелены на проверку факта обновления и некоторых данных,
 * которые специфическим образом обрабатываются при обновлении комплексной группы.
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateAdGroupDataTest extends ComplexUpdateAdGroupTestBase {

    private static final CompareStrategy AD_GROUP_UPDATE_STRATEGY = DefaultCompareStrategies.onlyExpectedFields();

    @Test
    public void update_OneEmptyAdGroup_AdGroupIsUpdated() {
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1);
        AdGroup expectedAdGroup = createExpectedUpdatedAdGroup(adGroupInfo1, adGroupForUpdate);
        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        AdGroup updatedAdGroup = getAdGroup(adGroupInfo1.getAdGroupId());
        assertThat("данные обновленной группы отличаются от ожидаемых",
                updatedAdGroup,
                beanDiffer(expectedAdGroup).useCompareStrategy(AD_GROUP_UPDATE_STRATEGY));
    }

    protected AdGroup createExpectedUpdatedAdGroup(AdGroupInfo sourceAdGroupInfo,
                                                   ComplexTextAdGroup complexAdGroupForUpdate) {
        AdGroup sourceAdGroup = sourceAdGroupInfo.getAdGroup();
        AdGroup adGroupForUpdate = complexAdGroupForUpdate.getAdGroup();
        return new TextAdGroup()
                .withId(sourceAdGroup.getId())
                .withCampaignId(sourceAdGroup.getCampaignId())
                .withName(adGroupForUpdate.getName());
    }
}
