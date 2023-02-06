package ru.yandex.autotests.innerpochta.hound.v2.positive;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.hound.V2FirstEnvelopeDateInTabResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/first_envelope_date_in_tab")
@Description("Тесты на ручку v2/first_envelope_date_in_tab")
@Features(MyFeatures.HOUND)
@RunWith(Parameterized.class)
@Credentials(loginGroup = "GetFirstEnvelopeDateInTabTest")
public class FirstEnvelopeDateInTabPositiveTest extends BaseHoundTest {

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    public String tabType;

    @Parameterized.Parameters(name = "tabType = {0}")
    public static Collection<Object[]> data() {
        return newArrayList(
                new Object[]{Tabs.Tab.RELEVANT.getName()},
                new Object[]{Tabs.Tab.NEWS.getName()},
                new Object[]{Tabs.Tab.SOCIAL.getName()}
        );
    }

    public FirstEnvelopeDateInTabPositiveTest(String tabType) {
        this.tabType = tabType;
    }

    @Test
    @Title("first_envelope_date_in_tab с корректным именем таба")
    public void firstEnvelopeDateInTabWithCorrectTab() throws Exception {
        List<Envelope> envelopes = sendWith(authClient).viaProd().count(3).send().waitDeliver().
                getEnvelopes();
        List<String> mids = envelopes.stream()
                .map(Envelope::getMid)
                .collect(Collectors.toList());
        long utcTimestamp = envelopes.stream()
                .map(Envelope::getReceiveDate)
                .min(Comparator.naturalOrder())
                .get();
        Mops.complexMove(authClient, folderList.inboxFID(), tabType, new MidsSource(mids))
                .post(shouldBe(okSync()));
        Long response = apiHoundV2().firstEnvelopeDateInTab().withUid(uid())
                .withTab(tabType).get(shouldBe(ok200())).as(V2FirstEnvelopeDateInTabResponse.class).getFirstEnvelopeDateInTab();

        assertThat("Ожидали другое значение поля <first_envelope_date_in_tab>",
                response, equalTo(utcTimestamp));
    }
}