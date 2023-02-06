package ru.yandex.autotests.innerpochta.hound;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.Error;
import ru.yandex.autotests.innerpochta.beans.yplatform.DeletedMessage;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.DeletedMessagesObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.DeletedMessages;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withCode;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withMessage;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withReason;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.apply;
import static ru.yandex.autotests.innerpochta.hound.ThreadsByTimestampRangeNew.timestampGenerator;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

@Aqua.Test
@Title("[HOUND] ручка deleted_messages")
@Description("Общие тесты на ручку deleted_messages")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@RunWith(DataProviderRunner.class)
@Credentials(loginGroup = "DeletedMessagesTest")
@Issue("MAILPG-1779")
public class DeletedMessagesTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("deleted_messages возвращает ошибку если не задан uid")
    @IgnoreForPg("MAILPG-2767")
    public void testDeletedMessagesUidRequiredError() throws Exception {
        Error err = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setFirst("0")
                        .setCount("1")
        ).parsed();

        assertThat("Должна быть ошибка", err, allOf(
                withCode(is(5001L)),
                withMessage(is("invalid argument")),
                withReason(is("uid parameter is required"))
        ));
    }

    @Test
    @Title("deleted_messages возвращает ошибку если не задан count")
    public void testDeletedMessagesCountRequiredError() throws Exception {
        Error err = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
        ).parsed();

        assertThat("Должна быть ошибка", err, allOf(
                withCode(is(5001L)),
                withMessage(is("invalid argument")),
                withReason(is("count parameter is required"))
        ));
    }

    @Test
    @Title("deleted_messages возвращает ошибку если не задан first или page")
    public void testDeletedMessagesFirstOrPageRequiredError() throws Exception {
        Error err = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setCount("1")
        ).parsed();

        assertThat("Должна быть ошибка", err, allOf(
                withCode(is(5001L)),
                withMessage(is("invalid argument")),
                withReason(is("first or page parameter is required"))
        ));
    }

    @Test
    @Title("deleted_messages возвращает mid и stid удаленного письма")
    public void testDeletedMessagesByFirstReturnsMidAndStid() throws Exception {
        DeletedMessage message = getDeletedInfo(sendAndDeleteMessage());

        List<DeletedMessage> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("1")
        ).midsAndStids();

        assertThat("Количество писем больше ожидаемого",
                deletedMessages.size(), lessThanOrEqualTo(1));
        assertThat("Среди полученных писем должно быть ранее удаленное",
                deletedMessages.get(0), equalTo(message));
    }

    @Test
    @Title("deleted_messages возвращает mid и stid удаленного письма")
    public void testDeletedMessagesByPageReturnsMidAndStid() throws Exception {
        DeletedMessage message = getDeletedInfo(sendAndDeleteMessage());

        List<DeletedMessage> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setPage("1")
                        .setCount("0")
        ).midsAndStids();

        assertThat("Количество писем больше ожидаемого",
                deletedMessages.size(), lessThanOrEqualTo(1));
        assertThat("Среди полученных писем должно быть ранее удаленное",
                deletedMessages.get(0), equalTo(message));
    }

    @DataProvider
    public static List<List<Object>> data() {
        return timestampGenerator();
    }

    @Test
    @Issue("MAILPG-2274")
    @Description("Проверяем выборку удаленных писем с фильтрацией по дате получения")
    @UseDataProvider("data")
    public void testDeletdMessagesWithSinceAndTill(BiFunction<Envelope, Envelope, Long> since_,
                                                   BiFunction<Envelope, Envelope, Long> till_,
                                                   BiFunction<Envelope, Envelope, List<Envelope>> expected_) throws Exception {
        Envelope first = sendAndDeleteMessage();
        Thread.sleep(1000); // guarantee of different received_date
        Envelope second = sendAndDeleteMessage();

        String since = apply(since_, first, second);
        String till = apply(till_, first, second);

        List<DeletedMessage> expected = expected_.apply(first, second)
                .stream().map(this::getDeletedInfo).collect(Collectors.toList());
        List<DeletedMessage> notExpected = asList(first, second)
                .stream().map(this::getDeletedInfo).collect(Collectors.toList());
        notExpected.removeAll(expected);

        List<DeletedMessage> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("2")
                        .setSince(since)
                        .setTill(till)
        ).midsAndStids();

        assertThat("Письма должны были попасть в выборку по датам",
                deletedMessages, containsAll(expected));
        assertThat("Письма не должны были попасть в выборку по датам",
                deletedMessages, not(containsAny(notExpected)));
    }

    @Test
    @Issue("MAILPG-2282")
    @Description("deleted_messages возвращает ошибку если задан неверный формат")
    public void testDeletedMessagesInvalidFormat() throws Exception {
        Error err = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("1")
                        .setFormat("blahblah")
        ).parsed();

        assertThat("Должна быть ошибка", err, allOf(
                withCode(is(5001L)),
                withMessage(is("invalid argument")),
                withReason(is("unknown format: blahblah"))
        ));
    }

    @Test
    @Issue("MAILPG-2282")
    @Description("Прверяем выдачу ручки /deleted_messages в коротком формате")
    public void testDeletedMessagesMidStidFormat() throws Exception {
        DeletedMessage message = getDeletedInfo(sendAndDeleteMessage());

        List<DeletedMessage> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("1")
                        .setFormat("mid_stid")
        ).midsAndStids();

        assertThat("Количество писем больше ожидаемого",
                deletedMessages.size(), lessThanOrEqualTo(1));
        assertThat("Среди полученных писем должно быть ранее удаленное",
                deletedMessages.get(0), equalTo(message));
    }

    @Test
    @Issue("MAILPG-2282")
    @Description("Прверяем выдачу ручки /deleted_messages в полном формате")
    public void testDeletedMessagesFullFormat() throws Exception {
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();

        Envelope envelope = sendAndDeleteMessage(attach);

        List<Envelope> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("1")
                        .setFormat("full")
        ).envelopes();

        assertThat("Количество писем больше ожидаемого",
                deletedMessages.size(), lessThanOrEqualTo(1));
        assertThat("Среди полученных писем должно быть ранее удаленное",
                deletedMessages.get(0), matchDeleted(envelope));
    }

    @Test
    @Issue("MAILPG-2282")
    @Description("Проверяем выборку удаленных писем с фильтрацией по дате получения в полном формате")
    public void testDeletdMessagesWithSinceAndTillFullFormat() throws Exception {
        Envelope envelope = sendAndDeleteMessage();

        Long since = envelope.getReceiveDate() - 1;
        Long till = envelope.getReceiveDate() + 1;

        List<Envelope> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("1")
                        .setSince(since.toString())
                        .setTill(till.toString())
                        .setFormat("full")
        ).envelopes();

        assertThat("Количество писем больше ожидаемого",
                deletedMessages.size(), lessThanOrEqualTo(1));
        assertThat("Среди полученных писем должно быть ранее удаленное",
                deletedMessages.get(0), matchDeleted(envelope));
    }

    @Test
    @Issue("MAILPG-2282")
    @Description("Не должны отдавать ошибку, если на удаленном письме стояла удаленная метка")
    public void testDeletedMessagesWithDeletedLabel() throws Exception {
        Envelope envelope = sendMessage();
        String lid = Mops.newLabelByName(authClient, getRandomString());
        Mops.label(authClient, new MidsSource(envelope.getMid()), asList(lid)).post(shouldBe(okSync()));
        deleteMessage(envelope.getMid());
        Mops.deleteLabel(authClient, lid).post(shouldBe(okSync()));

        List<Envelope> deletedMessages = getDeletedMessages(
                DeletedMessagesObj.empty()
                        .setUid(uid())
                        .setFirst("0")
                        .setCount("1")
                        .setFormat("full")
        ).envelopes();

        assertThat("Количество писем больше ожидаемого",
                deletedMessages.size(), lessThanOrEqualTo(1));
        assertThat("Среди полученных писем должно быть ранее удаленное",
                deletedMessages.get(0), matchDeleted(envelope));
    }

    private Envelope sendAndDeleteMessage(File ... attaches) throws Exception {
        Envelope envelope = sendMessage(attaches);
        deleteMessage(envelope.getMid());
        return envelope;
    }

    private Envelope sendMessage(File ... attaches) throws Exception {
        return sendWith(authClient).addAttaches(attaches).send().waitDeliver().getEnvelopes().get(0);
    }

    private void deleteMessage(String mid) throws Exception {
        Mops.purge(authClient, new MidsSource(mid)).post(shouldBe(okSync()));
    }

    private DeletedMessage getDeletedInfo(Envelope envelope) {
        return new DeletedMessage()
                .withMid(envelope.getMid())
                .withStid(envelope.getStid());
    }

    private DeletedMessages getDeletedMessages(DeletedMessagesObj params) {
        return api(DeletedMessages.class)
                .setHost(props().houndUri())
                .params(params)
                .get()
                .via(authClient);
    }

    private static Matcher<List<DeletedMessage>> containsAll(List<DeletedMessage> messages) {
        return allOf(messages.stream()
                .map(Matchers::hasItem)
                .toArray(Matcher[]::new));
    }

    private static Matcher<List<DeletedMessage>> containsAny(List<DeletedMessage> messages) {
        return anyOf(messages.stream()
                .map(Matchers::hasItem)
                .toArray(Matcher[]::new));
    }

    private static Matcher<Envelope> matchDeleted(Envelope envelope) {
        return allOf(
                hasProperty("mid", equalTo(envelope.getMid())),
                hasProperty("stid", equalTo(envelope.getStid())),
                hasProperty("receiveDate", equalTo(envelope.getReceiveDate())),
                hasProperty("subject", equalTo(envelope.getSubject())),
                hasProperty("firstline", equalTo(envelope.getFirstline())),
                hasProperty("attachments", equalTo(envelope.getAttachments())),
                hasProperty("from", equalTo(envelope.getFrom())),
                hasProperty("to", equalTo(envelope.getTo()))
        );
    }
}
