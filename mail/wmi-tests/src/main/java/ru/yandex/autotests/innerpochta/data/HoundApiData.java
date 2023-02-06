package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.wmi.core.obj.GetFirstEnvelopeDateObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.net.URLEncoder.encode;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.InReplyToObj.empty;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.10.13
 * Time: 15:40
 * <p/>
 * http://wiki.yandex-team.ru/users/jkennedy/ywmiapi
 * <p/>
 * Класс представляет собой тестовые данные для ручек.
 */
public class HoundApiData {
    //yplatformtest
    public static final String UID = "227053667";
    public static final String UID_PG = "320178487";
    public static final String TID = "2220000003640191895";
    public static final String TID_2 = "2510000000000625889";
    public static final String INBOX_FID = "2510000490000066302";
    public static final String INBOX_FID_PG = "1";
    public static final String LABEL_ID = "2220000001908416270";

    public static final String NOT_EXIST_UID = "10001";
    public static final String NOT_EXIST_PARAM = "21463124314";

    public static List<Object[]> apiHandles() {
        List<Object[]> operList = new ArrayList<Object[]>();

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
//        //DARIA-44825
        operList.addAll(apiMessagesInFolderWithPins());
        operList.addAll(apiThreadsInFolderWithPins());
//        //DARIA-46510
        operList.addAll(apiMessagesByFolderWithoutLabel());
//        //MAILPG-113 CAL-7217
        operList.addAll(apiMessagesUnreadUseful());
//        //DARIA-47992
        operList.addAll(apiMessagesByThreadWithPins());
//        //DARIA-48808
        operList.addAll(apiYamailStatus());
        //DARIA-50550
        operList.addAll(apiThreadsInfo());

        return operList;
    }

    private static Collection<? extends Object[]> apiMessagesUnreadUseful() {
        List<Object[]> operList = new ArrayList<Object[]>();
        operList.add(new Object[]{
                api(MessagesUnreadUseful.class),
                new MessagesUnreadUsefulObj().setUid(UID),
                MessagesUnreadUseful.class.getSimpleName() + " uid"
        });

        operList.add(new Object[]{
                api(MessagesUnreadUseful.class),
                new MessagesUnreadUsefulObj().setUid(UID)
                .setCount("0"),
                MessagesUnreadUseful.class.getSimpleName() + " count=0"
        });

        operList.add(new Object[]{
                api(MessagesUnreadUseful.class),
                new MessagesUnreadUsefulObj().setUid(UID)
                        .setCount("1"),
                MessagesUnreadUseful.class.getSimpleName() + " count=1"
        });

        return operList;
    }

    private static Collection<? extends Object[]> apiMessagesByThreadWithPins() {
        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(MessagesByThreadWithPins.class),
                new MessagesByFolderObj().setUid(UID).setTid(TID)
                        .setFirst("0").setCount("10"),
                MessagesByThreadWithPins.class.getSimpleName() + " uid"
        });

        operList.add(new Object[]{
                api(MessagesByThreadWithPins.class),
                new MessagesByFolderObj().setUid(UID)
                        .setFirst("0").setCount("10"),
                MessagesByThreadWithPins.class.getSimpleName() + " without tid"
        });

        operList.add(new Object[]{
                api(MessagesByThreadWithPins.class),
                new MessagesByFolderObj().setUid(UID).setTid(TID)
                        .setPage("1").setCount("100"),
                MessagesByThreadWithPins.class.getSimpleName() + " with page"
        });

        return operList;
    }

    private static Collection<Object[]> apiCounters() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //counters with uid
        operList.add(new Object[]{
                api(Counters.class),
                new CountersObject().setUid(UID),
                Counters.class.getSimpleName() + " uid"
        });

        //counters with wrong uid
        operList.add(new Object[]{
                api(Counters.class),
                new CountersObject().setUid(NOT_EXIST_PARAM),
                Counters.class.getSimpleName() + " wrong uid"
        });

        return operList;
    }

    public static List<Object[]> apiFolders() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //folders
        operList.add(new Object[]{
                api(Folders.class),
                new FoldersObj().setUid(UID),
                Folders.class.getSimpleName() + " uid"
        });

        //folders with wrong uid
        operList.add(new Object[]{
                api(Folders.class),
                new FoldersObj()
                        .setUid(NOT_EXIST_UID),
                Folders.class.getSimpleName() + " wrong uid"
        });

        return operList;
    }

    public static List<Object[]> apiLabels() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //labels uid
        operList.add(new Object[]{
                api(Labels.class),
                new LabelsObj().setUid(UID),
                Labels.class.getSimpleName() + " uid"
        });

        //labels with wrong uid
        operList.add(new Object[]{
                api(Labels.class),
                new LabelsObj()
                        .setUid(NOT_EXIST_UID),
                Labels.class.getSimpleName() + " wrong uid"
        });

        return operList;
    }

    public static List<Object[]> apiThreads() {
        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(Threads.class),
                new ThreadsObj()
                        .setUid(UID)
                        .setTids(TID),
                Threads.class.getSimpleName() + " uid"
        });

        //threads with wrong tids
        operList.add(new Object[]{
                api(Threads.class),
                new ThreadsObj()
                        .setUid(UID)
                        .setTids(NOT_EXIST_PARAM),
                Threads.class.getSimpleName() + " with wrong tid"
        });

        //threads with wrong uid
        operList.add(new Object[]{
                api(Threads.class),
                new ThreadsObj()
                        .setUid(NOT_EXIST_UID)
                        .setTids(TID),
                Threads.class.getSimpleName() + " with wrong uid"
        });

        operList.add(new Object[]{
                api(Threads.class),
                new ThreadsObj()
                        .setUid(UID),
                Threads.class.getSimpleName() + " MAILPG-317"
        });

        return operList;
    }

    public static List<Object[]> apiFilterSearch() {
        List<Object[]> operList = new ArrayList<Object[]>();

        //DARIA-52833
        operList.add(new Object[]{
                api(FilterSearchCommand.class),
                new FilterSearchObj()
                        .setUid(UID)
                        .setOrder("date1")
                        .setFids(INBOX_FID_PG)
                        .needMailboxRevision("1")
                        .setMids("2530000000704793148", "2530000000476095559"),
                FilterSearchCommand.class.getSimpleName() + " [DARIA-52833]"
        });

        //DARIA-52833
        operList.add(new Object[]{
                api(FilterSearchCommand.class),
                new FilterSearchObj()
                        .setUid(UID)
                        .setOrder("date1")
                        .setFids(INBOX_FID_PG)
                        .setFullFoldersAndLabels("1")
                        .needMailboxRevision("1")
                        .setMids("2530000000704793148", "2530000000476095559"),
                FilterSearchCommand.class.getSimpleName() + " [DARIA-52833]"
        });

        return operList;
    }

    public static List<Object[]> apiMessagesByFolder() {
        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " without inbox"
        });

        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " uid"
        });

        //MAILPG-704
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID_PG)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID_PG)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " uid"
        });

        //messages_by_folder with wrong uid
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " with wrong uid"
        });

        //messages_by_folder with wrong fid
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(NOT_EXIST_PARAM)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " with wrong fid"
        });

        //messages_by_folder with wrong sort_type
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType(NOT_EXIST_PARAM),
                MessagesByFolder.class.getSimpleName() + " with wrong sort_type"
        });

        //messages_by_folder with big first
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " with big first"
        });

        //messages_by_folder with null count
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("0")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " with null count"
        });

        //messages_by_folder with page
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setPage("1")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesByFolder.class.getSimpleName() + " with page"
        });

        return operList;
    }

    public static List<Object[]> apiMessageByLabel() {
        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setLid(LABEL_ID)
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " uid"
        });

        //messages_by_label with wrong uid
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setLid(LABEL_ID)
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " with wrong uid"
        });

        //messages_by_label with wrong lid
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setLid(NOT_EXIST_PARAM)
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " with wrong lid"
        });

        //messages_by_label with wrong sort_type
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setLid(LABEL_ID)
                        .setSortType(NOT_EXIST_PARAM),
                MessagesByLabel.class.getSimpleName() + " with wrong sort_type"
        });

        //messages_by_label with big first
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setLid(LABEL_ID)
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " with big first"
        });

        //messages_by_label with null count
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("0")
                        .setLid(LABEL_ID)
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " with null count"
        });

        //messages_by_label with page
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setPage("1")
                        .setCount("50")
                        .setLid(LABEL_ID)
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " with page"
        });

        //MAILDEV-221
        //messages_by_label with page for pg
        operList.add(new Object[]{
                api(MessagesByLabel.class),
                new MessagesByLabelObj()
                        .setUid(UID)
                        .setPage("3")
                        .setCount("5")
                        .setLid("1")
                        .setSortType("date1"),
                MessagesByLabel.class.getSimpleName() + " with page"
        });

        return operList;
    }

    public static List<Object[]> apiMessageByThread() {
        List<Object[]> operList = new ArrayList<Object[]>();

        //messages_by_thread
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setTid(TID)
                        .setSortType("date1"),
                MessagesByThread.class.getSimpleName() + " uid"
        });

        //messages_by_thread with wrong uid
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setTid("2220000003607514267")
                        .setSortType("date1"),
                MessagesByThread.class.getSimpleName() + " with wrong uid"
        });

        //messages_by_thread with wrong tid
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setTid(NOT_EXIST_PARAM)
                        .setSortType("date1"),
                MessagesByThread.class.getSimpleName() + " with wrong tid"
        });

        //messages_by_thread with wrong sort_type
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setTid(TID)
                        .setSortType(NOT_EXIST_PARAM),
                MessagesByThread.class.getSimpleName() + " with wrong sort_type"
        });

        //messages_by_thread with big first
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setTid(TID)
                        .setSortType("date1"),
                MessagesByThread.class.getSimpleName() + " with big first"
        });

        //messages_by_thread with null count
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("0")
                        .setTid(TID)
                        .setSortType("date1"),
                MessagesByThread.class.getSimpleName() + " with null count"
        });

        //messages_by_thread with page
        operList.add(new Object[]{
                api(MessagesByThread.class),
                new MessagesByThreadObj()
                        .setUid(UID)
                        .setPage("1")
                        .setCount("50")
                        .setTid(TID)
                        .setSortType("date1"),
                MessagesByThread.class.getSimpleName() + " with page"
        });

        return operList;
    }

    public static List<Object[]> apiMessageUnread() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //messages_unread   DARIA-30609
        operList.add(new Object[]{
                api(MessagesUnread.class),
                new MessagesUnreadObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesUnread.class.getSimpleName() + " uid"
        });

        //messages_unread with wrong uid
        operList.add(new Object[]{
                api(MessagesUnread.class),
                new MessagesUnreadObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesUnread.class.getSimpleName() + " with wrong uid"
        });

        //messages_unread with wrong sort_type
        operList.add(new Object[]{
                api(MessagesUnread.class),
                new MessagesUnreadObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setSortType(NOT_EXIST_PARAM),
                MessagesUnread.class.getSimpleName() + " with wrong sort_type"
        });

        //messages_unread with big first
        operList.add(new Object[]{
                api(MessagesUnread.class),
                new MessagesUnreadObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesUnread.class.getSimpleName() + " with big first"
        });


        //messages_unread with null count
        operList.add(new Object[]{
                api(MessagesUnread.class),
                new MessagesUnreadObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("0")
                        .setSortType("date1"),
                MessagesUnread.class.getSimpleName() + " with null count"
        });

        //messages_unread with page
        operList.add(new Object[]{
                api(MessagesUnread.class),
                new MessagesUnreadObj()
                        .setUid(UID)
                        .setPage("1")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesUnread.class.getSimpleName() + " with page"
        });

        return operList;
    }

    //DARIA-44825
    public static List<Object[]> apiMessagesInFolderWithPins() {
        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " without inbox"
        });

        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " uid"
        });

        //messages_by_folder with wrong uid
        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " with wrong uid"
        });

        //messages_by_folder with wrong fid
        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(NOT_EXIST_PARAM)
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " with wrong fid"
        });

        //messages_by_folder with wrong sort_type
        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType(NOT_EXIST_PARAM),
                MessagesInFolderWithPins.class.getSimpleName() + " with wrong sort_type"
        });

        //messages_by_folder with big first
        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " with big first"
        });

        //messages_by_folder with null count
        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("0")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " with null count"
        });

        //messages_by_folder with page
        operList.add(new Object[]{
                api(MessagesInFolderWithPins.class),
                new MessagesByFolderObj()
                        .setUid(UID)
                        .setPage("1")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesInFolderWithPins.class.getSimpleName() + " with page"
        });

        return operList;
    }

    public static List<Object[]> apiThreadsInFolderWithPins() {
        List<Object[]> operList = new ArrayList<Object[]>();

        //threads_by_folder
        operList.add(new Object[]{
                api(ThreadsInFolderWithPins.class),
                new ThreadsByFolderObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setSince("1391600218")
                        .setTill("9999999999"),
                ThreadsInFolderWithPins.class.getSimpleName() + " without inbox"
        });

        //threads_by_folder
        operList.add(new Object[]{
                api(ThreadsInFolderWithPins.class),
                new ThreadsByFolderObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(INBOX_FID)
                        .setSince("1391600218")
                        .setTill("9999999999"),
                ThreadsInFolderWithPins.class.getSimpleName()
        });

        operList.add(new Object[]{
                api(ThreadsInFolderWithPins.class),
                new ThreadsByFolderObj()
                        .setUid(NOT_EXIST_UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid("2300000310083650377"),
                ThreadsInFolderWithPins.class.getSimpleName() + " with NOT_EXIST_UID"
        });

        operList.add(new Object[]{
                api(ThreadsInFolderWithPins.class),
                new ThreadsByFolderObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(NOT_EXIST_PARAM),
                ThreadsInFolderWithPins.class.getSimpleName() + " with wrong FID"
        });
        return operList;
    }

    public static List<Object[]> apiMessagesByFolderWithoutLabel() {
        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(MessagesByFolderWithoutLabel.class),
                new MessagesByFolderWithoutLabelObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setLid(LABEL_ID) ,
                MessagesByFolderWithoutLabel.class.getSimpleName() + " without inbox"
        });

        //MessagesByFolderWithoutLabel
        operList.add(new Object[]{
                api(MessagesByFolderWithoutLabel.class),
                new MessagesByFolderWithoutLabelObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(INBOX_FID)
                        .setLid(LABEL_ID) ,
                MessagesByFolderWithoutLabel.class.getSimpleName()
        });

        operList.add(new Object[]{
                api(MessagesByFolderWithoutLabel.class),
                new MessagesByFolderWithoutLabelObj()
                        .setUid(NOT_EXIST_UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(INBOX_FID)
                        .setLid(LABEL_ID),
                MessagesByFolderWithoutLabel.class.getSimpleName() + " with NOT_EXIST_UID"
        });

        operList.add(new Object[]{
                api(MessagesByFolderWithoutLabel.class),
                new MessagesByFolderWithoutLabelObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(NOT_EXIST_PARAM)
                        .setLid(LABEL_ID),
                MessagesByFolderWithoutLabel.class.getSimpleName() + " with not exist FID"
        });

        operList.add(new Object[]{
                api(MessagesByFolderWithoutLabel.class),
                new MessagesByFolderWithoutLabelObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(INBOX_FID)
                        .setLid(NOT_EXIST_PARAM),
                MessagesByFolderWithoutLabel.class.getSimpleName() + " with not exist LID"
        });
        return operList;
    }

    public static List<Object[]> apiMessageUnreadByFolder() {
        List<Object[]> operList = new ArrayList<Object[]>();

        //messages_unread_by_folder
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setSortType("date1"),
                MessagesUnreadByFolder.class.getSimpleName() + " without inbox"
        });

        //messages_unread_by_folder
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesUnreadByFolder.class.getSimpleName() + " uid"
        });

        //messages_unread_by_folder with wrong uid
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesUnreadByFolder.class.getSimpleName() + " with wrong uid"
        });

        //messages_unread_by_folder with wrong fid
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(NOT_EXIST_PARAM)
                        .setSortType("date1"),
                MessagesUnreadByFolder.class.getSimpleName() + " with wrong fid"
        });

        //messages_unread_by_folder with wrong sort_type
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType(NOT_EXIST_PARAM),
                MessagesUnreadByFolder.class.getSimpleName() + " with wrong sort_type"
        });

        //messages_unread_by_folder with big first
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesUnreadByFolder.class.getSimpleName() + " with big first"
        });

        //messages_unread_by_folder with null count
        operList.add(new Object[]{
                api(MessagesUnreadByFolder.class),
                new MessagesUnreadByFolderObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("0")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesUnreadByFolder.class.getSimpleName() + " with null count"
        });

        return operList;
    }

    public static List<Object[]> apiMessageWithAttaches() {
        List<Object[]> operList = new ArrayList<Object[]>();

        //messages_with_attaches
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesWithAttaches.class.getSimpleName() + " uid"
        });

        //messages_with_attaches with wrong uid
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(NOT_EXIST_UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesWithAttaches.class.getSimpleName() + " with wrong uid"
        });

        //messages_with_attaches with wrong fid
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(NOT_EXIST_PARAM)
                        .setSortType("date1"),
                MessagesWithAttaches.class.getSimpleName() + " with wrong fid"
        });


        //messages_with_attaches with wrong sort_type
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(UID)
                        .setFirst("0")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType(NOT_EXIST_PARAM),
                MessagesWithAttaches.class.getSimpleName() + " with wrong sort_type"
        });


        //messages_with_attaches with big first
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesWithAttaches.class.getSimpleName() + " with big first"
        });

        //messages_with_attaches with null count
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(UID)
                        .setFirst("50")
                        .setCount("0")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesWithAttaches.class.getSimpleName() + " with null count"
        });

        //messages_with_attaches with page
        operList.add(new Object[]{
                api(MessagesWithAttaches.class),
                new MessagesWithAttachesObj()
                        .setUid(UID)
                        .setPage("1")
                        .setCount("50")
                        .setFid(INBOX_FID)
                        .setSortType("date1"),
                MessagesWithAttaches.class.getSimpleName()
        });

        return operList;
    }

    public static List<Object[]> apiThreadsByFolder() {
        List<Object[]> operList = new ArrayList<Object[]>();

        //threads_by_folder
        operList.add(new Object[]{
                api(ThreadsByFolderCommand.class),
                new ThreadsByFolderObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setSince("1391600218")
                        .setTill("9999999999"),
                ThreadsByFolderCommand.class.getSimpleName() + " without inbox"
        });

        //threads_by_folder
        operList.add(new Object[]{
                api(ThreadsByFolderCommand.class),
                new ThreadsByFolderObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(INBOX_FID)
                        .setSince("1391600218")
                        .setTill("9999999999"),
                ThreadsByFolderCommand.class.getSimpleName()
        });

        operList.add(new Object[]{
                api(ThreadsByFolderCommand.class),
                new ThreadsByFolderObj()
                        .setUid(NOT_EXIST_UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid("2300000310083650377"),
                ThreadsByFolderCommand.class.getSimpleName() + " with NOT_EXIST_UID"
        });

        operList.add(new Object[]{
                api(ThreadsByFolderCommand.class),
                new ThreadsByFolderObj()
                        .setUid(UID)
                        .setCount("10")
                        .setPage("1")
                        .setFid(NOT_EXIST_PARAM),
                ThreadsByFolderCommand.class.getSimpleName() + " with NOT_EXIST_FID"
        });
        return operList;
    }

    public static List<Object[]> apiInReplyTo() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //in_reply_to
        operList.add(new Object[]{
                api(InReplyTo.class),
                empty().setUid(UID)
                        .setMessageId("%3C149431400240256%40webcorp2h.yandex-team.ru%3E"),
                InReplyTo.class.getSimpleName()
        });

        operList.add(new Object[]{
                api(InReplyTo.class),
                empty()
                        .setUid(NOT_EXIST_UID)
                        .setMessageId("%3C149431400240256%40webcorp2h.yandex-team.ru%3E"),
                InReplyTo.class.getSimpleName() + "with NOT_EXIST_UID"
        });

        operList.add(new Object[]{
                api(InReplyTo.class),
                empty()
                        .setUid(UID)
                        .setMessageId(NOT_EXIST_PARAM),
                InReplyTo.class.getSimpleName() + "with wrong message_id"
        });


        operList.add(new Object[]{
                api(InReplyTo.class),
                empty()
                        .setUid(UID)
                        .setMessageId(encode("<337461401967702@web26m.yandex.ru>")),
                InReplyTo.class.getSimpleName() + "with DRAFT"
        });

        return operList;
    }

    public static List<Object[]> apiYamailStatus() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //auth
        operList.add(new Object[]{
                api(YamailStatus.class),
                CountersObject.empty().setUid(UID),
                YamailStatus.class.getSimpleName() + "with uid"
        });

        //auth with emails = yes
        operList.add(new Object[]{
                api(YamailStatus.class),
                CountersObject.empty().setUid(UID),
                YamailStatus.class.getSimpleName() + " with uid"
        });

        return operList;
    }

    //DARIA-50550
    public static List<Object[]> apiThreadsInfo() {

        List<Object[]> operList = new ArrayList<Object[]>();

        operList.add(new Object[]{
                api(ThreadsInfo.class),
                ThreadsInfoObj.empty().setUid(UID).setTid(TID),
                ThreadsInfo.class.getSimpleName() + " with tid"
        });

        operList.add(new Object[]{
                api(ThreadsInfo.class),
                ThreadsInfoObj.empty().setUid(UID).setTid(TID, TID_2),
                ThreadsInfo.class.getSimpleName() + " with tids"
        });

        operList.add(new Object[]{
                api(ThreadsInfo.class),
                ThreadsInfoObj.empty().setUid(NOT_EXIST_UID).setTid(TID),
                ThreadsInfoObj.class.getSimpleName() + " with NOT_EXIST_UID"
        });

        operList.add(new Object[]{
                api(ThreadsInfo.class),
                ThreadsInfoObj.empty().setUid(UID).setTid(TID),
                ThreadsInfoObj.class.getSimpleName() + " with uid, tid"
        });

        return operList;
    }

    //DARIA-50550
    public static List<Object[]> apiGetFirstEnvelopeDate() {
        List<Object[]> operList = new ArrayList<Object[]>();
        operList.add(new Object[]{
                FirstEnvelopeDate.firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj()
                        .setUid(UID).setFid(INBOX_FID)),
                ThreadsInfoObj.class.getSimpleName() + " with uid"
        });

        return operList;
    }
}
