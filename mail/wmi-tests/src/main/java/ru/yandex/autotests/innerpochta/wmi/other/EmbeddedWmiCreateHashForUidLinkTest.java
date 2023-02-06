package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.CreateHashJsxApiScript;
import ru.yandex.autotests.innerpochta.wmi.core.oper.onlyapi.InternalDropJsCache;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CreateFileRule;
import ru.yandex.qatools.allure.annotations.*;
import ru.yandex.qatools.allure.model.SeverityLevel;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.fromClasspath;

@Aqua.Test
@Title("jsx embedded_wmi: метод createHashForUidLink")
@Description("Тест embedded метода createHashForUidLink - результат должен проходить проверку " +
        "через validateHashForUidLink")
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@Issue("MAILPG-968")
public class EmbeddedWmiCreateHashForUidLinkTest extends BaseTest {
    //Скрипт создает хеш через новый метод и проверяет его валидность через старый метод проверки
    public static final String FILEPATH = "/var/wwwroot/mail/jsxapi/create_hash.jsx";
    public static String getScript() {
        try {
            return fromClasspath("jsxapi/createHashForUidLink.jsx");
        } catch (Exception e) {
            return "";
        }
    }

    @ClassRule
    public static RuleChain ruleChain = outerRule(sshAuthRule)
            .around(new CreateFileRule(sshAuthRule).path(FILEPATH)
                    .text(getScript()));

    @Before
    public void dropCache(){
        //Сбрасываем кеш, чтобы raw.jsx, который используется в других тестах, перечитался
        api(InternalDropJsCache.class).setHost(props().on8079Host()).post().via(hc)
                .assertResponse(not(containsString("error")))
                .assertResponse(not(containsString("The requested URL was not found")));
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @Description("Проверяем, что скрипт вернет true")
    public void embeddedWmiCreateHashForUidLinkTest() throws IOException {
        api(CreateHashJsxApiScript.class).setHost(props().on8079Host()).post().via(hc).isTrue();
    }
}
