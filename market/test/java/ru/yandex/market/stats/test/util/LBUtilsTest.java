package ru.yandex.market.stats.test.util;

import org.junit.Test;

import ru.yandex.market.stat.utils.LBUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 04.06.2020
 */
public class LBUtilsTest {
    @Test
    public void testOldFormatUnchanged() {
        String topic = "test-ident--testlog";
        assertThat(LBUtils.toOldFormatTopic(topic), is("test-ident--testlog"));
    }
    @Test
    public void testOldFormatFromNew() {
        String topic = "test-ident/testlog";
        assertThat(LBUtils.toOldFormatTopic(topic), is("test-ident--testlog"));
    }
    @Test
    public void testOldFormatFromIdent() {
        String ident = "test-ident";
        String logtype = "testlog";
        assertThat(LBUtils.toOldFormatTopic(ident, logtype), is("test-ident--testlog"));
    }
    @Test
    public void testNewFormatUnchanged() {
        String topic = "test-ident/testlog";
        assertThat(LBUtils.toNewFormatTopic(topic), is("test-ident/testlog"));
    }
    @Test
    public void testNewFormatFromOld() {
        String topic = "test-ident--testlog";
        assertThat(LBUtils.toNewFormatTopic(topic), is("test-ident/testlog"));
    }
    @Test
    public void testOldFormatForDC() {
        String topic = "rt3.man--marketstat--market-clicks-log:0";
        assertThat(LBUtils.toOldFormatTopic(topic), is(topic));
    }
    @Test
    public void testNewFormatFullUnchanged() {
        String topic = "test-account/testing/testlog";
        assertThat(LBUtils.toNewFormatTopic(topic), is(topic));
    }
    @Test
    public void testNewFormatFullToOld() {
        String topic = "test-account/testing/testlog";
        assertThat(LBUtils.toOldFormatTopic(topic), is("test-account@testing--testlog"));
    }
    @Test
    public void testOldTopicFullUnchanged() {
        String topic = "test-account@testing--testlog";
        assertThat(LBUtils.toOldFormatTopic(topic), is(topic));
    }
    @Test
    public void testOldTopicFullToNewFormat() {
        String topic = "test-account@testing--testlog";
        assertThat(LBUtils.toNewFormatTopic(topic), is("test-account/testing/testlog"));
    }
    @Test
    public void testNewFormatLongestUnchanged() {
        String topic = "test-account/testing/vasiliy/petrovich/testlog";
        assertThat(LBUtils.toNewFormatTopic(topic), is(topic));
    }
    @Test
    public void testNewFormatLongestToOld() {
        String topic = "test-account/testing/vasiliy/petrovich/testlog";
        assertThat(LBUtils.toOldFormatTopic(topic), is("test-account@testing@vasiliy@petrovich--testlog"));
    }
    @Test
    public void testOldTopicLongestUnchanged() {
        String topic = "test-account@testing@vasiliy@petrovich--testlog";
        assertThat(LBUtils.toOldFormatTopic(topic), is(topic));
    }
    @Test
    public void testOldTopicLongestToNewFormat() {
        String topic = "test-account@testing@vasiliy@petrovich--testlog";
        assertThat(LBUtils.toNewFormatTopic(topic), is("test-account/testing/vasiliy/petrovich/testlog"));
    }
    @Test
    public void testIdentLogtypeFromNewTopic() {
        String topic = "test-account/testing/vasiliy/petrovich/testlog";
        assertThat(LBUtils.getIdentOldFormat(topic), is("test-account@testing@vasiliy@petrovich"));
        assertThat(LBUtils.getLogtype(topic), is("testlog"));
    }
    @Test
    public void testIdentLogtypeFromOld() {
        String topic = "test-account@testing@vasiliy@petrovich--testlog";
        assertThat(LBUtils.getIdentOldFormat(topic), is("test-account@testing@vasiliy@petrovich"));
        assertThat(LBUtils.getLogtype(topic), is("testlog"));
    }
}
