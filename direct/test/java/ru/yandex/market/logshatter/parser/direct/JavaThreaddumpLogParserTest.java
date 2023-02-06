package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class JavaThreaddumpLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new JavaThreaddumpLogParser());
    private SimpleDateFormat dateFormat = new SimpleDateFormat(JavaThreaddumpLogParser.DATETIME_FORMAT);

    @Test
    public void testParse() throws Exception {
        String line = "2019-11-11:18:35:16 ppcdev6.yandex.ru 971128 java-jobs " +
            "H4sIAN9uzF0AA8VWbW/bNhD+PP2Kg5sPCZZwpGS9GXWxdGm6ZE2axWmLYRgCWjw7TGTSkOQkXdH" +
            "/vpPkOLLluP42w4BP57uH5PPcHfWbNQaTAhUUFjKc2ALh9POZc4n51JocIbEKoQ/ccbmID4SgL4io5" +
            "/k9ETjHszSF4iZDqUDNJlP4OEVzevQHBN2Dt7qAAWb3mMHnM9gVgnHm/hzDRD" +
            "/SahOC3es5zlWVnEOSyjyHwdklaDOyPef6Vt7L6xr6OtV50eePnD7hSHlRl3ORBHwfUjTj4qYvPLIxxQmaIu9" +
            "/c55Dk4SLkOx9aPrcKIlavli042S3jHOWfUE7V0ZrfLLl87yEbOe743QmUpsOvBIwzbTt" +
            "+2Dz68rikExnfbfEY9yb5HQsOc1R9X0R0gZZ6OVQaNVvHxFM5UYlwphIhI/DWxKWPUhd7O4B/P2cobp" +
            "+QhTyfxwAKHlmqTRjVkvBBoUssAdfDk+uTs7fw641YCso0szowmZ7zk" +
            "+yaOQ1F6q8Q5njr7Xev5zLQt8jnGFxY1Wdmc3YV2kUPjKlsyoTh+yoMr" +
            "/g8HA6ZSU5u0ueErfnhXvE3CWOMEOTIPxOKClmRKMLSlLtmppNwVfoFC5nsb" +
            "+GTN4ms66NZzIjBeXJtBkD4SfWKHogq0mojEYj70eEXn46Pz98++HdCnsZjtjiSBWJxzZbOC6Q1jPjD9QAW3H7Au7OwppzxrKZaQM" +
            "+76Pi2xVeSfixNjLV/1ZEe0tER6tlG7Cu2JLmqt0aNOPmmpURjtz" +
            "/qWYPmhXw2tgn7GxRiYSgUzlM8c0mFf6c4QxZOWTvcQP5dVilgAiDdbouFNlZWHMi1sq6CHoC5aWsAz0mNxzpfCqL5KaSt7skb7wiL2desKW61eBsqDuirjemZGihavOzXeM4nVssiq8HDza7w+yArqKwHKHhCzPUD5iIlvfL44j5UWu/aujHzf26kd/ebxkZxqPkh1W41Oj5zDCjLUtu2LsLm6bbj8nVzAFdcAmV8slkmjJl68c2Uju2Ft3lLdilqNQmd4dGHb0IvA6zW3VHmUpX+utnXSNfBjwMojewK5srfip0uuNuTBJ8Nal1os0Hybfff1fUUDYbM0xSTWXCqhJjhHsmjRyjekrbeTIuMqtmCTXTfKGVuPlVFbcad1OHzv+rUiOvW7bnewE0EbTBV6Kzep3FLF7zauCK1cKW9Ls8Zkdxo7DrVf6yM5pslzgZIL2wSWKHJt3qkl7IQ+onzrd7I3Fdf/mNpNn/TofeBy+QwJVO4Ermd1Cfv3XOyPcisfbmDvz2op6HS0NHei/c3I5zen4C49QOaf7RPM174Ib78IDybv7IHcf5D7xxyU4aCwAA";
        Date date = dateFormat.parse("2019-11-11:18:35:16");
        Object[] expect = new Object[]{
            "ppcdev6.yandex.ru",
            971128L,
            "java-jobs",
            "Connected to remote JVM\n" +
                "Response code = 0\n" +
                "2019-11-11 18:35:16\n" +
                "Full thread dump OpenJDK 64-Bit Server VM (11.0.2+9 mixed mode):\n" +
                "\n" +
                "Threads class SMR info:\n" +
                "_java_thread_list=0x00007fd384001c60, length=130, elements={\n" +
                "0x00007fd3cc017000, 0x00007fd3cc28c800, 0x00007fd3cc291000, 0x00007fd3cc2a4800,\n" +
                "0x00007fd3cc2a6800, 0x00007fd3cc2a8800, 0x00007fd3cc2aa800, 0x00007fd3cc33c000\n" +
                "}\n",
            "JNI global refs: 27, weak refs: 0\n" +
                "\n" +
                "\n",
            new ArrayList<>(asList(1L, 2L, 3L, 4L, 17L, 0L, 0L, 0L)),
            new ArrayList<>(asList("main", "Reference Handler", "Finalizer", "Signal Dispatcher", "jetty" +
                "-worker-1-17", "G1 Refine#1", "G1 Young RemSet Sampling", "VM Periodic Task Thread")),
            new ArrayList<>(asList(0, 1, 1, 1, 0, 0, 0, 0)),
            new ArrayList<>(asList("in Object.wait()", "waiting on condition", "in Object.wait()", "runnable",
                "runnable", "runnable", "runnable", "waiting on condition")),
            new ArrayList<>(asList("WAITING (on object monitor)", "RUNNABLE", "WAITING (on object monitor)",
                "RUNNABLE", "RUNNABLE", "", "", "")),
            new ArrayList<>(asList("\"main\" #1 prio=5 os_prio=0 cpu=28800.03ms elapsed=517100.73s " +
                    "tid=0x00007fd3cc017000 nid=0xed179 in Object.wait()  [0x00007fd3d45c4000]",
                "\"Reference Handler\" #2 daemon prio=10 os_prio=0 cpu=120.95ms elapsed=517100.70s " +
                    "tid=0x00007fd3cc28c800 nid=0xed18d waiting on condition  [0x00007fd3a8ff3000]",
                "\"Finalizer\" #3 daemon prio=8 os_prio=0 cpu=26.41ms elapsed=517100.70s tid=0x00007fd3cc291000 " +
                    "nid=0xed18e in Object.wait()  [0x00007fd3a8ef2000]",
                "\"Signal Dispatcher\" #4 daemon prio=9 os_prio=0 cpu=0.36ms elapsed=517100.70s " +
                    "tid=0x00007fd3cc2a4800 nid=0xed18f runnable  [0x0000000000000000]",
                "\"jetty-worker-1-17\" #17 prio=5 os_prio=0 cpu=256.18ms elapsed=517098.58s tid=0x00007fd3cdb59800 " +
                    "nid=0xed285 runnable  [0x00007fd379fc2000]",
                "\"G1 Refine#1\" os_prio=0 cpu=19.93ms elapsed=517100.21s tid=0x00007fd3a0001000 nid=0xed1f9 runnable",
                "\"G1 Young RemSet Sampling\" os_prio=0 cpu=370756.00ms elapsed=517100.73s tid=0x00007fd3cc225000 " +
                    "nid=0xed17f runnable",
                "\"VM Periodic Task Thread\" os_prio=0 cpu=185381.95ms elapsed=517100.65s tid=0x00007fd3cc33e800 " +
                    "nid=0xed1a3 waiting on condition")),
            new ArrayList<>(asList(
                "\tat java.lang.Object.wait(java.base@11.0.2/Native Method)\n\tat ru.yandex.direct.web.DirectWebApp.main(DirectWebApp.java:37)",
                "\tat java.lang.ref.Reference.waitForReferencePendingList(java.base@11.0.2/Native Method)\n\tat java.lang.ref.Reference$ReferenceHandler.run(java.base@11.0.2/Reference.java:213)",
                "\tat java.lang.Object.wait(java.base@11.0.2/Native Method)\n\t- waiting on <no object reference available>\n" +
                    "\tat java.lang.ref.ReferenceQueue.remove(java.base@11.0.2/ReferenceQueue.java:176)\n\tat java.lang.ref.Finalizer$FinalizerThread.run(java.base@11.0.2/Finalizer.java:170)",
                "",
                "\tat sun.nio.ch.EPoll.wait(java.base@11.0.2/Native Method)\n" +
                    "\tat sun.nio.ch.EPollSelectorImpl.doSelect(java.base@11.0.2/EPollSelectorImpl.java:120)\n" +
                    "\tat sun.nio.ch.SelectorImpl.lockAndDoSelect(java.base@11.0.2/SelectorImpl.java:124)\n" +
                    "\t- locked <0x0000000085a60768> (a sun.nio.ch.Util$2)\n" +
                    "\t- locked <0x0000000085a60710> (a sun.nio.ch.EPollSelectorImpl)\n" +
                    "\tat sun.nio.ch.SelectorImpl.select(java.base@11.0.2/SelectorImpl.java:141)\n" +
                    "\tat org.eclipse.jetty.io.ManagedSelector$SelectorProducer.select(ManagedSelector.java:396)\n" +
                    "\tat java.lang.Thread.run(java.base@11.0.2/Thread.java:834)",
                "",
                "",
                ""
            ))
        };

        checker.check(line,
            singletonList(date),
            singletonList(expect)
        );
    }
}
