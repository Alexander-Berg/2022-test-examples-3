package ru.yandex.autotests.direct.web.api.tests.deals;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.GetDealsDetailsResponse;
import ru.yandex.autotests.direct.web.api.rules.DealRule;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
@Aqua.Test
@Description("Получение деталей сделки")
@Stories(TestFeatures.Deal.GET_DEALS_DETAILS)
@Features(TestFeatures.DEAL)
@Tag(TrunkTag.YES)
@Tag(Tags.DEAL)
public class GetDealsDetailsTest {
    private static final String CLIENT_LOGIN = Logins.AGENCY;

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    public DealRule dealRule = new DealRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectRule directRule =
            DirectRule.defaultRule().as(Logins.SUPER).withRules(dealRule);

    @Test
    public void getDealsDetails() {
        GetDealsDetailsResponse response =
                dealRule.getDirectWebApiSteps().dealSteps().getDealsDetails(dealRule.getDealIds(), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true", response.getSuccess(), is(true));
    }
}
