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
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsByFolderCommand;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Тест ручки threads_by_folder с параметрами since и till")
@Description("Проверяем только соответствие количества писем")
@Features(MyFeatures.HOUND)
@Stories({MyStories.BY_TIMESTAMP, MyStories.THREAD_LIST})
@RunWith(DataProviderRunner.class)
@Credentials(loginGroup = "ThreadsParamsSinceTest")
@Issue("MAILPG-379")
public class ThreadsByTimestampRangeNew extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Description("При фильтрации по timestamp должны учитываться только самые новые письма треда." +
            "Если самое новое письмо треда отбрасывается фильтром, то тред не должен попадать в выдачу.")
    public void threadsByFolderWithSinceAndTillShouldReturnOnlyNewestMessageInThread() throws InterruptedException {
        Optional<Envelope> first = sendWith(authClient).viaProd().send().waitDeliver().getEnvelope();
        assertTrue("Не смогли получить envelope первого письма", first.isPresent());
        Thread.sleep(1000); // guarantee of different received_date
        Optional<Envelope> second = sendWith(authClient).viaProd().inReplyTo(first.get().getRfcId()).send().waitDeliver().getEnvelope();
        assertTrue("Не смогли получить envelope второго письма", second.isPresent());

        Long since = first.get().getReceiveDate() - 10L;
        Long till = first.get().getReceiveDate() + 1L;

        List<ru.yandex.autotests.innerpochta.beans.yplatform.Envelope> envelopes =
                api(ThreadsByFolderCommand.class).params(ThreadsByFolderObj.empty()
                        .setSince(since.toString())
                        .setTill(till.toString())
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("50")
                        .setFid(folderList.defaultFID()))
                        .setHost(props().houndUri()).get().via(authClient).resp().getThreadsByFolder().getEnvelopes();

        assertThat(String.format("Количество писем в диапазоне " +
                        "since=<%s> till=<%s> не совпадает с ожидаемым", since, till),
                envelopes.size(), equalTo(0));
    }

    @DataProvider
    public static List<List<Object>> data() {
        return timestampGenerator();
    }

    @Test
    @Issue("DARIA-34367")
    @UseDataProvider("data")
    public void threadsByFolderWithSinceAndTillCounterChecks(BiFunction<Envelope, Envelope, Long> since_,
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

        List<ru.yandex.autotests.innerpochta.beans.yplatform.Envelope> envelopes =
                api(ThreadsByFolderCommand.class).params(ThreadsByFolderObj.empty()
                        .setSince(since)
                        .setTill(till)
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("50")
                        .setFid(folderList.defaultFID()))
                        .setHost(props().houndUri()).get().via(authClient).resp().getThreadsByFolder().getEnvelopes();

        assertThat(String.format("Количество писем в диапазоне " +
                        "since=<%s> till=<%s> не совпадает с ожидаемым [DARIA-34367]", since, till),
                envelopes.size(), equalTo(expected));
    }

    //TEST DATA: since, till, expected
    public static List<List<Object>> timestampGenerator() {
        List<List<Object>> data = new ArrayList<>();

        final BiFunction<Envelope, Envelope, Long> zero = (Envelope first, Envelope second) -> 0L;
        final BiFunction<Envelope, Envelope, Long> nil = (Envelope first, Envelope second) -> null;
        final BiFunction<Envelope, Envelope, Long> future = (Envelope first, Envelope second) ->
                new Timestamp(System.currentTimeMillis()).getTime() / 1000 + 10000000L;

        final BiFunction<Envelope, Envelope, Long> firstArg = (Envelope first, Envelope second) -> first.getReceiveDate();
        final BiFunction<Envelope, Envelope, Long> secondArg = (Envelope first, Envelope second) -> second.getReceiveDate();

        final Function<Long, Long> plusOne = (Long arg) -> arg + 1L;
        final Function<Long, Long> minusOne = (Long arg) -> arg - 1L;

        final BiFunction<Envelope, Envelope, List<Envelope>> noEnvelopes =
                (Envelope first, Envelope second) -> new ArrayList();
        final BiFunction<Envelope, Envelope, List<Envelope>> firstEnvelope =
                (Envelope first, Envelope second) -> new ArrayList(asList(first));
        final BiFunction<Envelope, Envelope, List<Envelope>> secondEnvelope =
                (Envelope first, Envelope second) -> new ArrayList(asList(second));
        final BiFunction<Envelope, Envelope, List<Envelope>> bothEnvelopes =
                (Envelope first, Envelope second) -> new ArrayList(asList(first, second));

        data.add(asList(zero, zero, bothEnvelopes));
        data.add(asList(zero, firstArg, noEnvelopes));

        //MAILPG-379 стало: from <= date < to;
        ///from <= date
        data.add(asList(zero, firstArg.andThen(plusOne), firstEnvelope));

        data.add(asList(zero, firstArg.andThen(plusOne).andThen(plusOne), firstEnvelope));

        data.add(asList(firstArg, firstArg.andThen(plusOne), firstEnvelope));
        //date < to;
        data.add(asList(firstArg.andThen(minusOne), firstArg, noEnvelopes));

        data.add(asList(firstArg.andThen(plusOne), firstArg.andThen(plusOne), noEnvelopes));

        data.add(asList(secondArg.andThen(minusOne), secondArg.andThen(plusOne), secondEnvelope));
        data.add(asList(firstArg.andThen(plusOne), zero, secondEnvelope));

        data.add(asList(firstArg.andThen(minusOne), future, bothEnvelopes));

        //since > till
        data.add(asList(future, secondArg.andThen(plusOne), noEnvelopes));

        data.add(asList(future, future, noEnvelopes));

        data.add(asList(future, nil, noEnvelopes));
        data.add(asList(nil, future, bothEnvelopes));

        data.add(asList(firstArg.andThen(plusOne), zero.andThen(plusOne), noEnvelopes));

        return data;
    }

    public static String apply(BiFunction<Envelope, Envelope, Long> f, Envelope first, Envelope second) {
        return Optional.ofNullable(f.apply(first, second))
                .map(Object::toString)
                .orElse("");
    }
}
