package ru.yandex.autotests.direct.httpclient.showstat;


import org.apache.http.client.utils.URIBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.CoreMatchers.equalTo;

public abstract class MasterOfReportsBaseTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static String host;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameter(value = 0)
    public String url;

    @BeforeClass
    public static void beforeClass() {
        host = DirectTestRunProperties.getInstance().getDirectCmdHost().replace("https://", "");
    }

    @Description("Проверяем код ответа")
    public void testHTTPResponseCodesInMasterOfReports() {
        URIBuilder uri = new URIBuilder();
        uri.setScheme("https");
        uri.setHost(host);
        uri.setPath("/registered/main.pl");
        uri.setCustomQuery(url);

        cmdRule.oldSteps().commonSteps().checkDirectResponseStatusCodeForRequest(uri.toString(), equalTo(200));
    }

}
