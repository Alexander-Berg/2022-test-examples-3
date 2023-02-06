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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;

/**
 * Unmodify
 * Добавлять модифицирующие проверки запрещено, т.к. содержит банк писем для сравнения
 */

@Aqua.Test
@Title("[B2B] Вызов API методов c атрибутами или без")
@Description("Сравнение выводов")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = "ZooNew")
public class ApiB2BTest extends BaseTest {

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

    @Test
    @Description("Сравнение выдачи произвольного метода на разных машинках.\n" +
            "В константе бейз-сервер указывается оригинал\n" +
            "в обычной проперте - с чем сравниваем")
    public void testIsMethodsEquals() throws Exception {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        Oper respNew = oper.post().via(hc);

        oper.setHost(props().productionHost());
        Oper respBase = oper.post().via(hc);

        assertThat(respNew.toDocument(),
                equalToDoc(respBase.toDocument()).urlcomment(oper.getClass().getSimpleName()));
    }
}
