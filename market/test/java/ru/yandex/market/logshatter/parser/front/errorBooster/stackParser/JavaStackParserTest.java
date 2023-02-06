package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class JavaStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "java.io.InterruptedIOException\n" +
                "\tat dve$2.a(SourceFile:280)\n" +
                "\tat dve$2.a(SourceFile:21183)\n" +
                "\tat pxn.a(SourceFile:1086)\n" +
                "\tat pxq.a(SourceFile:240)\n" +
                "\tat ru.yandex.searchplugin.assistant.AssistantService$c.a(SourceFile:359)\n" +
                "\tat ru.yandex.searchplugin.assistant.AssistantService$b$1.a(SourceFile:474)\n" +
                "\tat dnx.run(SourceFile:34)\n" +
                "\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)\n" +
                "\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)\n" +
                "\tat java.lang.Thread.run(Thread.java:919)",
            new StackFrame("dve$2.a", "SourceFile", 280, 0),
            new StackFrame("dve$2.a", "SourceFile", 21183, 0),
            new StackFrame("pxn.a", "SourceFile", 1086, 0),
            new StackFrame("pxq.a", "SourceFile", 240, 0),
            new StackFrame("ru.yandex.searchplugin.assistant.AssistantService$c.a", "SourceFile", 359, 0),
            new StackFrame("ru.yandex.searchplugin.assistant.AssistantService$b$1.a", "SourceFile", 474, 0),
            new StackFrame("dnx.run", "SourceFile", 34, 0),
            new StackFrame("java.util.concurrent.ThreadPoolExecutor.runWorker", "ThreadPoolExecutor.java", 1167, 0),
            new StackFrame("java.util.concurrent.ThreadPoolExecutor$Worker.run", "ThreadPoolExecutor.java", 641, 0),
            new StackFrame("java.lang.Thread.run", "Thread.java", 919, 0)
        ),

        new TestData(
            "java.net.UnknownHostException: Unable to resolve host \"yandex.ru\": No address associated with " +
                "hostname\n" +
                "\tat java.net.Inet6AddressImpl.lookupHostByName(Inet6AddressImpl.java:95)\n" +
                "\tat ru.yandex.searchplugin.morda.data.MordaCardsLoadDispatcher$b.run(SourceFile:1138)\n" +
                "\tat android.os.HandlerThread.run(HandlerThread.java:61)",
            new StackFrame("java.net.Inet6AddressImpl.lookupHostByName", "Inet6AddressImpl.java", 95, 0),
            new StackFrame("ru.yandex.searchplugin.morda.data.MordaCardsLoadDispatcher$b.run", "SourceFile", 1138, 0),
            new StackFrame("android.os.HandlerThread.run", "HandlerThread.java", 61, 0)
        ),

        new TestData(
            "android.database.sqlite.SQLiteDiskIOException: disk I/O error (code 4874 SQLITE_IOERR_SHMSIZE): , while " +
                "compiling: PRAGMA journal_mode\n" +
                "\tat android.database.sqlite.SQLiteConnection.nativePrepareStatement(Native Method)\n" +
                "\tat java.lang.Thread.run(Thread.java:764)",
            new StackFrame("android.database.sqlite.SQLiteConnection.nativePrepareStatement", "Native Method", 0, 0),
            new StackFrame("java.lang.Thread.run", "Thread.java", 764, 0)
        ),

        new TestData(
            "java.lang.reflect.UndeclaredThrowableException\n" +
                "\tat $Proxy0.dispatchTouchEvent(Unknown Source)\n" +
                "\tat com.android.internal.policy.PhoneWindow$DecorView.dispatchTouchEvent(PhoneWindow.java:2364)\n" +
                "\tat android.view.View.dispatchPointerEvent(View.java:9526)\n" +
                "\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:628)\n" +
                "Caused by: java.lang.reflect.InvocationTargetException\n" +
                "\tat java.lang.reflect.Method.invoke(Native Method)\n" +
                "\tat org.chromium.base.ApplicationStatus$c.invoke(SourceFile:194)\n" +
                "\tat java.lang.reflect.Proxy.invoke(Proxy.java:393)\n" +
                "\t... 36 more\n" +
                "Caused by: java.lang.StackOverflowError: stack size 8MB\n" +
                "\tat android.view.MotionEvent.setTargetAccessibilityFocus(MotionEvent.java:1869)\n" +
                "\tat android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2123)\n",
            new StackFrame("$Proxy0.dispatchTouchEvent", "Unknown Source", 0, 0),
            new StackFrame("com.android.internal.policy.PhoneWindow$DecorView.dispatchTouchEvent",
                "PhoneWindow.java", 2364, 0),
            new StackFrame("android.view.View.dispatchPointerEvent", "View.java", 9526, 0),
            new StackFrame("com.android.internal.os.ZygoteInit.main", "ZygoteInit.java", 628, 0),
            new StackFrame("java.lang.reflect.Method.invoke", "Native Method", 0, 0),
            new StackFrame("org.chromium.base.ApplicationStatus$c.invoke", "SourceFile", 194, 0),
            new StackFrame("java.lang.reflect.Proxy.invoke", "Proxy.java", 393, 0),
            new StackFrame("android.view.MotionEvent.setTargetAccessibilityFocus", "MotionEvent.java", 1869, 0),
            new StackFrame("android.view.ViewGroup.dispatchTouchEvent", "ViewGroup.java", 2123, 0)
        ),

        new TestData(
            "ERROR 15:26:03,120 Exception in thread Thread[UpStage:293,5,main] java.lang.AssertionError: " +
                "SpecialSection\n" +
                "at org.random.Pillariterator.RRailDiceIterator.<init>(RRailDiceIterator.java:58)\n" +
                "at org.random.filter.DiceAskFilter.getRRailPillarIterator(DiceAskFilter.java:66)\n" +
                "at java.lang.Thread.run(Thread.java:680)",
            new StackFrame("org.random.Pillariterator.RRailDiceIterator.<init>", "RRailDiceIterator.java", 58, 0),
            new StackFrame("org.random.filter.DiceAskFilter.getRRailPillarIterator", "DiceAskFilter.java", 66, 0),
            new StackFrame("java.lang.Thread.run", "Thread.java", 680, 0)
        ),

        new TestData(
            "java.lang.IllegalStateException: ViewHolder views must not be attached when created. " +
                "Ensure that you are not passing 'true' to the attachToRoot parameter of LayoutInflater.inflate(" +
                "..., boolean attachToRoot)\n\tat android.support.v7.widget.RecyclerView$Adapter.createViewHolder(" +
                "RecyclerView.java:6796)\n\tat android.support.v7.widget.RecyclerView$Recycler.tryGetViewHolderForP" +
                "ositionByDeadline(RecyclerView.java:5975)\n",
            new StackFrame("android.support.v7.widget.RecyclerView$Adapter.createViewHolder", "RecyclerView.java",
                6796, 0),
            new StackFrame("android.support.v7.widget.RecyclerView$Recycler.tryGetViewHolderForPositionByDeadline",
                "RecyclerView.java", 5975, 0)
        ),

        new TestData(
            "java.lang.NullPointerException\n\tat com.yandex.auth.sync.AccountProvide" +
                "rHelper.yandexAccountsFromCursor(AccountProviderHelper." +
                "java:74)\n\tat com.yandex.auth.sync.AccountProviderHelper.getAccounts(AccountProviderHel" +
                "per.java:39)\n\tat com.yandex.auth.sync.BackupLogic.backup(BackupLogic.java:44)\n",
            new StackFrame("com.yandex.auth.sync.AccountProviderHelper.yandexAccountsFromCursor",
                "AccountProviderHelper.java", 74, 0),
            new StackFrame("com.yandex.auth.sync.AccountProviderHelper.getAccounts",
                "AccountProviderHelper.java", 39, 0),
            new StackFrame("com.yandex.auth.sync.BackupLogic.backup", "BackupLogic.java", 44, 0)
        ),

        new TestData(
            "java.lang.NullPointerException: OutOfMemoryError thrown while trying to throw OutOfMemoryError; no stack" +
                " trace available\\n"
        ),

        // Empty
        new TestData(
            ""
        ),
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    static class TestData {
        private final String stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = stack;
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new JavaStackParser(stack).getStackFrames();
            assertArrayEquals(
                frames,
                actualFrames,
                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message"
            );
        }
    }
}
