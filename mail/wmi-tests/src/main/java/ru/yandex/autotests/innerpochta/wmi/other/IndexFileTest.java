package ru.yandex.autotests.innerpochta.wmi.other;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.params.HttpClientParams;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 08.04.14
 * Time: 17:45
 * [DARIA-32881]
 */
@Aqua.Test
@Title("Проверяем обработку деррективы <index_file> в компоненте wmi")
@Description("Дампим запросы, проверяем только статус")
@Features(MyFeatures.WMI)
@Stories(MyStories.REDIRECTS)
@RunWith(Parameterized.class)
@Credentials(loginGroup = "Openforward")
public class IndexFileTest extends BaseTest {

    private String uri;
    private int status;

    @Parameterized.Parameters(name = "{0}-expected_status-{1}")
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{
                "{0}/socialsbscr/",
                HttpStatus.FOUND_302});
        return data;
    }

    public IndexFileTest(String uri, int status) {
        String serverUrl = props().betaHost();
        this.uri = MessageFormat.format(uri, serverUrl);
        this.status = status;
    }

    @Test
    public void indexFileTest() throws IOException {
        HttpClientParams.setRedirecting(hc.getParams(), false);
        Executor.newInstance(hc).execute(Request.Get(uri))
                .handleResponse(statusHandler(status));

    }

    private ResponseHandler<Object> statusHandler(final int expectedStatus) {
        return new ResponseHandler<Object>() {
            @Override
            public Object handleResponse(HttpResponse response)
                    throws ClientProtocolException, IOException {
                assertThat("Получен неверный статус код ",
                        response.getStatusLine().getStatusCode(), equalTo(expectedStatus));
                ;
                return null;
            }
        };
    }
}