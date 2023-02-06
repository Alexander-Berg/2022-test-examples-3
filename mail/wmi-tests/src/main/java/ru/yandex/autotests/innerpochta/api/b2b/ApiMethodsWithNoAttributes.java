package ru.yandex.autotests.innerpochta.api.b2b;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.ApiTestingB2BData;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
/*
    [info] Юзер  testixzoonew:testqa

    [bug]  [WMI-226] (бага в settings_rpopper_list - выдает <human_time>сегодня в undefined:undefined</human_time>
           вместо <human_time>сегодня в 11:25</human_time>

           [DARIA-19526] отпилили settings_rpopper_filter*

 */

@Aqua.Test
@Title("[B2B] Вызов API методов без атрибутов")
@Description("Сравнение с продакшеном")
@RunWith(Parameterized.class)
@Features(MyFeatures.API_WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = "Zoo")
public class ApiMethodsWithNoAttributes extends BaseTest {

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return ApiTestingB2BData.apiHandles();
    }

    private Oper oper;

    public ApiMethodsWithNoAttributes(Oper oper, Obj obj, String opername) {
        this.oper = oper;
        this.oper.params(obj);
    }

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    @Test
    @Description("Сравниваем с продакшеном выдачу различных ручек БЕЗ атрибутов.")
    public void compareMethodsResponse() throws Exception {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        Oper respNew = oper.post().via(hc);

        oper.setHost(props().b2bUri().toString());
        Oper respBase = oper.post().via(hc);

        assertThat(respNew.toDocument(),
                equalToDoc(respBase.toDocument()).urlcomment(oper.getClass().getSimpleName())
                        //прилетает из бб-рц
                        .exclude("//yamail/account_information/birthday"));
    }
}