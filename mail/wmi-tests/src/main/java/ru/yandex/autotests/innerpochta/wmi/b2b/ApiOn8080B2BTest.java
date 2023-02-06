package ru.yandex.autotests.innerpochta.wmi.b2b;

import org.junit.Before;
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
import ru.yandex.qatools.allure.annotations.*;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;

/**
 * /**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.10.13
 * Time: 15:22
 * <p/>
 * Unmodify
 * Добавлять модифицирующие проверки запрещено, т.к. содержит банк писем для сравнения
 */
@Aqua.Test
@Title("[B2B] Вызов API методов c атрибутами или без на 8079 порту")
@Description("Сравнение выводов")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = "ZooNew")
public class ApiOn8080B2BTest extends BaseTest {

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    //{2} имя ручки, которую проверяем
    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return ApiTestingB2BData.apiHandles();
    }

    @Parameterized.Parameter
    public Oper oper;

    @Parameterized.Parameter(1)
    public Obj obj;

    @Parameterized.Parameter(2)
    public String comment;

    @Before
    public void setUp() throws Exception {
        oper.params(obj);
    }

    /**
     * @throws Exception
     */
    @Test
    @Issue("DARIA-30314")
    @Description("Сравнение выдачи произвольного метода на разных машинках на порту 8079.\n" +
            "В константе бейз-сервер указывается оригинал\n" +
            "в обычной проперте - с чем сравниваем\n" +
            "since verstka-api-1-76")
    public void testIsMethodsEqualsOn8080() throws Exception {
        logger.warn("[DARIA-30314]");
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        Oper respNew = oper.setHost(props().on8079Host()).post().via(hc);

        oper.setHost(props().on8079Host(props().b2bUri().toString())).post().via(hc);
        Oper respBase = oper.post().via(hc).withDebugPrint();

        assertThat(respNew.toDocument(),
                equalToDoc(respBase.toDocument()).urlcomment(oper.getClass().getSimpleName()));
    }
}
