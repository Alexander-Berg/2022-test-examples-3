package ru.yandex.autotests.innerpochta.wmi.rightColumn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageHistoryNearestHandlersObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageHistoryNearestHandlers;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 08.11.13
 * Time: 14:22
 * [DARIA-30953]
 */
@Aqua.Test
@Title("Тестирование правой колонки. Проверяем кнопки вложений и ссылок")
@Description("Проверяет, что кнопки работают правильно")
@Features(MyFeatures.WMI)
@Stories(MyStories.RIGHT_COLUMN)
@RunWith(Parameterized.class)
@Credentials(loginGroup = "CommonRightColumn")
@Issue("DARIA-30953")
public class CommonRightColumn extends BaseTest {

    public static final String MID = "2300000002864980784";

    public static final String IMAGE_URl_JPEG =
            "http://img-fotki.yandex.ru/get/9300/219421176.0/0_d2a24_8d0e0fea_orig";

    @Parameterized.Parameter
    public String tab;

    //параметризуем по кнопке
    @Parameterized.Parameters(name = "Button-{0}")
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> tabs = new ArrayList<>();
        tabs.add(new Object[]{"link"});
        tabs.add(new Object[]{"attachments"});

        return tabs;
    }

    @Test
    @Issue("DARIA-31417")
    @Description("Проверяем что при нажатии на кнопоки\n" +
            "правой колонки не возвращаются ошибки\n" +
            "Тестовые данные должны быть созданы заранее,\n" +
            "так как не успевают проиндексироваться поиском\n" +
            "DARIA-31417")
    public void buttonsShouldWork() throws Exception {
        //createTestData();
        jsx(MessageHistoryNearestHandlers.class).params(MessageHistoryNearestHandlersObj.empty()
                .setIds(MID)
                .setTab(tab)
                .setPage("message")
                .setService("mail")
                .setLocale("ru")
                .setProduct("RUS"))
                .post().via(hc).notWithError();
    }

    /**
     * Формируем письмо на котором будем проверять правую колонку
     * В письме содержится ссылка и аттач
     *
     * @throws Exception
     */
    public String createTestData() throws Exception {
        File attach = downloadFile(IMAGE_URl_JPEG, Util.getRandomString(), hc);
        MailSendMsgObj msgObj = MailSendMsgObj.msg()
                .setSend("http://yandex.ru")
                .setTo(authClient.acc().getSelfEmail()).addAtts(attach);
        return sendWith.msg(msgObj).waitDeliver().send().getMid();
    }

}
