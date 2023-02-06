package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.GetFirstEnvelopeDateObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetFirstEnvelopeDate;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

@Aqua.Test
@Title("Тесты на ручку get_first_envelope_date")
@Description("Эту ручку используют, чтобы рисовать пейджер по датам внизу списка писем.")
@Features(MyFeatures.WMI)
@Stories(MyStories.MESSAGES_LIST)
@Issue("DARIA-52616")
@Credentials(loginGroup = "GetFirstEnvelopeDateTest")
public class GetFirstEnvelopeDateTest extends BaseTest {
    @Test
    @Issue("MAILPG-518")
    @Title("get_first_envelope_date с пустым fid")
    public void getFirstEnvelopeDateWithEmptyFidShouldSeeError() {
        jsx(GetFirstEnvelopeDate.class).params(GetFirstEnvelopeDateObj.getEmptyObj().setCurrentFolder(""))
                .post().via(hc).errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);
    }

    @Test
    @Issue("MAILPG-518")
    @Title("get_first_envelope_date без fid")
    public void getFirstEnvelopeDateWithoutFidShouldSeeError() {
        jsx(GetFirstEnvelopeDate.class).params(GetFirstEnvelopeDateObj.getEmptyObj())
                .post().via(hc).errorcode(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001);
    }
}
