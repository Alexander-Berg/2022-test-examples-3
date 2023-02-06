package ru.yandex.autotests.innerpochta.b2b;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

@Aqua.Test
@Title("B2B-тесты на отображение письма")
@Description("Сравнениваем выдачу писем с продакшеном, пачками")
@RunWith(Parameterized.class)
@Features(MyFeatures.B2B)
@Stories({MyStories.B2B, MyStories.MESSAGE_BODY})
public class MessageB2BTest7 extends MessageB2BTestBase {
    private static final int INDEX = 7;

    public MessageB2BTest7(String mid) {
        super(mid);
    }

    @Parameterized.Parameters(name = "MID={0}")
    public static Collection<Object[]> mids() throws Exception {
        return getMids(INDEX);
    }

    @Test
    @Title("[B2B] Вывод сообщения через через mbody")
    public void mbodyMessages() throws Exception {
        checkMbodyMessages();
    }
}
