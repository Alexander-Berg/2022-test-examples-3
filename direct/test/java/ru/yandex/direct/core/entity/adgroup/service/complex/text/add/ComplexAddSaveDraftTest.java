package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

/**
 * Проверка, что флажок пробросился в добавление групп и баннеров
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddSaveDraftTest extends ComplexTextAddTestBase {

    private static final CompareStrategy DRAFT_COMPARE_STRATEGY =
            DefaultCompareStrategies.onlyFields(newPath("statusModerate"));

    @Test
    public void oneAdGroupSaveDraft() {
        ComplexTextAdGroup complexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup), true);
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup expected = complexAdGroup.getAdGroup();
        expected.withStatusModerate(StatusModerate.NEW);
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(campaign.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat("группа должна быть черновиком", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }

    @Test
    public void oneAdGroupNotDraft() {
        ComplexTextAdGroup complexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup), false);
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup expected = complexAdGroup.getAdGroup();
        expected.withStatusModerate(StatusModerate.READY);
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(campaign.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat("группа не должна быть черновиком", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }

    @Test
    public void adGroupWithBannersSaveDraft() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup), true);
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        var expected = complexAdGroup.getComplexBanners().get(0).getBanner();
        expected.withStatusModerate(BannerStatusModerate.NEW);
        var actual = bannerTypedRepository.getStrictly(campaign.getShard(), singletonList(expected.getId()),
                BannerWithSystemFields.class).get(0);
        assertThat("баннер должен быть черновиком", actual,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }

    @Test
    public void adGroupWithBannersNotDraft() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup), false);
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        var expected = complexAdGroup.getComplexBanners().get(0).getBanner();
        expected.withStatusModerate(BannerStatusModerate.READY);
        var actual = bannerTypedRepository.getStrictly(campaign.getShard(), singletonList(expected.getId()),
                BannerWithSystemFields.class).get(0);
        assertThat("баннер не должен быть черновиком", actual,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }
}
