package ru.yandex.autotests.innerpochta.webattach;

import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes;
import ru.yandex.autotests.innerpochta.wmi.core.filter.CheckHeaderFilter;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Проверка кодировки названий вложений отдаваемых аттачами")
@Description("Смотрим на подготовленные файлики и проверяем, что их Content-Disposition, " +
        "отдаваемый аттачами совпадает с эталоном")
@RunWith(Parameterized.class)
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "RetrieverAttachFilenameEncoding")
public class FilenameEncodingTest extends BaseWebattachTest {

    private static String fileName = "!\"#$%&'()*+,-.0123456789:;<=>?@ABCXYZ[]^_`abcxyz{|}~»¼¿À×Øßà÷øùАБЮЯабюя";

    private String contentDisposition;
    private String userAgent;


    public FilenameEncodingTest(String engineName, String userAgent, String contentDisposition) {
        this.userAgent = userAgent;
        this.contentDisposition = contentDisposition;
    }

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{
                "None",
                "",
                "attachment;filename=\"!\"#$%&'()*+,-.0123456789:;<=>?@ABCXYZ[]^_`abcxyz{|}~»¼¿À×Øßà÷øùАБЮЯабюя\""
        });

        data.add(new Object[]{
                "WebKit",
                "Chrome: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36",
                "attachment;filename=\"!\"#$%&'()*+,-.0123456789:;<=>?@ABCXYZ[]^_`abcxyz{|}~»¼¿À×Øßà÷øùАБЮЯабюя\""
        });

        data.add(new Object[]{
                "Gecko",
                "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:70.0) Gecko/20100101 Firefox/70.0",
                "attachment;filename=\"=?UTF-8?B?ISIjJCUmJygpKissLS4wMTIzNDU2Nzg5Ojs8PT4/QEFCQ1hZWltdXl9gYWJjeHl6e3x9fs"
                + "K7wrzCv8OAw5fDmMOfw6DDt8O4w7nQkNCR0K7Qr9Cw0LHRjtGP?=\""
        });

        data.add(new Object[]{
                "Safari",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Safari/605.1.15",
                "attachment;filename*=UTF-8''%21%22%23%24%25%26%27%28%29%2A%2B%2C-.0123456789%3A%3B%3C%3D%3E%3F%40ABCXY"
                + "Z%5B%5D%5E_%60abcxyz%7B%7C%7D%7E%C2%BB%C2%BC%C2%BF%C3%80%C3%97%C3%98%C3%9F%C3%A0%C3%B7%C3%B8%C3%B9%D"
                + "0%90%D0%91%D0%AE%D0%AF%D0%B0%D0%B1%D1%8E%D1%8F"
        });

        data.add(new Object[]{
                "KHTML",
                "Mozilla/5.0 (compatible; Konqueror/4.3; Linux) KHTML/4.3.2 (like Gecko)",
                "attachment;filename=\"=?UTF-8?Q?!\"#$%&'()*+,-.0123456789:;<=3D>?@ABCXYZ[]^_`abcxyz{|}~=C2=BB=C2=B"
                + "C=C2=BF=C3=80=C3=97=C3=98=C3=9F=C3=A0=C3=B7=C3=B8=C3=B9=D0=90=D0=91=D0=AE=D0=AF=D0=B0=D0=B1=D1=8E=D1"
                + "=8F?="
        });

        data.add(new Object[]{
                "Presto",
                "Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.18",
                "attachment;filename*=UTF-8''!\"#$%&'()*+,-.0123456789:;<=>?@ABCXYZ[]^_`abcxyz{|}~»¼¿À×Øßà÷øùАБЮЯабюя"
        });

        data.add(new Object[]{
                "Trident",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko",
                "attachment;filename=\"!\"#$%&'()*+,-.0123456789:;<=>?@ABCXYZ[]^_`abcxyz{|}~�\""
        });

        return data;

    }

    @Test
    public void contentDispositionTest() throws Exception {
        File attach = AttachUtils.genFile(fileName);
        String mid = sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();

        String url = urlOfAttach(mid, attach.getName());
        logger.info("Урл для скачивания: " + url);
        String value = getContentDisposition(url);
        assertThat("Content-Disposition должен быть другим", value, equalTo(contentDisposition));

    }

    private String getContentDisposition(String url) throws IOException {
        hc.getParams().setParameter(HttpClientParams.USER_AGENT, userAgent);
        return Executor.newInstance(hc)
                .execute(Request.Get(url))
                .handleResponse(response -> {
                    String value = CheckHeaderFilter.iso8859toUTF8Value(response.getHeaders("Content-Disposition")[0]);
                    logger.info("Content-Disposition: " + value);

                    return value;
                });
    }

}
