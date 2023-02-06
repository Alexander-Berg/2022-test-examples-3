package ru.yandex.autotests.innerpochta.webattach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 29.01.13
 * Time: 20:34
 * <p/>
 * Ахтунг! Менять юзера нельзя.
 * Тестовыми данными забиты mid-ы и имена аттачей в них
 * Тестирование полурочное.
 * Прописываем в хостах:
 * #attach-qa.mail.yandex.net
 * 95.108.253.172   webattach.mail.yandex.net
 * и запускаем тесты
 */
@Aqua.Test
@Title("Проверка ContentType отдаваемых аттачами")
@Description("Смотрим на подготовленные файлики и проверяем, что их контент тип, " +
                "отдаваемый аттачами совпадает с эталоном")
@RunWith(Parameterized.class)
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Scope(Scopes.TESTING)
@Credentials(loginGroup = "RetrieverContentType")
public class ContentTypeTest extends BaseWebattachTest {

    private String contentType;
    private String mid;
    private String name;


    public ContentTypeTest(String mid, String name, String contentType) {
        this.mid = mid;
        this.name = name;
        this.contentType = contentType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{
                "157907461934678678",
                "Copy of Latina 130338.xls",
                "application/vnd.ms-excel;filename=\"Copy%20of%20Latina%20130338.xls\""
        });

        data.add(new Object[]{
                "157907461934678679",
                "4874_illustratorbevelv10.ai",
                "application/postscript;filename=\"4874_illustratorbevelv10.ai\""
        });

        data.add(new Object[]{
                "157907461934678680",
                "Шелфтокер Optimus G(правый).jpg",
                "image/jpeg;filename=\"Шелфтокер%20Optimus%20G(правый).jpg\""
        });

        data.add(new Object[]{
                "157907461934678681",
                "PastedGraphic-1.tiff",
                "image/tiff;filename=\"PastedGraphic-1.tiff\""
        });

        data.add(new Object[]{
                "157907461934678682",
                "2e65ee71f296.png",
                "image/png;filename=\"2e65ee71f296.png\""
        });

        // DARIA-44210
        data.add(new Object[]{
                "157907461934678683",
                "com.digiplex.game.apk",
                "application/vnd.android.package-archive;filename=\"com.digiplex.game.apk\""
        });

        data.add(new Object[]{
                "157907461934678684",
                "Flappy_Ice_Birdie.xap",
                "application/x-silverlight-app;filename=\"Flappy_Ice_Birdie.xap\""
        });

        data.add(new Object[]{
                "157907461934678685",
                "ticket.pkpass",
                "application/vnd.apple.pkpass;filename=\"ticket.pkpass\""
        });

        // DARIA-45272
        data.add(new Object[]{
                "157907461934678686",
                "Контрагенты для_1С (ОГЭ-2).xlsm",
                "application/vnd.ms-excel.sheet.macroenabled.12;filename=\"Контрагенты%20для_1С%20(ОГЭ-2).xlsm\""
        });

        return data;

    }

    @Test
    public void downloadXSL() throws Exception {
        String url = urlOfAttach(mid, name);
        logger.info("Урл для скачивания: " + url);
        String value = getContentType(url);
        assertThat("Content-Type должен быть другим", value, equalTo(contentType));

    }

    private String getContentType(String url) throws IOException {
        return Executor.newInstance(hc)
                .execute(Request.Get(url))
                .handleResponse(response -> {
                    String value = CheckHeaderFilter.iso8859toUTF8Value(response.getEntity().getContentType());
                    logger.info("Content-Type: " + value);

                    return value;
                });
    }

}
