package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.*;

@Aqua.Test
@Title("[HOUND] Ручка v2/first_envelope_date_in_tab")
@Description("Тесты на ручку v2/first_envelope_date_in_tab")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "GetFirstEnvelopeDateInTabNegativeTest")
public class FirstEnvelopeDateInTabNegativeTest extends BaseHoundTest {

    public static final String NOT_EXIST_TAB = "1233219TAB";

    @Test
    @Title("first_envelope_date_in_tab с некорректным именем таба")
    public void firstEnvelopeDateInTabWithIncorrectTab() {
        apiHoundV2().firstEnvelopeDateInTab().withUid(uid()).withTab(NOT_EXIST_TAB).
                get(shouldBe(invalidArgument(CoreMatchers.equalTo("invalid tab argument"))));
    }

    @Test
    @Title("first_envelope_date_in_tab с пустым именем таба")
    public void firstEnvelopeDateInTabWithEmptyTab() {
        apiHoundV2().firstEnvelopeDateInTab().withUid(uid()).withTab("").
                get(shouldBe(invalidArgument(CoreMatchers.equalTo("tab is missing or empty"))));
    }

    @Test
    @Title("first_envelope_date_in_tab с 0 количеством писем в корректном табе")
    public void firstEnvelopeDateInTabWithCorrectTabAndZeroCountOfMessages() {
        apiHoundV2().firstEnvelopeDateInTab().withUid(uid()).withTab(Tabs.Tab.NEWS.getName()).
                get(shouldBe(ok200()));
    }
}