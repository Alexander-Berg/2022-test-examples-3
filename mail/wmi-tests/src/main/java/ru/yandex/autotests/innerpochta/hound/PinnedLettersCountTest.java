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
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.labels.PinnedCommonTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.06.15
 * Time: 16:50
 */
@Aqua.Test
@Title("[PINS] Пины. Проверка количества писем в ручке ywmi")
@Description("Проверяем выдачу ручки messages_in_folder_with_pins")
@Features(MyFeatures.HOUND)
@Stories({MyStories.PINS, MyStories.LETTERS})
@Issues({@Issue("DARIA-47151"), @Issue("DARIA-47992")})
@Credentials(loginGroup = "PinnedLettersTest")
@RunWith(Parameterized.class)
public class PinnedLettersCountTest extends BaseHoundTest {
    private static final int COUNT_OF_PINNED_LETTERS = 5;
    private static final int COUNT_OF_NOT_PINNED_LETTERS = 5;

    @Parameterized.Parameter(0)
    public Integer first;

    @Parameterized.Parameter(1)
    public Integer count;

    @Parameterized.Parameters(name = "first = {0} count = {1}")
    public static Collection<Object[]> data() {
        return FirstAndCountData.get(COUNT_OF_PINNED_LETTERS, COUNT_OF_NOT_PINNED_LETTERS);
    }

    @ClassRule
    public static CleanMessagesMopsRule cleanMessagesRule = with(authClient)
            .before(true).after(false).allfolders();

    private static MessagesByFolderObj pinsYwmiObj;
    private static String labelId;

    @BeforeClass
    public static void getInit() throws Exception {
        pinsYwmiObj = new MessagesByFolderObj().setUid(uid()).setFid(folderList.defaultFID())
                .setSortType("date1");

        labelId = Hound.getLidBySymbolTitle(authClient, PINNED);

        assertThat("У пользователя нет метки <pinned>", labelId, not(equalTo("")));
        initLetters();
    }

    private static void initLetters() throws Exception {
        //отсылаем письма и помечаем часть _pinned_
        List<String> midsWithPins = sendWith(authClient).viaProd().subj(PinnedCommonTest.PIN_SUBJ)
                .count(COUNT_OF_PINNED_LETTERS).to(authClient.acc().getSelfEmail()).send()
                .waitDeliver().getMids();

        sendWith(authClient).viaProd().subj(PinnedCommonTest.NOTPIN_SUBJ)
                .count(COUNT_OF_NOT_PINNED_LETTERS).to(authClient.acc().getSelfEmail()).send()
                .waitDeliver();

        // Помечаем созданной меткой все вышенайденные миды
        Mops.label(authClient, new MidsSource(midsWithPins), asList(labelId))
                .post(shouldBe(okSync()));

        assertThat(authClient, hasMsgsWithLid(midsWithPins, labelId));
    }

    @Test
    @Stories({MyStories.PINS})
    @Title("Должны увидеть определенныое количество пиновых писем")
    @Description("Сравниваем с заранее заготовленным результатом")
    public void compareWithProdPinnedLettersWithCount() {
        pinsYwmiObj.setFirst(first.toString()).setCount(count.toString());

        List<Envelope> envelopes = api(MessagesInFolderWithPins.class).params(pinsYwmiObj.setFid(folderList.defaultFID()))
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

        MatcherAssert.assertThat(String.format("Количество писем в диапазоне " +
                        "first=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                envelopes.size(), equalTo(PinnedThreadsCountTest.getExpected(first, count)));

        MatcherAssert.assertThat(String.format("Количество запиненных писем в диапазоне " +
                        "first=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                countPinnedLetters, equalTo(PinnedThreadsCountTest.getExpectedPinned(first, count)));

        MatcherAssert.assertThat(String.format("Количество незапиненных писем в диапазоне " +
                        "first=<%s> count=<%s> не совпадает с ожидаемым [DARIA-47151]", first, count),
                countNotPinnedLetters, equalTo(PinnedThreadsCountTest.getExpected(first, count) -
                        PinnedThreadsCountTest.getExpectedPinned(first, count)));
    }
}