package ru.yandex.autotests.innerpochta.hound;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.apply;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.timestampGenerator;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Тест ручки messages_by_folder с параметрами since и till")
@Description("Проверяем только соответствие количества писем")
@Features(MyFeatures.HOUND)
@Stories(MyStories.BY_TIMESTAMP)
@RunWith(DataProviderRunner.class)
@Credentials(loginGroup = "MessagesByFolderWithTimestamp")
@Issue("MAILPG-379")
public class LettersByTimestampRangeNew extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @DataProvider
    public static List<List<Object>> data() {
        return timestampGenerator();
    }

    @Test
    @Issues({@Issue("DARIA-32622"), @Issue("DARIA-34367")})
    @Description("Проверяем количество писем, которые возвращает ручка")
    @UseDataProvider("data")
    public void messagesByFolderWithSinceAndTillCounterChecks(BiFunction<Envelope, Envelope, Long> since_,
                                                              BiFunction<Envelope, Envelope, Long> till_,
                                                              BiFunction<Envelope, Envelope, List<Envelope>> expected_)
            throws InterruptedException
    {
        Optional<Envelope> first = sendWith(authClient).viaProd().send().waitDeliver().getEnvelope();
        Thread.sleep(1000); // guarantee of different received_date
        Optional<Envelope> second = sendWith(authClient).viaProd().send().waitDeliver().getEnvelope();

        assertTrue("Получили неожиданное кол-во писем", first.isPresent() && second.isPresent());

        String since = apply(since_, first.get(), second.get());
        String till = apply(till_, first.get(), second.get());
        int expected = expected_.apply(first.get(), second.get()).size();

        List<Envelope> envelopes = api(MessagesByFolder.class).params(MessagesByFolderObj.empty()
                .setSince(since)
                .setTill(till)
                .setUid(uid())
                .setFirst("0")
                .setCount("50")
                .setFid(folderList.defaultFID()))
                .setHost(props().houndUri()).get().via(authClient).resp().getEnvelopes();

        assertThat(String.format("Количество писем в диапазоне " +
                        "since=<%s> till=<%s> не совпадает с ожидаемым [DARIA-34367]", since, till),
                envelopes.size(), equalTo(expected));
    }
}
