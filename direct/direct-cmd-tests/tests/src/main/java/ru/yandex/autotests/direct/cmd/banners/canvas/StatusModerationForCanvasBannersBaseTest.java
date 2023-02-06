package ru.yandex.autotests.direct.cmd.banners.canvas;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// таск: https://st.yandex-team.ru/TESTIRT-10226

@RunWith(Parameterized.class)
public abstract class StatusModerationForCanvasBannersBaseTest {
    protected static final String CLIENT = "at-direct-creative-construct";

    private BannersPerformanceStatusmoderate creativeModerateStatus;

    private BannersStatusmoderate bannerModerateStatus;

    private BannersStatuspostmoderate bannerPostModerateStatus;

    private List expStatuses;


    public StatusModerationForCanvasBannersBaseTest(BannersPerformanceStatusmoderate creativeModerateStatus,
                                                    BannersStatusmoderate bannerModerateStatus,
                                                    BannersStatuspostmoderate bannerPostModerateStatus,
                                                    List expStatuses) {
        this.creativeModerateStatus = creativeModerateStatus;
        this.bannerModerateStatus = bannerModerateStatus;
        this.bannerPostModerateStatus = bannerPostModerateStatus;
        this.expStatuses = expStatuses;
    }

    @Parameterized.Parameters(name = "Статус модерации креатива: {0}," +
            " Статус модерации баннера: {1}," +
            " Статус постмодерации баннера: {2}," +
            " результирующий статус баннера: {3},")
    public static Collection<Object[]> testData() {
        return Arrays.asList(
                new Object[][]{
                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.No, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.NO.toString())},

                        {BannersPerformanceStatusmoderate.No, BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.NO.toString())},

                        {BannersPerformanceStatusmoderate.Ready, BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.READY.toString(), StatusModerate.SENDING.toString())},
                        {BannersPerformanceStatusmoderate.Sent, BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.SENT.toString())},
                        {BannersPerformanceStatusmoderate.Sending, BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.READY.toString(), StatusModerate.SENDING.toString())},

                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.Ready, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.YES.toString())},
                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.Ready, BannersStatuspostmoderate.No, Arrays.asList(StatusModerate.READY.toString(), StatusModerate.SENDING.toString())},
                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.Ready, BannersStatuspostmoderate.Sent, Arrays.asList(StatusModerate.READY.toString(), StatusModerate.SENDING.toString())},
                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.Ready, BannersStatuspostmoderate.Ready, Arrays.asList(StatusModerate.READY.toString(), StatusModerate.SENDING.toString())},
                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.Ready, BannersStatuspostmoderate.New, Arrays.asList(StatusModerate.READY.toString(), StatusModerate.SENDING.toString())},

                        {BannersPerformanceStatusmoderate.New, BannersStatusmoderate.New, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.NEW.toString())},
                        {BannersPerformanceStatusmoderate.Yes, BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes, Arrays.asList(StatusModerate.YES.toString())},

                });
    }

    @Description("проверка статуса модерации")
    public void checkStatus() {
        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusModerate(
                getBannerRule().getBannerId(), BannersStatusmoderate.valueOf(bannerModerateStatus.toString())
        );

        TestEnvironment.newDbSteps().bannersPerformanceSteps().setCreativeStatusModerate(getBannerRule().getCampaignId(),
                getBannerRule().getGroupId(), getBannerRule().getBannerId(), creativeModerateStatus
        );


        TestEnvironment.newDbSteps().bannersSteps().setBannerStatusPostModerate(
                getBannerRule().getBannerId(), BannersStatuspostmoderate.valueOf(bannerPostModerateStatus.toString())
        );

        Banner banner = getCmdRule().cmdSteps().groupsSteps().getBanner(CLIENT, getBannerRule().getCampaignId(),
                getBannerRule().getBannerId());

        assertThat(
                "статус модерации баннера соответствует ожиданиям",
                expStatuses,
                hasItem(banner.getStatusModerate()));
    }

    protected abstract CreativeBannerRule getBannerRule();

    protected abstract DirectCmdRule getCmdRule();

}
