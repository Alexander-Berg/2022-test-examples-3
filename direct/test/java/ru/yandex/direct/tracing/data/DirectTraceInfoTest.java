package ru.yandex.direct.tracing.data;

import java.util.Optional;
import java.util.OptionalLong;

import org.junit.Assert;
import org.junit.Test;

public class DirectTraceInfoTest {
    @Test
    public void testFull() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(3102772033795860070L),
                        Optional.of("direct.script"),
                        Optional.of("bsClientData")),
                DirectTraceInfo.extractIgnoringErrors("UPDATE /* reqid:3102772033795860070:direct.script:bsClientData" +
                        " */   phrases")
        );
    }

    @Test
    public void testFullSpaces() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(3102772033795860070L),
                        Optional.of("direct.script"),
                        Optional.of("bsClientData")),
                DirectTraceInfo.extractIgnoringErrors(
                        "  \t\r\n UPDATE\n\r /*\t reqid:3102772033795860070:direct.script:bsClientData\n \t*/   " +
                                "phrases")
        );
    }

    @Test
    public void testFullBegin() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(3102772033795860070L),
                        Optional.of("direct.script"),
                        Optional.of("bsClientData")),
                DirectTraceInfo.extractIgnoringErrors("/* reqid:3102772033795860070:direct.script:bsClientData */ " +
                        "UPDATE phrases")
        );
    }

    @Test
    public void testFullBeginSpaces() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(3102772033795860070L),
                        Optional.of("direct.script"),
                        Optional.of("bsClientData")),
                DirectTraceInfo.extractIgnoringErrors(
                        "\n\t /* \t \n reqid:3102772033795860070:direct.script:bsClientData \t \n */\n UPDATE phrases")
        );
    }

    @Test
    public void testReqId() {
        Assert.assertEquals(
                new DirectTraceInfo(3102772033795860070L),
                DirectTraceInfo.extractIgnoringErrors("UPDATE /* reqid:3102772033795860070 */ phrases")
        );
    }

    @Test
    public void testEmpty() {
        Assert.assertEquals(
                DirectTraceInfo.empty(),
                DirectTraceInfo.extractIgnoringErrors("SELECT 1")
        );
    }

    @Test
    public void testOnlyFromStartOfQuery1() {
        Assert.assertEquals(
                DirectTraceInfo.empty(),
                DirectTraceInfo.extractIgnoringErrors("SELECT '/* reqid:123 */'"));
    }

    @Test
    public void testOnlyFromStartOfQuery2() {
        Assert.assertEquals(
                new DirectTraceInfo(777),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:777 */ '/* reqid:123 */'"));
    }

    @Test
    public void testOnlyFromStartOfQuery3() {
        Assert.assertEquals(
                DirectTraceInfo.empty(),
                DirectTraceInfo.extractIgnoringErrors("SELECT 1 FROM table WHERE name = '/* reqid:123 */'"));
    }

    @Test
    public void testOperator() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100500)),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:operator=100500*/"));
    }

    @Test
    public void testEssTag() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100500),
                        Optional.of("user1"),
                        false),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:operator=100500:ess" +
                        "=user1*/"));
    }

    @Test
    public void testEssTagWithoutOperator() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.empty(),
                        Optional.of("user1"),
                        false),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:ess=user1*/"));
    }

    @Test
    public void testEssTagBeforeOperator() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100500L),
                        Optional.of("user1"),
                        false),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:ess=user1:operator" +
                        "=100500*/"));
    }

    @Test
    public void testReshardingLast() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100500),
                        Optional.of("user1"),
                        true),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:operator=100500:ess" +
                        "=user1:resharding=1*/"));
    }

    @Test
    public void testReshardingFirst() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100500),
                        Optional.of("user1"),
                        true),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:resharding=1" +
                        ":operator=100500:ess=user1*/"));
    }


    @Test
    public void testReshardingFalse() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100500),
                        Optional.of("user1"),
                        false),
                DirectTraceInfo.extractIgnoringErrors("SELECT /* reqid:123:someService:someMethod:resharding=0" +
                        ":operator=100500:ess=user1*/"));
    }

    @Test
    public void testUnknownKeyValue() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.of(100501)),
                DirectTraceInfo.extractIgnoringErrors(
                        "SELECT /* reqid:123:someService:someMethod:unk1=val1:operator=100501:unk2=val2 " +
                                "*/"));
    }

    @Test
    public void testSurviveOnIncorrectOperator() {
        Assert.assertEquals(
                new DirectTraceInfo(
                        OptionalLong.of(123),
                        Optional.of("someService"),
                        Optional.of("someMethod"),
                        OptionalLong.empty()),
                DirectTraceInfo.extractIgnoringErrors(
                        "SELECT /* reqid:123:someService:someMethod:operator=MWAHAHA */"));
    }

}
