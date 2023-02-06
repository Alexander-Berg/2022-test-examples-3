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
import ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessagesForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;

@Aqua.Test
@Title("[HOUND] Ручка v2/messages_in_tab_with_pins")
@Description("Тесты на ошибки ручки v2/messages_in_tab_with_pins")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2MessagesInTabWithPinsTest")
@RunWith(DataProviderRunner.class)
public class MessagesInTabWithPinsPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    public List<String> makePinnedMids() throws Exception {
        String pinLid = Hound.getLidBySymbolTitle(authClient, PINNED);
        assertThat("У пользователя нет метки <pinned>", pinLid, IsNot.not(equalTo("")));
        List<String> pinnedMids = new ArrayList<>();
        pinnedMids.add(makeMessageForTab(authClient, Tab.EMPTY));
        pinnedMids.add(makeMessageForTab(authClient, Tab.RELEVANT));
        pinnedMids.add(makeMessageForTab(authClient, Tab.NEWS));
        pinnedMids.add(makeMessageForTab(authClient, Tab.SOCIAL));

        Mops.label(authClient, new MidsSource(pinnedMids), asList(pinLid))
                .post(shouldBe(okSync()));
        return pinnedMids;
    }

    @Test
    @Title("Должны вернуть запиненные письма из всех табов и обычные письма только из данного таба")
    @UseDataProvider("existingTabs")
    public void shouldGetMessagesForRequestedTabWithPinned(Tab tab) throws Exception {
        List<String> expected = makePinnedMids();
        expected.add(makeMessageForTab(authClient, tab));
        Tab notExpectedTab = tab != Tab.RELEVANT ? Tab.RELEVANT : Tab.NEWS;
        List<String> notExpected = asList(makeMessageForTab(authClient, notExpectedTab));

        assertOnlyExpected(getMids(getMessagesInTabWithPins(tab, 0, 100)),
                expected, notExpected);
    }

    @Test
    @Title("Должны вернуть запиненные письма раньше обычных")
    @UseDataProvider("existingTabs")
    public void shouldGetPinnedMessagesFirstAndThenFromTab(Tab tab) throws Exception {
        List<String> pinnedMids = makePinnedMids();
        List<String> mids = makeMessagesForTab(authClient, 5, tab);

        // Get only pinned messages
        assertOnlyExpected(getMids(getMessagesInTabWithPins(tab, 0, pinnedMids.size())),
                pinnedMids, mids);

        // Get only not pinned messages
        assertOnlyExpected(getMids(getMessagesInTabWithPins(tab, pinnedMids.size(), 10)),
                mids, pinnedMids);

        // Get mixed messages
        List<String> mixed = getMids(getMessagesInTabWithPins(tab, pinnedMids.size() - 1, 2));
        List<String> expected = asList(pinnedMids.get(0), mids.get(0));
        List<String> notExpected = pinnedMids.subList(1, pinnedMids.size());
        notExpected.addAll(mids.subList(1, mids.size()));
        assertOnlyExpected(mixed, expected, notExpected);
    }

    private List<Envelope> getMessagesInTabWithPins(Tab tab, int first, int count) {
        return apiHoundV2().messagesInTabWithPins()
                .withUid(uid())
                .withTab(tab.getName())
                .withFirst(String.valueOf(first))
                .withCount(String.valueOf(count))
                .get(shouldBe(ok200()))
                .body().as(Messages.class).getEnvelopes();
    }

    private List<String> getMids(List<Envelope> envelopes) {
        return envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
    }

    private void assertOnlyExpected(List<String> msgs, List<String> expected, List<String> notExpected) {
        assertThat("Письма должны быть в ответе",
                msgs, containsAll(expected));
        assertThat("Письма не должны быть в ответе",
                msgs, not(containsAny(notExpected)));
    }

    private static Matcher<List<String>> containsAll(List<String> messages) {
        return allOf(messages.stream()
                .map(Matchers::hasItem)
                .toArray(Matcher[]::new));
    }

    private static Matcher<List<String>> containsAny(List<String> messages) {
        return anyOf(messages.stream()
                .map(Matchers::hasItem)
                .toArray(Matcher[]::new));
    }
}
