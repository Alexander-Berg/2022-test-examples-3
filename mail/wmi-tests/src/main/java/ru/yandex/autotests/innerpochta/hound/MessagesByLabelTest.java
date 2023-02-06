package ru.yandex.autotests.innerpochta.hound;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByLabelObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.ATTACHED;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.SPAM;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.SYNCED;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] ручка messages_by_label")
@Description("Тесты на ручку messages_by_label")
@Features(MyFeatures.HOUND)
@Stories(MyStories.MESSAGES_LIST)
@Credentials(loginGroup = "MessagesByLabelTest")
@Issue("MAILPG-432")
@RunWith(DataProviderRunner.class)
public class MessagesByLabelTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @DataProvider
    public static Object[][] fakeLabelsProvider() {
        return new Object[][] {
                {SPAM}, {SYNCED}, {ATTACHED}
        };
    }

    @Test
    @UseDataProvider("fakeLabelsProvider")
    @Title("messages_by_label with fake labels in empty account")
    public void testMessagesByLabelWithFakeLabelInEmptyAccount(Hound.LabelSymbol symbolTitle) {
        String lid = Hound.getLidBySymbolTitle(authClient, symbolTitle);
        assertThat("Не нашли метку с символом " + symbolTitle, lid, not(equalTo("")));

        MessagesByLabel msgs = getMessagesByLabel(lid);
        assertEquals("Нашли что-то лишнее по метке " + lid, msgs.resp().getEnvelopes().size(), 0);
    }

    @Test
    @Title("messages_by_label with attached fake labels")
    public void testMessagesByLabelWithAttachedFakeLabel() throws Exception {
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();

        String mid = sendWith(authClient).viaProd()
                .addAttaches(attach).send().waitDeliver().getMid();

        String lid = Hound.getLidBySymbolTitle(authClient, ATTACHED);
        assertThat("Не нашли метку с символом " + ATTACHED, lid, not(equalTo("")));

        MessagesByLabel msgsByLabel = getMessagesByLabel(lid);

        assertTrue("Не нашли письма по метке attached", msgsByLabel.resp().getEnvelopes().stream()
                .anyMatch((envelope) -> envelope.getMid().equals(mid) && envelope.getLabels().contains(lid)));
    }

    @Test
    @Title("messages_by_label with user's label")
    public void testMessagesByLabelWithUsersLabel() throws Exception {
        String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String lid = Mops.newLabelByName(authClient, Util.getRandomString());

        Mops.label(authClient, new MidsSource(mid), Collections.singletonList(lid))
                .post(shouldBe(okSync()));

        MessagesByLabel msgsByLabel = getMessagesByLabel(lid);

        assertTrue("Не нашли письма по пользовательской метке", msgsByLabel.resp().getEnvelopes().stream()
                .anyMatch((envelope) -> envelope.getMid().equals(mid) && envelope.getLabels().contains(lid)));
    }

    private MessagesByLabel getMessagesByLabel(String lid) {
        return api(MessagesByLabel.class)
                .setHost(props().houndUri())
                .params(empty()
                        .setLid(lid)
                        .setFirst("0")
                        .setCount("10")
                        .setUid(uid())
                )
                .get()
                .via(authClient).withDebugPrint();
    }
}
