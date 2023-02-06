package ru.yandex.autotests.innerpochta.hound.v2.positive;

import com.tngtech.java.junit.dataprovider.DataProvider;
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
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.threadsbytab.ApiThreadsByTab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.apply;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.timestampGenerator;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeEnvelopeForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeThreadForMultiTabs;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeThreadForTab;

@Aqua.Test
@Title("[HOUND] Ручка v2/threads_by_tab")
@Description("Тесты на ручку v2/threads_by_tab")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2ThreadsByTabTest")
@RunWith(DataProviderRunner.class)
public class ThreadsByTabPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Должны вернуть тред только из данного таба")
    @UseDataProvider("existingTabs")
    public void shouldGetThreadsOnlyForRequestedTab(Tab tab) throws Exception {
        Map<String, String> tids = new HashMap<>();
        tids.put(Tab.RELEVANT.getName(), makeThreadForTab(authClient, Tab.RELEVANT).getKey());
        tids.put(Tab.NEWS.getName(), makeThreadForTab(authClient, Tab.NEWS).getKey());
        tids.put(Tab.SOCIAL.getName(), makeThreadForTab(authClient, Tab.SOCIAL).getKey());

        List<Envelope> envelopes = getThreadsByTab(tab).getEnvelopes();
        assertOnlyThreadsForTab(envelopes, tids, tab);
    }

    @DataProvider
    public static Object[][] MultiTabThreads() {
        return new Object[][] {
                { asList(Tab.RELEVANT, Tab.NEWS, Tab.SOCIAL), asList(new Tab[] {}) },
                { asList(Tab.RELEVANT, Tab.NEWS), asList(Tab.SOCIAL) },
                { asList(Tab.RELEVANT, Tab.SOCIAL), asList(Tab.NEWS) },
                { asList(Tab.NEWS, Tab.SOCIAL), asList(Tab.RELEVANT) }
        };
    }

    @Test
    @Title("Тред показывается в каждом табе, в котором есть письма из треда")
    @UseDataProvider("MultiTabThreads")
    public void shouldSeeThreadInEveryTab(List<Tab> expectedTabs, List<Tab> unexpectedTabs) throws Exception {
        String tid = makeThreadForMultiTabs(authClient, expectedTabs).getKey();

        for (Tab tab: expectedTabs) {
            assertTrue("Тред должен быть в табе " + tab.getName(),
                    getThreadsByTab(tab).getEnvelopes()
                            .stream().anyMatch((envelope) -> envelope.getThreadId().equals(tid)));
        }

        for (Tab tab: unexpectedTabs) {
            assertTrue("Треда не должно быть в табе " + tab.getName(),
                    getThreadsByTab(tab).getEnvelopes().isEmpty());
        }
    }

    @Test
    @Title("Для треда выбирается последнее письмо в табе")
    public void shouldSeeLatestMidInTabAsThreadHeader() throws Exception {
        List<Tab> allTabs = asList(
                Tab.RELEVANT,
                Tab.NEWS,
                Tab.SOCIAL,
                Tab.EMPTY
        );
        makeThreadForMultiTabs(authClient, allTabs).getValue();

        for (val t: existingTabs()) {
            Tab tab = (Tab) t[0];
            List<Envelope> envelopes = getThreadsByTab(tab).getEnvelopes();
            assertThat("Ожидалось одно письмо в табе " + tab.getName(),
                    envelopes.size(), equalTo(1));
            assertThat("Должны показывать письмо из таба " + tab.getName(),
                    envelopes.get(0).getTab(), equalTo(tab.getName()));
        }
    }

    @DataProvider
    public static List<List<Object>> date() {
        return timestampGenerator();
    }

    @Test
    @Description("Должны вернуть треды с фильтрацией по дате получения")
    @UseDataProvider("date")
    public void shouldGetThreadsInTabFilteredByDate(
            BiFunction<Envelope, Envelope, Long> since_,
            BiFunction<Envelope, Envelope, Long> till_,
            BiFunction<Envelope, Envelope, List<Envelope>> expected_) throws Exception {
        Tab tab = Tab.RELEVANT;
        Envelope first = makeEnvelopeForTab(authClient, tab);
        Thread.sleep(1000); // guarantee of different received_date
        Envelope second = makeEnvelopeForTab(authClient, tab);

        String since = apply(since_, first, second);
        String till = apply(till_, first, second);

        List<String> expected = getMids(expected_.apply(first, second));
        List<String> notExpected = getMids(asList(first, second));
        notExpected.removeAll(expected);

        List<String> msgs = getMids(getThreadsByTab(tab, till, since).getEnvelopes());
        assertOnlyExpected(msgs, expected, notExpected);
    }

    private ThreadsByFolder getThreadsByTab(Tab tab) {
        return fromResp(commonRequest(tab)).getThreadsByFolder();
    }

    private ThreadsByFolder getThreadsByTab(Tab tab, String till, String since) {
        return fromResp(commonRequest(tab)
                .withTill(till)
                .withSince(since))
                .getThreadsByFolder();
    }

    private ApiThreadsByTab commonRequest(Tab tab) {
        return apiHoundV2().threadsByTab()
                .withUid(uid())
                .withTab(tab.getName())
                .withFirst("0")
                .withCount("10");
    }

    private Threads fromResp(ApiThreadsByTab api) {
        return api.get(shouldBe(ok200()))
        .body().as(Threads.class);
    }

    private List<String> getMids(List<Envelope> envelopes) {
        return envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
    }

    private void assertOnlyThreadsForTab(List<Envelope> envelopes, Map<String, String> tids, Tab tab) {
        for (Map.Entry<String, String> tid: tids.entrySet()) {
            if (tid.getKey().equals(tab.getName())) {
                assertTrue("Не нашли треды из таба " + tid.getKey(), envelopes
                        .stream().anyMatch((envelope) -> envelope.getThreadId().equals(tid.getValue())));
            } else {
                assertTrue("Нашли треды из таба " + tid.getKey(), envelopes
                        .stream().noneMatch((envelope) -> envelope.getThreadId().equals(tid.getValue())));
            }
        }
    }

    private void assertOnlyExpected(List<String> msgs, List<String> expected, List<String> notExpected) {
        assertThat("Письма должны были попасть в выборку по датам",
                msgs, containsAll(expected));
        assertThat("Письма не должны были попасть в выборку по датам",
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
