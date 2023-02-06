package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.data.FirstAndCountData;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByThreadWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.FidSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSyncOrAsync;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[PINS] Пины. Проверка количества писем в треде в ручке hound")
@Description("Проверяем выдачу ручки messages_by_thread_with_pins")
@Credentials(loginGroup = PinnedLettersInThreadCountTest.LOGIN_GROUP)
@Features(MyFeatures.HOUND)
@Stories({MyStories.PINS, MyStories.THREADS, MyStories.LETTERS})
@Issues({@Issue("DARIA-47151"), @Issue("DARIA-47992")})
@RunWith(Parameterized.class)
public class PinnedLettersInThreadCountTest extends BaseHoundTest {
    @ClassRule
    public static CleanMessagesMopsRule cleanMessagesRule = with(authClient).before(true).after(true).allfolders();

    public static final String LOGIN_GROUP = "PinnedLettersInThreadTest";
    private static final int COUNT_OF_PINNED_LETTERS = 5;
    private static final int COUNT_OF_NOT_PINNED_LETTERS = 5;

    @Parameterized.Parameter(0)
    public Integer first;

    @Parameterized.Parameter(1)
    public Integer count;

    @Parameterized.Parameters(name = "first = {0} count = {1}")
    public static Collection<Object[]> data() {
        return FirstAndCountData.get(COUNT_OF_NOT_PINNED_LETTERS, COUNT_OF_NOT_PINNED_LETTERS);
    }

    private static MessagesByFolderObj pinsYwmiObj;
    private static String labelId;

    @BeforeClass
    public static void getInit() throws Exception {
        labelId = Hound.getLidBySymbolTitle(authClient, PINNED);

        assertThat("У пользователя нет метки <pinned>", labelId, not(equalTo("")));
        String subj = Util.getRandomString();
        //отсылаем письма и помечаем часть _pinned_
        List<Envelope> envelopes = sendWith(authClient).viaProd().subj(subj).to(authClient.acc().getSelfEmail())
                .count(COUNT_OF_PINNED_LETTERS + COUNT_OF_NOT_PINNED_LETTERS).send().waitDeliver().getEnvelopes();

        List<String> midsWithPins = envelopes.subList(0, COUNT_OF_PINNED_LETTERS).stream()
                .map(Envelope::getMid)
                .collect(Collectors.toList());
        String tid = envelopes.get(0).getThreadId();

        pinsYwmiObj = new MessagesByFolderObj().setUid(uid()).setFid(folderList.defaultFID())
                .setSortType("date1").setTid(tid);

        //убираем письма из outbox-а
        Mops.purge(authClient, new FidSource(folderList.sentFID()))
                .post(shouldBe(okSyncOrAsync()));
        // Дожидаемся очистки папки
        assertThat("Не смогли очистить папку Отправленные", authClient,
                withWaitFor(not(hasMsgsIn(folderList.sentFID())), MINUTES.toMillis(1)));
        // Помечаем созданной меткой все вышенайденные миды
        Mops.label(authClient, new MidsSource(midsWithPins), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgsWithLid(midsWithPins, labelId));
    }

    @Test
    @Stories({MyStories.PINS})
    @Title("Должны увидеть определенныое количество пиновых писем")
    @Description("Сравниваем с заранее заготовленным результатом")
    public void compareWithProdPinnedLettersWithCount() throws Exception {
        pinsYwmiObj.setFirst(first.toString()).setCount(count.toString());

        List<Envelope> envelopes = api(MessagesByThreadWithPins.class).params(pinsYwmiObj
                .setFid(folderList.defaultFID()))
                .setHost(props().houndUri()).get().via(authClient).resp().getEnvelopes();

        int countPinnedLetters = 0;
        for (Envelope envelope : envelopes) {
            for (String label : envelope.getLabels()) {
                if (labelId.equals(label)) {
                    countPinnedLetters++;
                }
            }
        }
        int countNotPinnedLetters = envelopes.size() - countPinnedLetters;

        MatcherAssert.assertThat(String.format("Количество писем в треде в диапазоне " +
                        "first=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                envelopes.size(), equalTo(PinnedThreadsCountTest.getExpected(first, count)));

        MatcherAssert.assertThat(String.format("Количество запиненных писем в треде в диапазоне " +
                        "first=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                countPinnedLetters, equalTo(PinnedThreadsCountTest.getExpectedPinned(first, count)));

        MatcherAssert.assertThat(String.format("Количество незапиненных писем в треде в диапазоне " +
                        "first=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                countNotPinnedLetters, equalTo(PinnedThreadsCountTest.getExpected(first, count) -
                        PinnedThreadsCountTest.getExpectedPinned(first, count)));
    }
}
