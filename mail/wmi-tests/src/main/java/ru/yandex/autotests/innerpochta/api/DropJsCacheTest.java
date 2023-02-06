package ru.yandex.autotests.innerpochta.api;

import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.runners.MethodSorters;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.AccountInformation;
import ru.yandex.autotests.innerpochta.wmi.core.oper.debug.Debug;
import ru.yandex.autotests.innerpochta.wmi.core.oper.onlyapi.InternalDropJsCache;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CreateFileRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.junitextensions.rules.retry.RetryRule;
import ru.yandex.qatools.allure.annotations.*;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.WaitUtils.waitSmth;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.DEBUG_NODE;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.12.13
 * Time: 18:04
 * <p/>
 * [DARIA-31369]
 */
@Aqua.Test
@Title("[API] Сброс кэшей серверного js")
@Description("Проверка, что кэш сбрасывается для jsx и для jsx4wmi")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "DropJsCache")
@Issue("DARIA-31369")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DropJsCacheTest extends BaseTest {

    @Rule
    public RetryRule retry = RetryRule.retry()
            .ifMessage("Поле обновилось до сброса кэша")
            .ifMessage("Серверный кэш для JSX не сбросился")
            .ifMessage("Серверный кэш для JS4WMI не сбросился")
            .times(5).every(5, TimeUnit.SECONDS);

    public static final String PATH_JSX_FILE = "/var/wwwroot/mail/jsxapi/debug.jsx";
    public static final String PATH_JS_FILE = "/usr/share/js4wmi/versions/default/account_information.js";

    public static final String INIT_DEBUG_VALUE = Util.getRandomString();
    private String oldDebugValue;
    private String newDebugValue;

    public static CreateFileRule createFileRule = new CreateFileRule(sshAuthRule);

    @ClassRule
    public static RuleChain ruleChain = outerRule(sshAuthRule)
            .around(new CreateFileRule(sshAuthRule).path(PATH_JSX_FILE)
                    .text(getScript(INIT_DEBUG_VALUE, DEBUG_NODE)));

    /**
     * Создаем дебажный скрипт для тестирования сброса кэша для jsx
     *
     * @param value переменная в debug
     * @return
     */
    public static String getScript(String value, String debugNode) {
        return String.format("try {" +
                "(function(writer, context, http) " +
                "{ writer.setHeaderField(\"Content-Type\", \"text/xml\"); " +
                "context.write(\"<%s>%s</%s>\"); " +
                "})(User.Writer, User.WmiInstance.Context, User.WmiInstance.Context.Http)} " +
                "catch (e) {User.WmiInstance.Context.write(JSON.stringify(e));}", debugNode, value, debugNode);
    }

    /**
     * Создаем поле, которое будем вставлять в account_information
     * для тестирования сброса кэша js4wmi
     *
     * @param var переменная в debug
     * @return
     */
    public String getPair(String nodeName, String var) {
        return String.format("vr.newPair(\"%s\", \"%s\");", nodeName, var);
    }

    @Step("Должны увидеть в ручке \"jsxapi/debug\" поле <abcdef> со значением {0}")
    public void debugWithValue(String debugValue) throws IOException {
        assertThat("Серверный кэш для JSX не сбросился",
                api(Debug.class).get().via(hc).getDebugValue(DEBUG_NODE),
                equalTo(debugValue));
    }

    @Step("Должны увидеть дебажное поле {0} в account_information со значением {1}")
    public void shouldSeeDebugValue(String xpath, String value) throws IOException {
        api(AccountInformation.class).get().via(hc)
                .assertDocument("Серверный кэш для JS4WMI не сбросился", hasXPath(xpath, equalTo(value)));
    }

    /**
     * Добавит указанное поле в account_information после поля yandex_account
     *
     * @param fieldName имя поле
     * @param value     значение
     * @throws IOException
     */
    public void shouldSeeDebugValueInAccountInformation(String fieldName, String value) throws IOException {
        String comm = String.format("sudo sed -e '/.*newPair(\"%s\",.*);/d; /newPair(\"yandex_account\".*)/ a\\ " +
                        "%s' -i %s", fieldName,
                getPair(fieldName, value), PATH_JS_FILE);
        sshAuthRule.ssh().cmd(comm);
    }

    /**
     * Удаляем дебажное поле
     *
     * @throws Exception
     */
    public static void deleteDebugFieldToAccountInformation(String fieldName) throws Exception {
        String comm = String.format("sudo sed -e '/.*newPair(\"%s\",.*);/d;' -i %s", fieldName, PATH_JS_FILE);
        sshAuthRule.ssh().cmd(comm);
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @Description("Дергаем ручку сброса кэша для jsx.\n" +
            "Ожидаемый результат:\n" +
            "ответ не содержит ошибок и кэш сбросился.\n" +
            "Порты 8080 и 8079")
    public void aDropCacheJsxTest() throws IOException {
        oldDebugValue = api(Debug.class).get().via(hc).getDebugValue(DEBUG_NODE);
        api(InternalDropJsCache.class).setHost(props().on8079Host()).post().via(hc)
                .assertResponse(not(containsString("error")))
                .assertResponse(not(containsString("The requested URL was not found")));

        waitSmth(2, TimeUnit.SECONDS);
        //проверяем, что дебажное поле НЕ обновило свое значение
        debugWithValue(oldDebugValue);
        newDebugValue = Util.getRandomString();
        createFileRule.createFileWithText(PATH_JSX_FILE, getScript(newDebugValue, DEBUG_NODE));
        //проверяем, что поле, после изменения, НЕ обновилось, выпилил пока так как возможно вклчение в конфиге
//        assertThat("Поле обновилось до сброса кэша", api(Debug.class).get().via(hc).getDebugValue(DEBUG_NODE),
//                equalTo(oldDebugValue));
        //дергаем ручку сброса кэша
        api(InternalDropJsCache.class).setHost(props().on8079Host()).post().via(hc)
                .assertResponse(not(containsString("error")))
                .assertResponse(not(containsString("The requested URL was not found")));
        //проверяем многократно, что выдача изменилась, после сброса кэша
        debugWithValue(newDebugValue);
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @Description("Дергаем ручку сброса кэша для js4wmi.\n" +
            "Ожидаемый результат:\n" +
            "ответ не содержит ошибок и кэш сбросился.\n" +
            "Порты 8080 и 8079\n" +
            "Замечание:\n" +
            "В этом тесте редактируем /usr/share/js4wmi/versions/default/account_information.js")
    public void bDropCacheJs4wmiTest() throws IOException, InterruptedException {
        String xpath = String.format("//%s/text()", DEBUG_NODE);
        shouldSeeDebugValueInAccountInformation(DEBUG_NODE, INIT_DEBUG_VALUE);
        api(InternalDropJsCache.class).setHost(props().on8079Host()).post().via(hc)
                .assertResponse(not(containsString("error")))
                .assertResponse(not(containsString("The requested URL was not found")));
        //проверяем что поле обновилось
        waitSmth(2, TimeUnit.SECONDS);
        shouldSeeDebugValue(xpath, INIT_DEBUG_VALUE);

        newDebugValue = Util.getRandomString();
        shouldSeeDebugValueInAccountInformation(DEBUG_NODE, newDebugValue);
        //проверяем, что поле после изменения НЕ обновилось
        api(AccountInformation.class).get().via(hc).assertDocument("Поле обновилось до сброса кэша",
                hasXPath(xpath, equalTo(INIT_DEBUG_VALUE)));

        api(InternalDropJsCache.class).setHost(props().on8079Host()).post().via(hc)
                .assertResponse(not(containsString("error")))
                .assertResponse(not(containsString("The requested URL was not found")));
        shouldSeeDebugValue(xpath, newDebugValue);
    }

    @AfterClass
    public static void clear() throws Exception {
        deleteDebugFieldToAccountInformation(WmiConsts.DEBUG_NODE);
    }
}
