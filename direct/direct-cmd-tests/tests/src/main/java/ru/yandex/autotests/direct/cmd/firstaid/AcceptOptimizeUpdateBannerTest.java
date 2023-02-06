package ru.yandex.autotests.direct.cmd.firstaid;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.direct.cmd.steps.firsthelp.FirstHelpHelper.prepareCampForOptimizinhAccept;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("С помощью контроллера acceptOptimize баннер обновляется, а не заменяется")
@Stories(TestFeatures.FirstAid.ACCEPT_OPTIMIZE)
@Features(TestFeatures.FIRST_AID)
@Tag(CmdTag.ACCEPT_OPTIMIZE)
@Tag(CampTypeTag.TEXT)
public class AcceptOptimizeUpdateBannerTest {
    public static final String CLIENT = "at-direct-b-firstaid-c7";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long optimizeRequestId;

    @Before
    public void before() {
        optimizeRequestId = prepareCampForOptimizinhAccept(cmdRule,
                bannersRule.getCampaignId(),
                bannersRule.getGroupId(),
                CLIENT
        );
    }

    @Test
    @Description("Подтверждение первой помощи")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10826")
    public void updateBanner() {
        Long expectedBannerId = bannersRule.getBannerId();

        cmdRule.cmdSteps().firstHelpSteps().acceptOptimize(
                bannersRule.getCampaignId(),
                bannersRule.getCurrentGroup().getPhrases().stream().map(Phrase::getId).collect(toList()),
                optimizeRequestId,
                CLIENT
        );
        List<Banner> banners =
                cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, bannersRule.getCampaignId()).getGroups()
                        .stream()
                        .flatMap(x -> x.getBanners().stream())
                        .collect(toList());

        assumeThat("Число баннеров не изменилось после обновления", banners, hasSize(1));
        assertThat("Баннер не изменил идентификатор", banners.get(0).getBid(), equalTo(expectedBannerId));
    }

}
