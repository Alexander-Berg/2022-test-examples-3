package ru.yandex.autotests.httpclient.firsthelp;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.direct.httpclient.UserSteps;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.SendCampaignOptimizingOrderParameters;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.model.User;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class RequestFirstHelpTest {

    UserSteps user;

    @Before
    public void setup() {
        user = new UserSteps(DirectTestRunProperties.getInstance());
        user.onPassport().authoriseAs("at-direct-ph-addbanner-c", "at-tester1");
//        DirectRequestBuilder requestBuilder = new DirectRequestBuilder()
//                .uri("https://test2-direct.yandex.ru/registered/main.pl");
//        firstHelpSteps = new FirstHelpSteps(getDefaultClient(), requestBuilder);
//        firstHelpSteps.authoriseAs("at-direct-ph-addbanner-c", "at-tester1");
    }

    @Test
    @Ignore
    public void sendRequest() {
        SendCampaignOptimizingOrderParameters params = new SendCampaignOptimizingOrderParameters();
        params.setCid("9358904");
        params.setAgree("yes");
        params.setBudget("123432");
        params.setImprovementKeyword("Keyword");
        params.setImprovementMediaplaner("Mediaplaner");
        params.setImprovementText("Text");

        user.firstHelpSteps().requestFirstHelp(params, new CSRFToken("fill_with_valid_value"));
    }
}
