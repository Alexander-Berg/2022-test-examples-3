package ru.yandex.autotests.innerpochta.hound.v2.positive;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.hound.Messages;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessagesForTab;

@Aqua.Test
@Title("[HOUND] Ручка v2/messages_unread_by_tab")
@Description("Тесты на ручку v2/message_unread_by_tab")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2MessagesUnreadByTabTest")
@RunWith(DataProviderRunner.class)
public class MessagesUnreadByTabPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Должны вернуть только непрочитанные письма из данного таба")
    @UseDataProvider("existingTabs")
    public void ShouldGetOnlyUnreadMessagesFormTab(Tab tab) throws Exception {
        Tab notExpectedTab = tab != Tab.RELEVANT ? Tab.RELEVANT : Tab.NEWS;
        makeMessageForTab(authClient, notExpectedTab);
        List<String> mids = makeMessagesForTab(authClient, 2, tab);

        // Both messages unread
        assertHasOnlyMids(getMessagesUnreadByTab(tab), mids);

        // One message unread
        Mops.mark(authClient, new MidsSource(mids.get(0)), StatusParam.READ).post(shouldBe(okSync()));
        assertHasOnlyMids(getMessagesUnreadByTab(tab), asList(mids.get(1)));

        // No messages unread
        Mops.mark(authClient, new MidsSource(mids.get(1)), StatusParam.READ).post(shouldBe(okSync()));
        assertHasOnlyMids(getMessagesUnreadByTab(tab), new ArrayList<>());
    }

    private List<Envelope> getMessagesUnreadByTab(Tab tab) {
        return apiHoundV2().messagesUnreadByTab()
                .withUid(uid())
                .withTab(tab.getName())
                .withFirst("0")
                .withCount("10")
                .get(shouldBe(ok200()))
                .body().as(Messages.class).getEnvelopes();
    }

    private List<String> getMids(List<Envelope> envelopes) {
        return envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
    }

    private void assertHasOnlyMids(List<Envelope> envelopes, List<String> mids) {
        if (mids.isEmpty()) {
            assertThat("Должны получить пустой список", envelopes, empty());
        } else {
            assertThat("Число писем в ответе не совпадает с ожидаемым",
                    envelopes.size(), equalTo(mids.size()));
            assertThat("Должны получить все непрочитанные письма в ответе",
                    getMids(envelopes), containsAll(mids));
        }
    }

    private static Matcher<List<String>> containsAll(List<String> messages) {
        return allOf(messages.stream()
                .map(Matchers::hasItem)
                .toArray(Matcher[]::new));
    }
}
