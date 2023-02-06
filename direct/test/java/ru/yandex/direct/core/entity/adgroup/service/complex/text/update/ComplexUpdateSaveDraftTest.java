package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

/**
 * Проверка, что флажок пробросился в обновление групп и баннеров
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateSaveDraftTest extends ComplexAdGroupUpdateOperationTestBase {
    private static final CompareStrategy DRAFT_COMPARE_STRATEGY =
            DefaultCompareStrategies.onlyFields(newPath("statusModerate"));

    @Test
    public void oneAdGroupSaveDraft() {
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1);
        MassResult<Long> result = createOperation(singletonList(adGroup), true).prepareAndApply();
        assumeThat("результат операции отрицательный, а ожидается положительный",
                result.isSuccessful(), is(true));
        AdGroup expected = adGroup.getAdGroup();
        expected.withStatusModerate(StatusModerate.NEW);
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(adGroupInfo1.getShard(), singletonList(expected.getId())).get(0);
        assertThat("группа должна быть черновиком", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }

    @Test
    public void oneAdGroupNotDraft() {
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1);
        MassResult<Long> result = createOperation(singletonList(adGroup), false).prepareAndApply();
        assumeThat("результат операции отрицательный, а ожидается положительный",
                result.isSuccessful(), is(true));
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(adGroupInfo1.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat("группа не должна быть черновиком", actualAdGroup.getStatusModerate(), not(StatusModerate.NEW));
    }

    @Test
    public void adGroupWithAddBannerSaveDraft() {
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(new ComplexTextBanner()
                        .withBanner(fullTextBanner()
                                .withId(null)
                                .withTitle(randomAlphabetic(10))
                                .withHref("https://yandex.ru"))));
        MassResult<Long> result = createOperation(singletonList(adGroup), true).prepareAndApply();
        assumeThat("результат операции отрицательный, а ожидается положительный",
                result.isSuccessful(), is(true));
        var expected = adGroup.getComplexBanners().get(0).getBanner();
        expected.withStatusModerate(BannerStatusModerate.NEW);
        var actual = bannerTypedRepository.getStrictly(adGroupInfo1.getShard(), singletonList(expected.getId()),
                BannerWithSystemFields.class).get(0);
        assertThat("баннер должен быть черновиком", actual,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }

    @Test
    public void adGroupWithUpdateBannerSaveDraft() {
        var banner = steps.textBannerSteps()
                .createBanner(new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withStatusModerate(BannerStatusModerate.YES))
                        .withAdGroupInfo(adGroupInfo1));
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(new ComplexTextBanner()
                        .withBanner(((TextBanner) banner.getBanner())
                                .withBody("another body")
                                .withDomain(null))));
        MassResult<Long> result = createOperation(singletonList(adGroup), true).prepareAndApply();
        assumeThat("результат операции отрицательный, а ожидается положительный",
                result.isSuccessful(), is(true));
        var expected = adGroup.getComplexBanners().get(0).getBanner();
        expected.withStatusModerate(BannerStatusModerate.NEW);
        var actual = bannerTypedRepository.getStrictly(adGroupInfo1.getShard(), singletonList(expected.getId()),
                BannerWithSystemFields.class).get(0);
        assertThat("баннер должен быть черновиком", actual,
                beanDiffer(expected).useCompareStrategy(DRAFT_COMPARE_STRATEGY));
    }

    @Test
    public void adGroupWithBannerNotDraft() {
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(new ComplexTextBanner()
                        .withBanner(clientTextBanner()
                                .withId(null)
                                .withTitle(randomAlphabetic(10))
                                .withHref("https://yandex.ru"))));
        MassResult<Long> result = createOperation(singletonList(adGroup), false).prepareAndApply();
        assumeThat("результат операции отрицательный, а ожидается положительный",
                result.isSuccessful(), is(true));
        var expected = adGroup.getComplexBanners().get(0).getBanner();
        var actual = bannerTypedRepository.getStrictly(adGroupInfo1.getShard(), singletonList(expected.getId()),
                BannerWithSystemFields.class).get(0);
        assertThat("баннер не должен быть черновиком", actual.getStatusModerate(), not(StatusModerate.NEW));
    }
}
