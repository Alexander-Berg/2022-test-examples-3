package ru.yandex.autotests.innerpochta.webattach;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.util.LogToFileUtils;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 29.01.13
 * Time: 20:34
 */
@Aqua.Test
@Title("Проверка ContentType для zip, отдаваемых аттачами")
@ru.yandex.qatools.allure.annotations.Description("Смотрим на подготовленные файлики и проверяем, что их контент тип, " +
                "отдаваемый аттачами совпадает с эталоном")
@RunWith(Parameterized.class)
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Scope(Scopes.TESTING)
@Credentials(loginGroup = "RetrieverContentType")
public class ZipDownloadTest extends BaseWebattachTest {

    public static final String NAME = "some.zip";
    public static final String APPLICATION_X_ZIP = "application/zip";
    private String encoding;
    private String userAgent;
    private String header; //Accept-Language
    private String mid;
    private Matcher<List<ZipEntry>> matcher;


    public ZipDownloadTest(String mid,
                           String encoding,
                           String userAgent, String header,
                           Matcher<List<ZipEntry>> matcher) {
        this.mid = mid;
        this.encoding = encoding;
        this.userAgent = userAgent;
        this.header = header;
        this.matcher = matcher;
    }

    @Parameterized.Parameters(name = "{index}{1}")
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<>();


        data.add(new Object[]{
                "157907461934678687",
                "UTF-8",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:18.0) Gecko/20100101 Firefox/18.0",
                null,
                hasItems(hasFileWithName("фыдвфыдвлофыждвоыждвлоаыджфваожфдывлоажфдывлоаждфылвоаждфыловаждф" +
                                "ыловаждфыоважофываждофывждаофыжвдаожфывоажфывоажфывоажфыоважфыловаждлфоываждлоыфвжао" +
                                "фыжвдаофыжвоаждфылвоаждфылвоаждлыфоваждофываджолфывждалофыжвдалофывждалофыжвдлаофыждв" +
                                "лоафывждоажфывдло.pdf"),
                        hasFileWithName("Re_ Поиски внутри торрент-са"))
        });


        data.add(new Object[]{
                "157907461934678687",
                "UTF-8",
                "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:15.0) Gecko/20100101 Firefox/15.0.1",
                null,
                hasItems(hasFileWithName("фыдвфыдвлофыждвоыждвлоаыджфваожфдывлоажфдывлоаждфылвоаждфыловаждф" +
                                "ыловаждфыоважофываждофывждаофыжвдаожфывоажфывоажфывоажфыоважфыловаждлфоываждлоыфвжао" +
                                "фыжвдаофыжвоаждфылвоаждфылвоаждлыфоваждофываджолфывждалофыжвдалофывждалофыжвдлаофыждв" +
                                "лоафывждоажфывдло.pdf"),
                        hasFileWithName("Re_ Поиски внутри торрент-са"))
        });


        data.add(new Object[]{
                "157907461934678687",
                "UTF-8",
                "Mozilla/5.0 (X11; Linux i686) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5",
                null,
                hasItems(hasFileWithName("фыдвфыдвлофыждвоыждвлоаыджфваожфдывлоажфдывлоаждфылвоаждфыловаждф" +
                                "ыловаждфыоважофываждофывждаофыжвдаожфывоажфывоажфывоажфыоважфыловаждлфоываждлоыфвжао" +
                                "фыжвдаофыжвоаждфылвоаждфылвоаждлыфоваждофываджолфывждалофыжвдалофывждалофыжвдлаофыждв" +
                                "лоафывждоажфывдло.pdf"),
                        hasFileWithName("Re_ Поиски внутри торрент-са"))
        });


        data.add(new Object[]{
                "157907461934678687",
                "UTF-8",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120421 Gecko Firefox/11.0",
                null,
                hasItems(hasFileWithName("фыдвфыдвлофыждвоыждвлоаыджфваожфдывлоажфдывлоаждфылвоаждфыловаждф" +
                                "ыловаждфыоважофываждофывждаофыжвдаожфывоажфывоажфывоажфыоважфыловаждлфоываждлоыфвжао" +
                                "фыжвдаофыжвоаждфылвоаждфылвоаждлыфоваждофываджолфывждалофыжвдалофывждалофыжвдлаофыждв" +
                                "лоафывждоажфывдло.pdf"),
                        hasFileWithName("Re_ Поиски внутри торрент-са"))
        });


        data.add(new Object[]{
                "157907461934678691",
                "UTF-8",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120421 Gecko Firefox/11.0",
                "tr",
                hasItems(hasFileWithName("Çevir"),
                        hasFileWithName("Tüm servisler."))  // плющит java от этой кодировки
        });


        data.add(new Object[]{
                "157907461934678687",
                "CP866",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.9.0.19) Gecko/2010031422 Firefox/3.0.19" +
                        " (.NET CLR 3.5.30729) YB/5.1.1",
                null,
                hasItems(hasFileWithName("фыдвфыдвлофыждвоыждвлоаыджфваожфдывлоажфдывлоаждфылвоаждфыловаждф" +
                                "ыловаждфыоважофываждофывждаофыжвдаожфывоажфывоажфывоажфыоважфыловаждлфоываждлоыфвжао" +
                                "фыжвдаофыжвоаждфылвоаждфылвоаждлыфоваждофываджолфывждалофыжвдалофывждалофыжвдлаофыждв" +
                                "лоафывждоажфывдло.pdf"),
                        hasFileWithName("Re_ Поиски внутри торрент-са"))
        });

        return data;

    }

    @Test
    public void downloadXSL() throws Exception {
        String url = urlOfAllAttachesZipArchive(mid, NAME);
        logger.info("Урл для скачивания: " + url);

        File zipFile = download(url + "&archive=zip", NAME);

        ZipFile zip = new ZipFile(zipFile, encoding);
        List<ZipEntry> enries = getFiles(zip);


        assertThat(enries, matcher);
    }

    private File download(final String url, final String name) throws IOException {
        hc.getParams().setParameter(HttpClientParams.USER_AGENT, userAgent);
        hc.addRequestInterceptor(addAccLanguageHeader());
        return Executor.newInstance(hc)
                .execute(Request.Get(url + "&name=" + NAME))
                .handleResponse(getZip(name));
    }

    private ResponseHandler<File> getZip(final String name) {
        return response -> {
            String contenttype = response.getEntity().getContentType().getValue();
            assertThat(contenttype, equalTo(APPLICATION_X_ZIP + ";filename=\"" + NAME + "\""));

            File file = LogToFileUtils.getLogFile(name, "zip");
            FileUtils.writeByteArrayToFile(file, EntityUtils.toByteArray(response.getEntity()));

            return file;
        };
    }

    private HttpRequestInterceptor addAccLanguageHeader() {
        return (request, context) -> {
            if (null != header) {
                request.addHeader("Accept-Language", header);
            }
        };
    }

    private List<ZipEntry> getFiles(ZipFile zip) {
        List<ZipEntry> entryList = new ArrayList<ZipEntry>();
        Enumeration entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            logger.info("File name: " + zipEntry.getName());
            entryList.add(zipEntry);
        }

        return entryList;
    }

    public static Matcher<ZipEntry> hasFileWithName(final String name) {
        return new TypeSafeMatcher<ZipEntry>() {
            @Override
            protected boolean matchesSafely(ZipEntry item) {
                return item.getName().contains(name);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Файл с содержанием в имени ").appendValue(name);
            }

            @Override
            protected void describeMismatchSafely(ZipEntry item, Description mismatchDescription) {
                mismatchDescription.appendText("\n").appendText("имя - ").appendValue(item.getName());
            }
        };
    }


}
