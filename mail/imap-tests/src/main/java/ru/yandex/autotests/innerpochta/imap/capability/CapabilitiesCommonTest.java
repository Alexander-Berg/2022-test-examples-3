package ru.yandex.autotests.innerpochta.imap.capability;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CapabilityRequest.capability;
import static ru.yandex.autotests.innerpochta.imap.requests.LoginRequest.login;
import static ru.yandex.autotests.innerpochta.imap.responses.CapabilityResponse.Capabilities;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.03.14
 * Time: 0:12
 * <p>
 * MAILPROTO-2339
 */
@Aqua.Test
@Title("Команда CAPABILITIES. Общие тесты.")
@Features({ImapCmd.CAPABILITY})
@Stories(MyStories.COMMON)
@Description("Проверяем выдачу до авторизации и после авторизации")
public class CapabilitiesCommonTest extends BaseTest {
    private static Class<?> currentClass = CapabilitiesCommonTest.class;


    @Rule
    public ImapClient imap = new ImapClient();

    @Test
    @Description("Проверяем CAPABILITIES до логина [MAILPROTO-2339]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("56")
    public void capabilitiesShouldSeeRequiredItemsBeforeLogin() {
        imap.request(capability()).shouldBeOk().shouldContainCapabilities(Capabilities.beforeLogin());
    }

    @Test
    @Description("Проверяем CAPABILITIES после логина [MAILPROTO-2339]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("57")
    public void capabilitiesShouldSeeRequiredItemsAfterLogin() {
        imap.request(login(currentClass.getSimpleName()));
        imap.request(capability()).shouldBeOk().shouldContainCapabilities(Capabilities.afterLogin());
        imap.request(capability()).shouldBeOk().shouldContainCapabilities(Capabilities.afterLogin());
    }

    @Test
    @Description("Делаем дважды до логина CAPABILITIES [MAILPROTO-2339]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("55")
    public void doubleCapabilitiesTest() {
        imap.request(capability()).shouldBeOk().shouldContainCapabilities(Capabilities.beforeLogin());
        imap.request(capability()).shouldBeOk().shouldContainCapabilities(Capabilities.beforeLogin());
    }
}
