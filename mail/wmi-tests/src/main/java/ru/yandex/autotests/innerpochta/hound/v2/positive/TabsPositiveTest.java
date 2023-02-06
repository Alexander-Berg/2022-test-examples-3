package ru.yandex.autotests.innerpochta.hound.v2.positive;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.Tab;
import ru.yandex.autotests.innerpochta.beans.hound.V2TabsResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessagesForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/tabs")
@Description("Тесты на ручку v2/tabs")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2TabsTest")
@RunWith(DataProviderRunner.class)
public class TabsPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Ручка v2/tabs с различными табами")
    @Description("Проверяем, что ручка возвращает список табов с правильными счетчиками")
    @UseDataProvider("existingTabs")
    public void shouldReceiveTabsListWithCounters(Tabs.Tab tab) throws Exception {
        int totalCount = 3, readCount = 2;

        List<String> mids = makeMessagesForTab(authClient, totalCount, tab);

        Mops.mark(authClient, new MidsSource(mids.subList(0, readCount)), StatusParam.READ)
                .post(shouldBe(okSync()));

        List<Tab> tabs = getTabs();
        assertThat("Неверные значения для таба " + tab.getName(), tabs, hasItem(allOf(
                hasProperty("type", equalTo(tab.getName())),
                hasProperty("messagesCount", equalTo(Long.valueOf(totalCount))),
                hasProperty("unreadMessagesCount", equalTo(Long.valueOf(totalCount - readCount)))
        )));
    }

    @Test
    @Title("Ручка v2/tabs счетчики свежих писем")
    @Description("Проверяем, что ручка возвращает правильные счетчики свежих писем")
    public void shouldReceiveTabsListWithFreshCounters() {
        int totalCount = 3;
        Long currentFreshCount = getTabs().stream()
                .filter(t -> t.getType().equals(Tabs.Tab.RELEVANT.getName()))
                .findFirst().get().getFreshMessagesCount();

        sendWith(authClient).viaProd().count(totalCount).send().waitDeliver();

        List<Tab> tabs = getTabs();
        assertThat("В табе relevant неверные счетчики свежих писем", tabs, hasItem(allOf(
                hasProperty("type", equalTo(Tabs.Tab.RELEVANT.getName())),
                hasProperty("freshMessagesCount", equalTo(currentFreshCount + totalCount)),
                hasProperty("isUnvisited", equalTo(true))
        )));

        apiHoundV2().resetTabUnvisited()
                .withUid(uid()).withTab(Tabs.Tab.RELEVANT.getName())
                .get(shouldBe(ok200()));

        tabs = getTabs();
        assertThat("В табе relevant неверные счетчики свежих писем", tabs, hasItem(allOf(
                hasProperty("type", equalTo(Tabs.Tab.RELEVANT.getName())),
                hasProperty("freshMessagesCount", equalTo(Long.valueOf(0))),
                hasProperty("isUnvisited", equalTo(false))
        )));
    }

    private List<Tab> getTabs() {
        return apiHoundV2().tabs()
                .withUid(uid())
                .get(shouldBe(ok200()))
                .body().as(V2TabsResponse.class)
                .getTabs();
    }
}
