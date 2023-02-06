package ru.yandex.autotests.innerpochta.wmi.b2b;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.api.b2b.ApiMethodsWithNoAttributes;
import ru.yandex.autotests.innerpochta.data.JsxTestingB2BData;
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

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.04.15
 * Time: 18:00
 */
@Aqua.Test
@Title("[B2B] Вызов JSX методов без атрибутов")
@Description("Сравнение с продакшеном")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = JsxMethodsWithNoAttributes.LOGIN_GROUP)
public class JsxMethodsWithNoAttributes extends BaseTest {

    public static final String LOGIN_GROUP = "ZooNew";

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return JsxTestingB2BData.jsxHandles();
    }

    private Oper oper;

    public JsxMethodsWithNoAttributes(Oper oper, Obj obj, String opername) {
        this.oper = oper;
        this.oper.params(obj);
    }

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    @Test
    @Description("Сравниваем с продакшеном выдачу различных jsx ручек БЕЗ атрибутов.")
    public void compareMethodsResponse() throws Exception {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        Oper respNew = oper.post().via(hc);

        oper.setHost(props().b2bUri().toString());
        Oper respBase = oper.post().via(hc);

        assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()).urlcomment(oper.getClass().getSimpleName())
                        //прилетает из бб-рц
                        .exclude("//yamail/account_information/birthday"));
    }
}