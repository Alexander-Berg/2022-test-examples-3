package ru.yandex.autotests.innerpochta.wmi.b2b;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.JsonToXML;
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.any;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;

/**
 * Unmodify
 * Добавлять модифицирующие проверки запрещено, т.к. содержит банк писем для сравнения
 */
@Aqua.Test
@Title("[B2B] Вызов хендлеров лайта")
@Description("Сравнение выводов")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories({MyStories.B2B, MyStories.LITE})
@Credentials(loginGroup = "ZooNew")
public class ApiLiteHandlersFastB2BTest extends BaseTest {

    public static final String JSX_PATH = "/lite/handlers/handlers.jsx";
    public static final String HANDLERS = "_handlers";
    public static final String PREFIX = JSX_PATH + "?" + HANDLERS + "=";

    @Rule
    public LogIPRule logIpRule = new LogIPRule();


    @Parameterized.Parameters
    public static Collection<Object[]> messagesId() throws Exception {
        return newArrayList(
                new Object[]{
                        any(PREFIX, "labels", "Запрос меток"),
                        new EmptyObj()},

                new Object[]{
                        any(PREFIX, "messages", "Запрос сообщений"),
                        new EmptyObj()
                },

                new Object[]{
                        any(PREFIX, "folders", "Запрос папок"),
                        new EmptyObj()
                },
                new Object[]{
                        any(PREFIX, "abook-suggest", "Саджест контактов")
                                .filters(new JsonToXML()),
                        new EmptyObj()
                }
        );
    }

    @Parameterized.Parameter
    public Oper oper;

    @Parameterized.Parameter(1)
    public Obj obj;

    @Before
    public void setUp() throws Exception {
        oper.params(obj);
    }

    @Test
    @Description("Сравнение выдачи произвольного метода lite на разных машинках.\n" +
            "в константе бейз-сервер указывается оригинал\n" +
            "в обычной проперте - с чем сравниваем")
    public void testIsMethodsEquals() throws Exception {
        Oper respNew = oper.post().via(hc);
        oper.setHost(props().productionHost());
        Oper respBase = oper.post().via(hc);

        System.out.println(respNew);

        assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }

}
