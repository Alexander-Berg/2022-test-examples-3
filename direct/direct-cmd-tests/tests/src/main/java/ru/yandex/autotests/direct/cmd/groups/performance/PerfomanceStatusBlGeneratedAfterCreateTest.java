package ru.yandex.autotests.direct.cmd.groups.performance;
//Task: TESTIRT-9323.

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdgroupsPerformanceRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBlGenerated;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.utils.matchers.BeanEqualsAssert.assertThat;

@Aqua.Test
@Description("Проверка, что перфоманс баннеры при создании компании обладают статусом StatusBlGenerated = 'идет обработка'")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class PerfomanceStatusBlGeneratedAfterCreateTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        cmdRule.cmdSteps().bannerSteps()
                .sendModerate(CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
    }

    @Test
    @Description("Статус генерации префоманс баннера при создании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9836")
    public void startBlGeneratedStatus() {
        AdgroupsPerformanceRecord actualStatus = TestEnvironment.newDbSteps(CLIENT)
                .adGroupsSteps().getAdgroupsPerformance(bannersRule.getGroupId());
        assertThat("у только что созданной группы статус соответсвует ожиданиям",
                actualStatus.getStatusblgenerated().getLiteral(),
                equalTo(StatusBlGenerated.PROCESS.toString()));
    }
}
