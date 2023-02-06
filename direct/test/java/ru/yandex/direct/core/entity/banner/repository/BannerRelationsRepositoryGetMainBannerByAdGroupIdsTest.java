package ru.yandex.direct.core.entity.banner.repository;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerRelationsRepositoryGetMainBannerByAdGroupIdsTest {

    private static final CompareStrategy COMPARE_STRATEGY = allFieldsExcept(newPath("lastChange"));

    @Autowired
    private TextBannerSteps textBannerSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private BannerRelationsRepository repoUnderTest;

    private int shard;
    private long adGroupId;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        adGroupInfo = adGroupSteps.createDefaultAdGroup();
        shard = adGroupInfo.getShard();
        adGroupId = adGroupInfo.getAdGroupId();
    }


    @Test
    public void getMainBannerByAdGroupIds_TwoTextBanners_GetBoth() {

        TextBanner banner1 = fullTextBanner().withCalloutIds(emptyList());
        TextBanner banner2 = fullTextBanner().withCalloutIds(emptyList());

        createBanner(banner1);
        createBanner(banner2);

        checkActualMainBanner(banner1);
    }

    @Test
    public void getMainBannerByAdGroupIds_TwoTextBanners_ActiveIsMain() {
        TextBanner banner1 = fullTextBanner()
                .withCalloutIds(emptyList())
                .withStatusShow(false);

        TextBanner banner2 = fullTextBanner()
                .withCalloutIds(emptyList())
                .withStatusShow(true)
                .withStatusActive(true);

        createBanner(banner1);
        createBanner(banner2);

        checkActualMainBanner(banner2);
    }

    private void createBanner(TextBanner banner) {
        textBannerSteps.createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    private void checkActualMainBanner(BannerWithSystemFields expectedBanner) {
        Map<Long, BannerWithSystemFields> actual =
                repoUnderTest.getMainBannerByAdGroupIds(shard, singletonList(adGroupId));

        assertThat("главный баннер некорректно получен по adGroupId из базы",
                actual.values(), contains(beanDiffer(expectedBanner)
                        .useCompareStrategy(COMPARE_STRATEGY)));
    }

}
