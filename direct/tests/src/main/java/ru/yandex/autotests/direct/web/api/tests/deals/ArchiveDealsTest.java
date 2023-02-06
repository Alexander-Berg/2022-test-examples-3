package ru.yandex.autotests.direct.web.api.tests.deals;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.DealsChangeStatusResponse;
import ru.yandex.autotests.direct.web.api.rules.DealRule;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Архивация сделки")
@Stories(TestFeatures.Deal.ARCHIVE)
@Features(TestFeatures.DEAL)
@Tag(TrunkTag.YES)
@Tag(Tags.DEAL)
public class ArchiveDealsTest {
    private static final String CLIENT_LOGIN = Logins.AGENCY;
    public static final String COMPLETE_REASON_BY_DATE = "BY_DATE";

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    public DealRule dealRule = new DealRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectRule directRule =
            DirectRule.defaultRule().as(Logins.SUPER).withRules(dealRule);

    @Before
    public void before() {
        dealRule.getDirectWebApiSteps().dealTestSteps()
                .complete(dealRule.getDealIds(), COMPLETE_REASON_BY_DATE, CLIENT_LOGIN);
    }

    @Test
    public void archive() {
        DealsChangeStatusResponse response =
                dealRule.getDirectWebApiSteps().dealSteps().archiveDeals(dealRule.getDealIds(), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));
    }
}
