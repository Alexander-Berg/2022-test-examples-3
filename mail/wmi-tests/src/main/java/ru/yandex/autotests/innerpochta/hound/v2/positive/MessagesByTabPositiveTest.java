package ru.yandex.autotests.innerpochta.hound.v2.positive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.messagesbytab.ApiMessagesByTab;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.beans.hound.Messages;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.apply;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.timestampGenerator;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeEnvelopeForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;

@Aqua.Test
@Title("[HOUND] Ручка v2/messages_by_tab")
@Description("Тесты на ручку v2/message_by_tab")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2MessagesByTabTest")
@RunWith(DataProviderRunner.class)
public class MessagesByTabPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Должны вернуть письма только из данного таба")
    @UseDataProvider("existingTabs")
    public void shouldGetMessagesOnlyForRequestedTab(Tab tab) throws Exception {
        Map<String, String> mids = new HashMap<>();
        mids.put(Tab.RELEVANT.getName(), makeMessageForTab(authClient, Tab.RELEVANT));
        mids.put(Tab.NEWS.getName(), makeMessageForTab(authClient, Tab.NEWS));
        mids.put(Tab.SOCIAL.getName(), makeMessageForTab(authClient, Tab.SOCIAL));

        List<Envelope> msgs = getMessagesByTab(tab);
        assertOnlyMessagesForTab(msgs, mids, tab);
    }

    @DataProvider
    public static List<List<Object>> date() {
        return timestampGenerator();
    }

    @Test
    @Description("Должны вернуть письма с фильтрацией по дате получения")
    @UseDataProvider("date")
    public void shouldGetMessagesFilteredByDate(BiFunction<Envelope, Envelope, Long> since_,
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

        List<String> msgs = getMids(getMessagesByTab(tab, till, since));
        assertOnlyExpected(msgs, expected, notExpected);
    }

    private List<Envelope> getMessagesByTab(Tab tab) {
        return envelopesFromResp(commonRequest(tab));
    }

    private List<Envelope> getMessagesByTab(Tab tab, String till, String since) {
        return envelopesFromResp(commonRequest(tab)
                .withTill(till)
                .withSince(since));
    }

    private ApiMessagesByTab commonRequest(Tab tab) {
        return apiHoundV2().messagesByTab()
                .withUid(uid())
                .withTab(tab.getName())
                .withFirst("0")
                .withCount("10");
    }

    private List<Envelope> envelopesFromResp(ApiMessagesByTab api) {
        return api.get(shouldBe(ok200()))
                .body().as(Messages.class).getEnvelopes();
    }

    private List<String> getMids(List<Envelope> envelopes) {
        return envelopes.stream().map(Envelope::getMid).collect(Collectors.toList());
    }

    private void assertOnlyMessagesForTab(List<Envelope> msgs, Map<String, String> mids, Tab tab) {
        for (Map.Entry<String, String> mid: mids.entrySet()) {
            if (mid.getKey().equals(tab.getName())) {
                assertTrue("Не нашли письма из таба " + mid.getKey(), msgs.stream()
                        .anyMatch((envelope) -> envelope.getMid().equals(mid.getValue())));
            } else {
                assertTrue("Нашли письма из таба " + mid.getKey(), msgs.stream()
                        .noneMatch((envelope) -> envelope.getMid().equals(mid.getValue())));
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
