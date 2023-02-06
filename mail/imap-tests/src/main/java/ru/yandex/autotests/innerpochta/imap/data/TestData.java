package ru.yandex.autotests.innerpochta.imap.data;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.sun.mail.imap.protocol.BASE64MailboxEncoder.encode;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.ANSWERED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.DELETED;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.DRAFT;
import static ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags.SEEN;
import static ru.yandex.autotests.innerpochta.imap.converters.ToObjectConverter.wrap;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.02.14
 * Time: 18:35
 */
public class TestData {
    private TestData() {
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> testMessages() {
        //todo: не можем заатачить письма с русским текстом
        return with(
                "/messages/hrs_hong_kong.eml",
                "/messages/header1message.eml",
                "/messages/complicated_message.eml",
//                "/messages/empty.eml",
//                "/messages/htmlWithAttach.eml",
//                "/messages/largeLetter.eml",
//                "/messages/htmlNoSubject.eml",
                "/messages/inline_attach_message.eml",
//                "/messages/invite1.eml",
                "/messages/multipleattach_mail1.eml",
//                "/messages/plaintext.eml",
//                "/messages/plainwithAttaches.eml",
                "/messages/softspam.eml"
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> allKindsOfInbox() {
        return with(
                "inbox",
                "INBOX",
                "INbox",
                "Inbox"
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> allSystemFolders() {
        return with(
                systemFolders().getSent(),                  //todo: добавить inbox
                systemFolders().getDeleted(),
                systemFolders().getDrafts(),
                systemFolders().getOutgoing(),
                systemFolders().getSpam()
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> allKindsOfFolders() {
        return with(
                Utils.generateName(),
                encode("папко"),
                "sdfg!fff",
                "-1",
                "NIL",
                "VERYVERYVERYVERYVERY"
//                todo: добавить папки с экранированием
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> badSequenceMessages() {
        return with(
                "0",
                "0:1",
                "1:0",
                "0:0",
                "0,1",
                "1,0",
                "1:",
                ":1",
                ":*",
                "*:",
                "0:*",
                "*:0",
                ":",          //todo: добавить отрицательную последовательнсть сообщений
                "-1:*",
                "-10:",
                ":-10",
                "-10:-12",
                "-1:2",
                "2:-3",
                ""
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> goodSequenceMessages() {
        return with(
                "1",
                "1:1",
                "1:2",
                "2:1",
                "1,2",
                "1,*",
                "10",
                "01",
                "12,13",
//                "1:9999",
//                "10:9999",
                "*",
                "1:*",
                "*:3",
                "*:*"
        ).convert(wrap());
    }

    public static List<Object[]> allFlags() {
        List<Object[]> allFlags = new ArrayList<Object[]>();
        allFlags.addAll(getSystemFlags());
        allFlags.addAll(getUserFlags());
        allFlags.addAll(getMixedFlags());
        allFlags.addAll(getSystemMixedFlags());

        return allFlags;
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> getSystemFlags() {
        return with(roundBraceList(SEEN.value()),
                roundBraceList(DELETED.value()),
                roundBraceList(DRAFT.value())
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> getSystemMixedFlags() {
        return with(roundBraceList(SEEN.value(), ANSWERED.value()),
                roundBraceList(SEEN.value(), ANSWERED.value(), DELETED.value()),
                roundBraceList(SEEN.value(), ANSWERED.value(), DELETED.value(), DRAFT.value())
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> getUserFlags() {
        return with(
                roundBraceList(MessageFlags.NOT_JUNK),
                //при установки флага $Junk проставляется флаг /Seen
                roundBraceList(MessageFlags.JUNK, SEEN.value()),
                roundBraceList(encode(MessageFlags.CYRILLIC)),
                roundBraceList("123123123"),
                roundBraceList("1!uiui"),
                roundBraceList("NIL")
        ).convert(wrap());
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> getMixedFlags() {
        return with(
                roundBraceList(SEEN.value(), ANSWERED.value(), DELETED.value(), DRAFT.value(),
                        encode(MessageFlags.CYRILLIC))
        ).convert(wrap());
    }
}
