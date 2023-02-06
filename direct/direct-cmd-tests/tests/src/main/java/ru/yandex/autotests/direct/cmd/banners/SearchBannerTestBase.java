package ru.yandex.autotests.direct.cmd.banners;

import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersResponse;
import ru.yandex.autotests.direct.cmd.data.banners.SearchWhat;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;

public abstract class SearchBannerTestBase {

    protected static final String CLIENT = "at-direct-backend-b";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    public SearchBannerTestBase(BannersRule bannersRule) {
        this.bannersRule = bannersRule;
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Description("Поиск баннера по номеру объявления в группе")
    public void searchBannerByAdNumber() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.NUM.getName(), bannersRule.getBannerId().toString());
        assumeThat("Нашли баннер по номеру объявления в группе",
                response.getBanners(), hasSize(greaterThan(0)));

        Banner banner = response.getBanners().get(0);
        assertThat("Найденный баннер совпадает с искомым баннером",
                banner.getBid(), equalTo(bannersRule.getBannerId()));
    }

    @Description("Поиск баннера по номеру группы")
    public void searchBannerByGroupNumber() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.GROUP.getName(), bannersRule.getGroupId().toString());
        assumeThat("Нашли баннер по номеру группе",
                response.getBanners(), hasSize(greaterThan(0)));

        Banner banner = response.getBanners().get(0);
        assertThat("Найденный баннер совпадает с искомым баннером",
                banner.getBid(), equalTo(bannersRule.getBannerId()));
    }


}
