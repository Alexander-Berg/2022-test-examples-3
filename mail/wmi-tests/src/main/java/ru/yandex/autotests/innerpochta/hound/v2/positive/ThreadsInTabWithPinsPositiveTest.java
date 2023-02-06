package ru.yandex.autotests.innerpochta.hound.v2.positive;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import lombok.val;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.hound.Threads;
import ru.yandex.autotests.innerpochta.beans.hound.ThreadsByFolder;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.TidsSource;
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
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeThreadForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;

@Aqua.Test
@Title("[HOUND] Ручка v2/threads_in_tab_with_pins")
@Description("Тесты на ошибки ручки v2/threads_in_tab_with_pins")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2ThreadsInTabWithPinsTest")
@RunWith(DataProviderRunner.class)
public class ThreadsInTabWithPinsPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Должны вернуть запиненные треды из всех табов и обычные треды только из данного таба")
    @UseDataProvider("existingTabs")
    public void shouldGetThreadsForRequestedTabWithPinned(Tab tab) throws Exception {
        List<String> expected = makePinnedThreads();
        Tab notExpectedTab = tab != Tab.RELEVANT ? Tab.RELEVANT : Tab.NEWS;
        List<String> notExpected = asList(makeThreadForTab(authClient, notExpectedTab).getKey());
        expected.add(makeThreadForTab(authClient, tab).getKey());

        assertOnlyExpected(getTids(getThreadsInTabWithPins(tab, 0, 100)),
                expected, notExpected);
    }

    @Test
    @Title("Должны вернуть запиненные треды раньше обычных")
    @UseDataProvider("existingTabs")
    public void shouldGetPinnedThreadsFirstAndThenFromTab(Tab tab) throws Exception {
        List<String> pinnedTids = makePinnedThreads();
        List<String> tids = asList(makeThreadForTab(authClient, tab).getKey());

        // Get only pinned threads
        assertOnlyExpected(getTids(getThreadsInTabWithPins(tab, 0, pinnedTids.size())),
                pinnedTids, tids);

        // Get only not pinned messages
        assertOnlyExpected(getTids(getThreadsInTabWithPins(tab, pinnedTids.size(), 10)),
                tids, pinnedTids);

        // Get mixed messages
        List<String> mixed = getTids(getThreadsInTabWithPins(tab, pinnedTids.size() - 1, 2));
        List<String> expected = asList(pinnedTids.get(0), tids.get(0));
        List<String> notExpected = pinnedTids.subList(1, pinnedTids.size());
        assertOnlyExpected(mixed, expected, notExpected);
    }

    @Test
    @Title("Должны показать тред в запиненных, если в нем есть хотя бы одно запиненное письмо")
    @UseDataProvider("existingTabs")
    public void shouldSeeThreadAsPinnedIfAtLeastOneMessagePinned(Tab tab) throws Exception {
        String pinnedTid = makeThreadWithPinned(Tab.EMPTY);

        assertOnlyExpected(getTids(getThreadsInTabWithPins(tab, 0, 100)),
                asList(pinnedTid), new ArrayList<>());
    }

    @Test
    @Title("Не показываем тред в незапиненных, если уже показали в запиненных")
    @UseDataProvider("existingTabs")
    public void shouldNotShowPinnedThreadTwice(Tab tab) throws Exception {
        String pinnedTid = makeThreadWithPinned(tab);
        String tid = makeThreadForTab(authClient, tab).getKey();

        assertOnlyExpected(getTids(getThreadsInTabWithPins(tab, 1, 100)),
                asList(tid), asList(pinnedTid));
    }

    private List<String> makePinnedThreads() throws Exception {
        String pinLid = Hound.getLidBySymbolTitle(authClient, PINNED);
        assertThat("У пользователя нет метки <pinned>", pinLid, IsNot.not(equalTo("")));
        List<String> pinnedTids = new ArrayList<>();
        pinnedTids.add(makeThreadForTab(authClient, Tab.EMPTY).getKey());
        pinnedTids.add(makeThreadForTab(authClient, Tab.RELEVANT).getKey());
        pinnedTids.add(makeThreadForTab(authClient, Tab.NEWS).getKey());
        pinnedTids.add(makeThreadForTab(authClient, Tab.SOCIAL).getKey());

        for (String tid: pinnedTids) {
            Mops.label(authClient, new TidsSource(tid), asList(pinLid))
                    .post(shouldBe(okSync()));
        }
        return pinnedTids;
    }

    private String makeThreadWithPinned(Tab tab) throws Exception {
        String pinLid = Hound.getLidBySymbolTitle(authClient, PINNED);
        assertThat("У пользователя нет метки <pinned>", pinLid, IsNot.not(equalTo("")));

        val midTids = makeThreadForTab(authClient, tab);
        Mops.label(authClient, new MidsSource(midTids.getValue().get(1)), asList(pinLid))
                .post(shouldBe(okSync()));

        return midTids.getKey();
    }

    private ThreadsByFolder getThreadsInTabWithPins(Tab tab, int first, int count) {
        return apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withTab(tab.getName())
                .withFirst(String.valueOf(first))
                .withCount(String.valueOf(count))
                .get(shouldBe(ok200()))
                .body().as(Threads.class)
                .getThreadsByFolder();
    }

    private List<String> getTids(ThreadsByFolder threads) {
        return threads.getEnvelopes().stream().map(Envelope::getThreadId).collect(Collectors.toList());
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
