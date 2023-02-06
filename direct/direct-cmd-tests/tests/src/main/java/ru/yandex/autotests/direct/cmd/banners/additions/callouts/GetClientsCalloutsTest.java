package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Получение дополнений клиента с различными статусами модерации")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.GET_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class GetClientsCalloutsTest {

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-2";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    Long clientId;
    DirectJooqDbSteps dbSteps;
    List<Callout> expectedCallouts = new LinkedList<>();
    private CalloutsTestHelper helper;

    @Before
    public void before() {
        dbSteps = TestEnvironment.newDbSteps();

        clientId = Long.valueOf(User.get(ulogin).getClientID());

        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), null);

        helper.clearCalloutsForClient();

        for (AdditionsItemCalloutsStatusmoderate statusModerate : AdditionsItemCalloutsStatusmoderate.values()) {
            String text = "callout_status_" + statusModerate.toString();

            Long id = TestEnvironment.newDbSteps().bannerAdditionsSteps().saveAdditionsItemCallouts(
                    clientId,
                    text,
                    statusModerate);

            expectedCallouts.add((Callout) new Callout()
                    .withCalloutText(text)
                    .withStatusModerate(statusModerate)
                    .withAdditionsItemId(id));
        }
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9077")
    public void getCalloutsTest() {
        GetBannersAdditionsResponse response = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(ulogin);
        List<Callout> actual = response.getCallouts();
        expectedCallouts = expectedCallouts.stream()
                .sorted(Comparator.comparing(Callout::getAdditionsItemId))
                .collect(Collectors.toList());
        assertThat("в запросе присутствуют дополнения", actual, beanDiffer(expectedCallouts));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9078")
    public void withLimitAndOffset() {
        GetBannersAdditionsResponse response = cmdRule.cmdSteps()
                .bannersAdditionsSteps().getCallouts(ulogin, 2, 2);
        List<Callout> actual = response.getCallouts();
        expectedCallouts = expectedCallouts.stream()
                .sorted(Comparator.comparing(Callout::getAdditionsItemId))
                .collect(Collectors.toList());
        List<Callout> expected = expectedCallouts.subList(2, 4);
        assertThat("в запросе присутствуют дополнения", actual, beanDiffer(expected));
    }

}
