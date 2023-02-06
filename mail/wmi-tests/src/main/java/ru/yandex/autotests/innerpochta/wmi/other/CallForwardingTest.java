package ru.yandex.autotests.innerpochta.wmi.other;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.params.HttpClientParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.text.MessageFormat;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 22.01.14
 * Time: 14:03
 * <p/>
 * Руками проверять так:
 * curl -k -D - https://wmi6-qa.yandex.ru/%5cwww.google.com%2f%2e%2e/neo
 * [DARIA-24619]
 * [DARIA-15341]
 */
@Aqua.Test
@Title("Проверяем, что нет открытой переадрессации")
@Description("Дампим запросы, проверяем хэдер location")
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@RunWith(DataProviderRunner.class)
@Credentials(loginGroup = "Openforward")
public class CallForwardingTest extends BaseTest {

    @DataProvider
    public static Object[][] dangerousUriPart() throws Exception {
        return new Object[][]{
                {"{0}//www.google.ru/..%2fneo"},
                {"{0}//www.google.ru/..%2fneo2"},
                {"{0}/%5cgoogle.com%2f%2e%2e/neo2"},
                {"{0}/%5cwww.google.com%2f%2e%2e/neo"},
                {"{0}/%5cwww.google.com%2f%2e%2e/m"},
                {"{0}/url/QcvrPmARpiwLDWH_U06fIA,1338274841/oxdef.info"}
        };
    }

    @Test
    @UseDataProvider("dangerousUriPart")
    public void callForwardingTest(String uriPart) throws IOException {
        String uri = MessageFormat.format(uriPart, props().betaHost());
        HttpClientParams.setRedirecting(hc.getParams(), false);
        Executor.newInstance(hc).execute(Request.Get(uri))
                .handleResponse(response ->
                {
                    assertThat("Открытая переадресация",
                            String.valueOf(response.getStatusLine().getStatusCode()),
                            both(not(startsWith("2"))).and(not(startsWith("5"))));
                    return null;
                });
    }
}
