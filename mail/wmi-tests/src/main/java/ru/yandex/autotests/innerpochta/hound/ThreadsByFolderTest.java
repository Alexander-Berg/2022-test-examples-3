package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.Label;
import ru.yandex.autotests.innerpochta.beans.yplatform.ThreadLabel;
import ru.yandex.autotests.innerpochta.beans.yplatform.ThreadsByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsByFolderCommand;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.beans.yplatform.LabelMatchers.withCnt;
import static ru.yandex.autotests.innerpochta.beans.yplatform.LabelMatchers.withLid;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ThreadLabelMatchers.withTid;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ThreadsByFolderMatchers.withThreadsByFolder;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ThreadsByFolder_Matchers.withEnvelopes;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsByFolderObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PRIORITY_HIGH;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsByFolderCommand.threadsByFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.06.14
 * Time: 18:05
 */
@Aqua.Test
@Title("[HOUND] Ручка для /threads_by_folder")
@Description("Проверяем количество тредов")
@Credentials(loginGroup = "ThreadsByFolder")
@Features(MyFeatures.HOUND)
@Stories(MyStories.THREAD_LIST)
public class ThreadsByFolderTest extends BaseHoundTest {

    public static final int COUNT_OF_LETTERS = 4;
    private static String FIRST = "0";
    private static String COUNT = "5";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public RuleChain clearLabels = new LogConfigRule()
            .around(new DeleteLabelsMopsRule(authClient));

    @Test
    @Issue("DARIA-41073")
    @Title("lcn-ревизии")
    @Description("Проверяем наличие \"lcn-ревизии\" для треда\n" +
            "[DARIA-41073]")
    public void shouldSeeLcn() throws Exception {
        String subject = Util.getRandomString();
        List<String> mids = sendWith(authClient).count(COUNT_OF_LETTERS).viaProd().subj(subject).send()
                .waitDeliver().getMids();
        List<Envelope> envelopes = Hound.getLastEnvelopesInFolderBy(authClient, folderList.defaultFID(), COUNT_OF_LETTERS);

        assertThat("В инбоксе были лишние письма",
                envelopes.stream().map(Envelope::getMid).collect(Collectors.toList()).containsAll(mids));

        String threadId = envelopes.get(0).getThreadId();

        //ставим две метки:
        String lid = Mops.newLabelByName(authClient, Util.getRandomString());
        String lid2 = Mops.newLabelByName(authClient, Util.getRandomString());

        Mops.label(authClient, new MidsSource(mids.get(0)), Collections.singletonList(lid))
                .post(shouldBe(okSync()));
        Mops.label(authClient, new MidsSource(mids), Collections.singletonList(lid2))
                .post(shouldBe(okSync()));

        List<ThreadLabel> treadLabels = api(ThreadsByFolderCommand.class)
                .setHost(props().houndUri())
                .params(empty()
                        .setUid(uid())
                        .setFirst(FIRST).setCount(COUNT).setFid(folderList.defaultFID())).get().via(authClient).resp()
                .getThreadsByFolder().getThreadLabels();

        ThreadLabel threadLabel = selectFirst(treadLabels, withTid(equalTo(threadId)));

        List<Label> labels = threadLabel.getLabels();

        Label label1 = selectFirst(labels, withLid(equalTo(lid)));
        assertThat("Неверное количество писем в треде помеченных первой меткой " + lid, label1, withCnt(equalTo("1")));

        Label label2 = selectFirst(labels, withLid(equalTo(lid2)));
        assertThat("Неверное количество писем в треде помеченных второй меткой " + lid2, label2,
                withCnt(equalTo(String.valueOf(COUNT_OF_LETTERS))));
    }

    @Test
    @Issue("DARIA-41073")
    @Title("lcn-ревизии")
    @Description("Проверяем наличие \"lcn-ревизии\" для треда\n" +
            "[DARIA-41073]")
    public void shouldSeeLcnSystemLabels() throws Exception {
        List<String> mids = sendWith(authClient).count(COUNT_OF_LETTERS).viaProd().send().waitDeliver().getMids();
        List<Envelope> envelopes = Hound.getLastEnvelopesInFolderBy(authClient, folderList.defaultFID(), COUNT_OF_LETTERS);

        assertThat("В инбоксе были лишние письма",
                envelopes.stream().map(Envelope::getMid).collect(Collectors.toList()).containsAll(mids));

        String threadId = envelopes.get(0).getThreadId();

        String lid = Hound.getLidBySymbolTitle(authClient, PRIORITY_HIGH);

        assertThat("У пользователя нет метки " + PRIORITY_HIGH, lid, not(equalTo("")));

        Mops.label(authClient, new MidsSource(mids.get(0)), Collections.singletonList(lid))
                .post(shouldBe(okSync()));

        List<ThreadLabel> treadLabels = threadsByFolder(empty()
                .setUid(uid())
                .setFirst(FIRST).setCount(COUNT).setFid(folderList.defaultFID()))
                .get().via(authClient).withDebugPrint()
                .resp()
                .getThreadsByFolder().getThreadLabels();

        ThreadLabel threadLabel = selectFirst(treadLabels, withTid(equalTo(threadId)));

        List<Label> labels = threadLabel.getLabels();

        Label label1 = selectFirst(labels, withLid(equalTo(lid)));
        assertThat("Неверное количество писем в треде помеченных первой меткой " + lid, label1, withCnt(equalTo("1")));
    }

    @Test
    @Issue("MAILPG-2007")
    @Title("Проверяем tab")
    @Description("В выдаче энвелопов всех ручек должно быть поле tab")
    public void testTab() throws Exception {
        String mid = sendWith(authClient).viaProd().send().strict().waitDeliver().getMid();
        String tab = threadsByFolder(empty()
                .setUid(uid())
                .setFirst("0").setCount("1").setFid(folderList.defaultFID()))
                .get().via(authClient).resp().getThreadsByFolder().getEnvelopes().get(0).getTab();
        assertThat("Поле tab должно быть default", tab, Matchers.equalTo(Tab.defaultTab));
    }
}
