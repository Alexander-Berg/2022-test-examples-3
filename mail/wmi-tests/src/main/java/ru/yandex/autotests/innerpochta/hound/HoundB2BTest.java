package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.HoundApiData;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraint;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes.PRODUCTION;
import static ru.yandex.autotests.innerpochta.wmi.other.KcufCompTimeTest.X_REAL_IP_HEADER;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraints.ignore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.10.13
 * Time: 15:36
 * <p/>
 * Для ручек folder и label запросы без кук
 */
@Aqua.Test
@Title("[HOUND][B2B] Тестирование сервиса hound на 9091 порту")
@Description("Тесты на различные ручки hound-а")
@Features(MyFeatures.HOUND)
@Stories(MyStories.B2B)
@RunWith(Parameterized.class)
@Credentials(loginGroup = "Yplatform")
@Scope(PRODUCTION)
@IgnoreForPg("MAILPG-2767")
public class HoundB2BTest extends BaseTest {

    public static final String LOGIN_GROUP = "Yplatform";

    public static final BeanConstraint IGNORE_DEFAULT = ignore("age", "birthdayDate", "httpOnly", "timestamp",
            "timer_logic", "timer_db", "tscn", "ckey", "jane", "account_information/age", "account_information/ticket",
            "threads_by_folder/threadLabels[0]/tscn", "threads_by_folder/threadLabels[1]/tscn",
            //for api_,mobile
            "account_information/account-information/ckey", "account_information/account-information/timer_logic",
            "get_user_parameters/body/timer_logic", "yamail_status/yamail_status/timer_logic",
            "settings_setup/body/timer_logic", "files[0]/url", "files[0]/preview",
            // FIXME: remove from ignore after MAILPG-1391
            "account_information");

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return HoundApiData.apiHandles();
    }

    private Oper oper;
    private Obj obj;

    public HoundB2BTest(Oper oper, Obj obj, String opername) {
        this.oper = oper;
        this.obj = obj;
    }

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    @Test
    @Title("Сравние yplatform ручек с продакшеном (GET)")
    @Description("Сравниваем с продакшеном выдачу GET методов (с заголовками) Yplatform на 9090 порту.")
    public void compareMethodsResponseWithHeaders() throws IOException {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        oper.header(X_REAL_IP_HEADER, "37.140.175.107")
                .header("X-Original-Host", "mail.yandex.ru");

        Oper respNew = oper.params(obj).setHost(props().houndUri()).get().via(authClient);
        Oper respBase = oper.params(obj).setHost(props().houndUri(props().b2bUri().toString())).get().via(authClient);

        Map expectedMap = JsonUtils.getObject(respBase.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respNew.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(IGNORE_DEFAULT));
    }

    @Test
    @Title("Сравние yplatform ручек с продакшеном (POST и без заголовков)")
    @Issue("DARIA-35785")
    @Description("Сравниваем с продакшеном выдачу POST методов Yplatform на 9090 порту.\n" +
            "Делаем тоже самое, но POST запросы\n" +
            "DARIA-35785]")
    public void compareMethodsResponsePostRequest() throws IOException {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());

        Oper respNew = oper.params(obj.setContent(obj.asGet(false).replaceFirst("&", "")))
                .setHost(props().houndUri()).post().via(authClient);

        Oper respBase = oper.setHost(props().houndUri(props().b2bUri().toString())).post().via(authClient);


        Map expectedMap = JsonUtils.getObject(respBase.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respNew.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(IGNORE_DEFAULT));
    }
}