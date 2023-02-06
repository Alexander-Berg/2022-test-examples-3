package ru.yandex.autotests.innerpochta.hound.data;

import lombok.val;
import ru.yandex.autotests.innerpochta.wmi.core.obj.GetFirstEnvelopeDateObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.CountersObject;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.InReplyToObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.LabelsObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderWithoutLabelObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByLabelObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByThreadObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadUsefulObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesWithAttachesObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsInfoObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Counters;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FirstEnvelopeDate;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.InReplyTo;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolderWithoutLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByThread;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByThreadWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnread;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnreadByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnreadUseful;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesWithAttaches;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Threads;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsByFolderCommand;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsInFolderWithPins;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsInfo;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.YamailStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static org.fest.util.Arrays.array;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

public class HoundB2BDataGenerator {
    @FunctionalInterface
    public interface TestMethod {
        Oper call(String uid, Map<String, String> params);
    }

    public static List<Object[]> apiHandles() {
        List<Object[]> operList = new ArrayList<>();

        operList.addAll(apiFolders());
        operList.addAll(apiLabels());
        operList.addAll(apiThreads());
        operList.addAll(apiThreadsByFolder());
        //MAILPG-317
        operList.addAll(apiFilterSearch());
        operList.addAll(apiMessagesByFolder());
        operList.addAll(apiMessageByLabel());
        operList.addAll(apiMessageByThread());
        operList.addAll(apiMessageUnread());
        operList.addAll(apiMessageUnreadByFolder());
        operList.addAll(apiMessageWithAttaches());
        operList.addAll(apiInReplyTo());
        operList.addAll(apiCounters());
        //DARIA-44825
        operList.addAll(apiMessagesInFolderWithPins());
        operList.addAll(apiThreadsInFolderWithPins());
        //DARIA-46510
        operList.addAll(apiMessagesByFolderWithoutLabel());
        //MAILPG-113 CAL-7217
        operList.addAll(apiMessagesUnreadUseful());
        //DARIA-47992
        operList.addAll(apiMessagesByThreadWithPins());
        //DARIA-48808
        operList.addAll(apiYamailStatus());
        //DARIA-50550
        operList.addAll(apiThreadsInfo());
        operList.addAll(apiGetFirstEnvelopeDate());

        return operList;
    }

    private static Object[] buildTestSuite(Class handler, BiFunction<String, Map<String, String>, Obj> paramsBuilder, String description) {
        TestMethod oper = (String uid, Map<String, String> params) -> {
            val obj = paramsBuilder.apply(uid, params);
            if (params.containsKey("post")) {
                obj.setContent(obj.asGet(false).replaceFirst("&", ""));
            }
            return api(handler).params(obj);
        };

        return array(oper, handler.getSimpleName() + " " + description);
    }

    private static Collection<? extends Object[]> apiMessagesUnreadUseful() {
        return asList(
                buildTestSuite(MessagesUnreadUseful.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadUsefulObj().setUid(uid),
                        "uid"),

                buildTestSuite(MessagesUnreadUseful.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadUsefulObj().setUid(uid).setCount("0"),
                        "count=0"),

                buildTestSuite(MessagesUnreadUseful.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadUsefulObj().setUid(uid).setCount("1"),
                        "count=1")
        );
    }

    private static Collection<? extends Object[]> apiMessagesByThreadWithPins() {
        return asList(
                buildTestSuite(MessagesByThreadWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setTid(params.get("tid"))
                                .setFirst("0").setCount("10"),
                        "uid"),

                buildTestSuite(MessagesByThreadWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid)
                                .setFirst("0").setCount("10"),
                        "without tid"),

                buildTestSuite(MessagesByThreadWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setTid(params.get("tid"))
                                .setPage("1").setCount("100"),
                        "with page")
        );
    }

    private static Collection<Object[]> apiCounters() {
        return asList(
                buildTestSuite(Counters.class, (String uid, Map<String, String> params) ->
                        new CountersObject().setUid(uid),
                        "uid"),

                buildTestSuite(Counters.class, (String uid, Map<String, String> params) ->
                        new CountersObject().setUid(params.get("non_exist_param")),
                        "wrong uid")
        );
    }

    private static List<Object[]> apiFolders() {
        return asList(
                buildTestSuite(Folders.class, (String uid, Map<String, String> params) ->
                        new FoldersObj().setUid(uid),
                        "uid"),

                buildTestSuite(Folders.class, (String uid, Map<String, String> params) ->
                        new FoldersObj().setUid(params.get("non_exist_param")),
                        "wrong uid")
        );
    }

    private static List<Object[]> apiLabels() {
        return asList(
                buildTestSuite(Labels.class, (String uid, Map<String, String> params) ->
                        new LabelsObj().setUid(uid),
                        "uid"),

                buildTestSuite(Labels.class, (String uid, Map<String, String> params) ->
                        new LabelsObj().setUid(params.get("non_exist_param")),
                        "wrong uid")
        );
    }

    private static List<Object[]> apiThreads() {
        return asList(
                buildTestSuite(Threads.class, (String uid, Map<String, String> params) ->
                        new ThreadsObj().setUid(uid).setTids(params.get("tid")),
                        "uid"),

                buildTestSuite(Threads.class, (String uid, Map<String, String> params) ->
                        new ThreadsObj().setUid(uid).setTids(params.get("non_exist_param")),
                       "with wrong tid"),

                buildTestSuite(Threads.class, (String uid, Map<String, String> params) ->
                        new ThreadsObj().setUid(params.get("non_exist_uid")).setTids(params.get("tid")),
                        "with wrong uid"),

                buildTestSuite(Threads.class, (String uid, Map<String, String> params) ->
                        new ThreadsObj().setUid(uid),
                        "MAILPG-317")
        );
    }

    private static List<Object[]> apiFilterSearch() {
        return asList(
                buildTestSuite(FilterSearchCommand.class, (String uid, Map<String, String> params) ->
                        new FilterSearchObj().setUid(uid).setOrder("date1").setFids(params.get("inbox_fid"))
                                .needMailboxRevision("1").setMids(params.get("mid0"), params.get("mid1")),
                        "[DARIA-52833]"),

                buildTestSuite(FilterSearchCommand.class, (String uid, Map<String, String> params) ->
                        new FilterSearchObj().setUid(uid).setOrder("date1").setFids(params.get("inbox_fid"))
                                .needMailboxRevision("1").setMids(params.get("mid0"), params.get("mid1"))
                                .setFullFoldersAndLabels("1"),
                        "[DARIA-52833]")
        );
    }

    private static List<Object[]> apiMessagesByFolder() {
        return asList(
                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1"),
                        "without inbox"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "uid"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(params.get("non_exist_uid")).setFirst("0")
                                .setCount("50").setSortType("date1").setFid(params.get("inbox_fid")),
                        "with wrong uid"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("non_exist_param")),
                        "with wrong fid"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50")
                                .setSortType(params.get("non_exist_param")).setFid(params.get("inbox_fid")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("50").setCount("50")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with big first"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("50").setCount("0")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with zero count"),

                buildTestSuite(MessagesByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setPage("1").setCount("50")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with page")
        );
    }

    private static List<Object[]> apiMessageByLabel() {
        return asList(
                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setFirst("0").setCount("50").setLid(params.get("lid"))
                                .setSortType("date1"),
                        "uid"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(params.get("non_exist_uid")).setFirst("0").setCount("50")
                                .setLid(params.get("lid")).setSortType("date1"),
                        "with wrong uid"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setFirst("0").setCount("50")
                                .setLid(params.get("non_exist_param")).setSortType("date1"),
                        "with wrong lid"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setFirst("0").setCount("50")
                                .setLid(params.get("lid")).setSortType(params.get("non_exist_param")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setFirst("50").setCount("50").setLid(params.get("lid"))
                                .setSortType("date1"),
                        "with big first"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setFirst("0").setCount("0").setLid(params.get("lid"))
                                .setSortType("date1"),
                        "with null count"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setPage("1").setCount("50").setLid(params.get("lid"))
                                .setSortType("date1"),
                        "with page"),

                buildTestSuite(MessagesByLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByLabelObj().setUid(uid).setPage("3").setCount("5").setLid("1")
                                .setSortType("date1"),
                        "MAILDEV-221")
        );
    }

    private static List<Object[]> apiMessageByThread() {
        return asList(
                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(uid).setFirst("0").setCount("50").setTid(params.get("tid"))
                                .setSortType("date1"),
                        "uid"),

                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(params.get("non_exist_uid")).setFirst("0").setCount("50")
                                .setTid(params.get("tid")).setSortType("date1"),
                        "with wrong uid"),

                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(uid).setFirst("0").setCount("50")
                                .setTid(params.get("non_exist_param")).setSortType("date1"),
                        "with wrong tid"),

                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(uid).setFirst("0").setCount("50")
                                .setTid(params.get("tid")).setSortType(params.get("non_exist_param")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(uid).setFirst("50").setCount("50")
                                .setTid(params.get("tid")).setSortType("date1"),
                        "with big first"),

                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(uid).setFirst("50").setCount("0")
                                .setTid(params.get("tid")).setSortType("date1"),
                        "with null count"),

                buildTestSuite(MessagesByThread.class, (String uid, Map<String, String> params) ->
                        new MessagesByThreadObj().setUid(uid).setPage("1").setCount("50")
                                .setTid(params.get("tid")).setSortType("date1"),
                        "with page")
        );
    }

    private static List<Object[]> apiMessageUnread() {
        return asList(
                buildTestSuite(MessagesUnread.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1"),
                        "uid"),

                buildTestSuite(MessagesUnread.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadObj().setUid(params.get("non_exist_uid")).setFirst("0").setCount("50")
                                .setSortType("date1"),
                        "with wrong uid"),

                buildTestSuite(MessagesUnread.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadObj().setUid(uid).setFirst("0").setCount("50")
                                .setSortType(params.get("non_exist_param")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesUnread.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadObj().setUid(uid).setFirst("50").setCount("50").setSortType("date1"),
                        "with big first"),

                buildTestSuite(MessagesUnread.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadObj().setUid(uid).setFirst("50").setCount("0").setSortType("date1"),
                        "with null count"),

                buildTestSuite(MessagesUnread.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadObj().setUid(uid).setPage("1").setCount("50")
                                .setSortType(params.get("non_exist_param")),
                        "with page")
        );
    }

    //DARIA-44825
    private static List<Object[]> apiMessagesInFolderWithPins() {
        return asList(
                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1"),
                        "without inbox"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "with uid"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(params.get("non_exist_uid")).setFirst("0").setCount("50")
                                .setSortType("date1").setFid(params.get("inbox_fid")),
                        "with wrong uid"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("non_exist_param")),
                        "with wrong fid"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("non_exist_param")
                                .setFid(params.get("inbox_fid")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("50").setCount("0").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "with null count"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setFirst("50").setCount("50").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "with big first"),

                buildTestSuite(MessagesInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderObj().setUid(uid).setPage("1").setCount("50").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "with page")
        );
    }

    private static List<Object[]> apiThreadsInFolderWithPins() {
        return asList(
                buildTestSuite(ThreadsInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new ThreadsByFolderObj().setUid(uid).setPage("1").setCount("10"),
                        "without inbox"),

                buildTestSuite(ThreadsInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new ThreadsByFolderObj().setUid(uid).setPage("1").setCount("10")
                                .setFid(params.get("inbox_fid")),
                        "with inbox"),

                buildTestSuite(ThreadsInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new ThreadsByFolderObj().setUid(params.get("non_exist_uid")).setPage("1").setCount("10")
                                .setFid(params.get("inbox_fid")),
                        "with NOT_EXIST_UID"),

                buildTestSuite(ThreadsInFolderWithPins.class, (String uid, Map<String, String> params) ->
                        new ThreadsByFolderObj().setUid(uid).setPage("1").setCount("10")
                                .setFid(params.get("non_exist_param")),
                        "with wrong FID")
        );
    }

    private static List<Object[]> apiMessagesByFolderWithoutLabel() {
        return asList(
                buildTestSuite(MessagesByFolderWithoutLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderWithoutLabelObj().setUid(uid).setPage("1").setCount("10")
                                .setLid(params.get("lid")),
                        "without inbox"),

                buildTestSuite(MessagesByFolderWithoutLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderWithoutLabelObj().setUid(uid).setPage("1").setCount("10")
                                .setLid(params.get("lid")).setFid(params.get("inbox_fid")),
                        "with inbox"),

                buildTestSuite(MessagesByFolderWithoutLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderWithoutLabelObj().setUid(params.get("non_exist_uid")).setPage("1")
                                .setCount("10").setLid(params.get("lid")).setFid(params.get("inbox_fid")),
                        "with NOT_EXIST_UID"),

                buildTestSuite(MessagesByFolderWithoutLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderWithoutLabelObj().setUid(uid).setPage("1")
                                .setCount("10").setLid(params.get("lid")).setFid(params.get("non_exist_param")),
                        "with not exist FID"),

                buildTestSuite(MessagesByFolderWithoutLabel.class, (String uid, Map<String, String> params) ->
                        new MessagesByFolderWithoutLabelObj().setUid(uid).setPage("1")
                                .setCount("10").setLid(params.get("non_exist_param")).setFid(params.get("inbox_fid")),
                        "with not exist LID")
        );
    }

    private static List<Object[]> apiMessageUnreadByFolder() {
        return asList(
                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1"),
                        "without inbox"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "uid"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(params.get("non_exist_uid")).setFirst("0")
                                .setCount("50").setSortType("date1").setFid(params.get("inbox_fid")),
                        "with wrong uid"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("non_exist_param")),
                        "with wrong fid"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setFirst("0").setCount("50")
                                .setSortType(params.get("non_exist_param")).setFid(params.get("inbox_fid")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setFirst("50").setCount("50")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with big first"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setFirst("50").setCount("0")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with zero count"),

                buildTestSuite(MessagesUnreadByFolder.class, (String uid, Map<String, String> params) ->
                        new MessagesUnreadByFolderObj().setUid(uid).setPage("1").setCount("50")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with page")
        );
    }

    private static List<Object[]> apiMessageWithAttaches() {
        return asList(
                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1"),
                        "without inbox"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("inbox_fid")),
                        "uid"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(params.get("non_exist_uid")).setFirst("0")
                                .setCount("50").setSortType("date1").setFid(params.get("inbox_fid")),
                        "with wrong uid"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setFirst("0").setCount("50").setSortType("date1")
                                .setFid(params.get("non_exist_param")),
                        "with wrong fid"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setFirst("0").setCount("50")
                                .setSortType(params.get("non_exist_param")).setFid(params.get("inbox_fid")),
                        "with wrong sort_type"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setFirst("50").setCount("50")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with big first"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setFirst("50").setCount("0")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with zero count"),

                buildTestSuite(MessagesWithAttaches.class, (String uid, Map<String, String> params) ->
                        new MessagesWithAttachesObj().setUid(uid).setPage("1").setCount("50")
                                .setFid(params.get("inbox_fid")).setSortType("date1"),
                        "with page")
        );
    }

    private static List<Object[]> apiThreadsByFolder() {
        return asList(
                buildTestSuite(ThreadsByFolderCommand.class, (String uid, Map<String, String> params) ->
                        ThreadsByFolderObj.empty().setUid(uid).setCount("10").setPage("1"),
                "without inbox"),

                buildTestSuite(ThreadsByFolderCommand.class, (String uid, Map<String, String> params) ->
                        ThreadsByFolderObj.empty().setUid(uid).setCount("10").setPage("1")
                                .setFid(params.get("inbox_fid")),
                "with inbox"),

                buildTestSuite(ThreadsByFolderCommand.class, (String uid, Map<String, String> params) ->
                        ThreadsByFolderObj.empty().setUid(params.get("non_exist_uid")).setCount("10").setPage("1")
                                .setFid(params.get("inbox_fid")),
                "with NOT_EXIST_UID"),

                buildTestSuite(ThreadsByFolderCommand.class, (String uid, Map<String, String> params) ->
                        ThreadsByFolderObj.empty().setUid(uid).setCount("10").setPage("1")
                                .setFid(params.get("non_exist_param")),
                "with NOT_EXIST_FID")
        );
    }

    private static List<Object[]> apiInReplyTo() {
        return asList(
                buildTestSuite(InReplyTo.class, (String uid, Map<String, String> params) ->
                        InReplyToObj.empty().setUid(uid)
                                .setMessageId(params.get("msgid")),
                        "with uid"),

                buildTestSuite(InReplyTo.class, (String uid, Map<String, String> params) ->
                        InReplyToObj.empty().setUid(params.get("non_exist_param")),
                        "wrong uid"),

                buildTestSuite(InReplyTo.class, (String uid, Map<String, String> params) ->
                        InReplyToObj.empty().setUid(uid),
                        "with wrong message_id"),

                buildTestSuite(InReplyTo.class, (String uid, Map<String, String> params) ->
                        InReplyToObj.empty().setUid(uid)
                                .setMessageId(params.get("msgid")),
                        "with DRAFT")
        );
    }

    private static List<Object[]> apiYamailStatus() {
        List<Object[]> operList = new ArrayList<>();
        operList.add(
                buildTestSuite(YamailStatus.class, (String uid, Map<String, String> params) ->
                        CountersObject.empty().setUid(uid),
                        "with uid")
        );
        return operList;
    }

    //DARIA-50550
    private static List<Object[]> apiThreadsInfo() {
        return asList(
                buildTestSuite(ThreadsInfo.class,
                        (String uid, Map<String, String> params) ->
                                ThreadsInfoObj.empty().setUid(uid).setTid(params.get("tid")),
                        "with tid"),

                buildTestSuite(ThreadsInfo.class,
                        (String uid, Map<String, String> params) ->
                                ThreadsInfoObj.empty().setUid(uid).setTid(params.get("tid"), params.get("tid2")),
                        "with tids"),

                buildTestSuite(ThreadsInfo.class,
                        (String uid, Map<String, String> params) ->
                                ThreadsInfoObj.empty().setUid(params.get("non_exist_uid")).setTid(params.get("tid")),
                        "with NOT_EXIST_UID")
        );
    }

    //DARIA-50550
    private static List<Object[]> apiGetFirstEnvelopeDate() {
        List<Object[]> operList = new ArrayList<>();
        operList.add(
                buildTestSuite(FirstEnvelopeDate.class, (String uid, Map<String, String> params) ->
                        GetFirstEnvelopeDateObj.getEmptyObj()
                                .setUid(uid).setFid(params.get("inbox_fid")),
                        "with uid")
        );

        return operList;
    }
}
