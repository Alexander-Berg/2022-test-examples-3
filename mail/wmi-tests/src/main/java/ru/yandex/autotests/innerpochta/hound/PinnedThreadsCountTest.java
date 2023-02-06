package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.Label;
import ru.yandex.autotests.innerpochta.beans.yplatform.ThreadLabel;
import ru.yandex.autotests.innerpochta.data.FirstAndCountData;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.labels.PinnedCommonTest;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSyncOrAsync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.04.15
 * Time: 20:06
 */
@Aqua.Test
@Title("[PINS] Пины. Проверка количества тредов в ручке ywmi")
@Description("Проверяем выдачу ручки threads_in_folder_with_pins")
@Credentials(loginGroup = "PinnedThreadsTest")
@Features(MyFeatures.HOUND)
@Stories({MyStories.PINS, MyStories.THREADS})
@Issues({@Issue("DARIA-47151"), @Issue("DARIA-47992")})
@RunWith(Parameterized.class)
public class PinnedThreadsCountTest extends BaseHoundTest {

    public static final int COUNT_OF_PINNED_THREADS = 5;
    public static final int COUNT_OF_NOT_PINNED_THREADS = 5;

    @Parameterized.Parameter(0)
    public Integer first;

    @Parameterized.Parameter(1)
    public Integer count;

    @Parameterized.Parameters(name = "first = {0} count = {1}")
    public static Collection<Object[]> data() {
       return FirstAndCountData.get(COUNT_OF_PINNED_THREADS, COUNT_OF_NOT_PINNED_THREADS);
    }

    @ClassRule
    public static CleanMessagesMopsRule cleanMessagesRule =
            with(authClient).before(true).after(false).allfolders();

    public static final int COUNT_OF_LETTERS = 2;

    public static MessagesByFolderObj pinsYwmiObj;

    private static String labelId;

    @BeforeClass
    public static void getInit() throws Exception {
        pinsYwmiObj = new MessagesByFolderObj().setUid(uid()).setFid(folderList.defaultFID())
                .setSortType("date1");

        labelId = Hound.getLidBySymbolTitle(authClient, PINNED);

        assertThat("У пользователя нет метки <pinned>", labelId, not(equalTo("")));
        initThreads();
    }

    public static void initThreads() throws Exception {
        List<String> midsWithPins = newArrayList();
        for (int i = 0; i < COUNT_OF_PINNED_THREADS; i++) {
            List<String> mids = sendWith(authClient).viaProd().subj(PinnedCommonTest.PIN_SUBJ + i + Util.getRandomString())
                    .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();
            midsWithPins.addAll(mids);
        }

        List<String> midsWithoutPins = newArrayList();
        for (int i = 0; i < COUNT_OF_NOT_PINNED_THREADS; i++) {
            List<String> mids = sendWith(authClient).viaProd().subj(PinnedCommonTest.NOTPIN_SUBJ + i + Util.getRandomString())
                    .count(COUNT_OF_LETTERS).send().waitDeliver().getMids();
            midsWithoutPins.addAll(mids);
        }

        // Помечаем созданной меткой все вышенайденные миды
        Mops.label(authClient, new MidsSource(midsWithPins), asList(labelId))
                .post(shouldBe(okSyncOrAsync()));
        assertThat(authClient, withWaitFor(hasMsgsWithLid(midsWithPins, labelId)));

        assertThat(authClient, hasMsgsWithLid(midsWithPins, labelId));
    }


    @Test
    @Stories({MyStories.PINS})
    @Title("Должны увидеть определенныое количество пиновых тредов")
    @Description("Сравниваем с заранее заготовленным результатом")
    public void compareWithProdPinnedLettersWithCount() throws Exception {
        pinsYwmiObj.setFirst(first.toString()).setCount(count.toString());
        ru.yandex.autotests.innerpochta.beans.yplatform.ThreadsByFolder threadsByFolderWithPins =
                api(ThreadsInFolderWithPins.class).params(pinsYwmiObj.setFid(folderList.defaultFID()))
                        .setHost(props().houndUri()).get().via(authClient).withDebugPrint().resp();
        List<Envelope> envelopes = threadsByFolderWithPins.getThreadsByFolder().getEnvelopes();

        List<ThreadLabel> threadLabels = threadsByFolderWithPins.getThreadsByFolder().getThreadLabels();
        int countPinnedThreads = 0;
        for (ThreadLabel threadLabel : threadLabels) {
            for (Label label : threadLabel.getLabels()) {
                if (labelId.equals(label.getLid())) {
                    countPinnedThreads++;
                }
            }
        }
        int countNotPinnedThreads = threadLabels.size() - countPinnedThreads;

        MatcherAssert.assertThat(String.format("Количество тредов в диапазоне " +
                        "fist=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                envelopes.size(), equalTo(getExpected(first, count)));

        MatcherAssert.assertThat(String.format("Количество запиненных тредов в диапазоне " +
                        "fist=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                countPinnedThreads, equalTo(getExpectedPinned(first, count)));

        MatcherAssert.assertThat(String.format("Количество незапиненных тредов в диапазоне " +
                        "fist=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                countNotPinnedThreads, equalTo(getExpected(first, count) - getExpectedPinned(first, count)));
    }

    static int getExpected(int first, int count) {
        int expected = 0;
        if (first + count >= COUNT_OF_PINNED_THREADS + COUNT_OF_NOT_PINNED_THREADS) {
            expected = COUNT_OF_PINNED_THREADS + COUNT_OF_NOT_PINNED_THREADS - first;
        }
        if ((first + count < COUNT_OF_PINNED_THREADS + COUNT_OF_NOT_PINNED_THREADS) && (first + count > 0)) {
            expected = first + count;
        }
        return expected;
    }

    static int getExpectedPinned(int first, int count) {
        int expected = 0;
        if ((COUNT_OF_PINNED_THREADS > first) && (first + count) >= COUNT_OF_PINNED_THREADS) {
            expected = COUNT_OF_PINNED_THREADS - first;
        }

        if ((COUNT_OF_PINNED_THREADS > first) && (first + count < COUNT_OF_PINNED_THREADS)) {
            expected = count;
        }

        return expected;
    }
}
