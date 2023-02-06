package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.IsPddUser;
import ru.yandex.autotests.innerpochta.wmi.core.oper.IsYandexUser;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 11.09.12
 * Time: 13:23
 * <p/>
 * unmodify
 */
@Aqua.Test
@Title("Яндексовый или пдд юзер")
@Description("Проверка специфичных ручек")
@Features(MyFeatures.WMI)
@Stories(MyStories.PDD)
@Credentials(loginGroup = "Zoo")
public class IsYandexOrPddUser extends BaseTest {

    @Before
    public void prepare() throws Exception {
        logger.warn("Проверка ББ ручек через вми [TESTPERS-11]");
    }

    @Test
    public void isYandexUser() throws IOException {
        jsx(IsYandexUser.class).params(new EmptyObj().set("login", authClient.acc().getLogin()))
                .post().via(hc)
                .assertDocument("Текущий логин должен принадлежать яндексовому",
                        hasXPath("//y/text()", equalTo("yes")));
    }


    @Test
    public void isNotYandexUser() throws IOException {
        String someloginnotyandex = "someloginnotyandex";
        jsx(IsYandexUser.class).params(new EmptyObj().set("login", someloginnotyandex))
                .post().via(hc)
                .assertDocument("Логин " + someloginnotyandex + " не должен принадлежать яндексовому",
                        hasXPath("//y/text()", equalTo("no")));
    }


    @Test
    public void isPddUser() throws IOException {
        String pdduser = "lanwen@kida-lo-vo.name";
        jsx(IsPddUser.class).params(new EmptyObj().set("login", pdduser))
                .post().via(hc)
                .assertDocument("Логин " + pdduser + " должен принадлежать ПДД",
                        hasXPath("//pdd/text()", equalTo("yes")));
    }


    @Test
    public void isNotPddUser() throws IOException {
        jsx(IsPddUser.class).params(new EmptyObj().set("login", authClient.acc().getLogin()))
                .post().via(hc)
                .assertDocument("Текущий логин не должен принадлежать ПДД",
                        hasXPath("//pdd/text()", equalTo("no")));
    }


}
