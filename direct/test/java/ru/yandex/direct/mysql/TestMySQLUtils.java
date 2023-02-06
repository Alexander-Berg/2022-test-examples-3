package ru.yandex.direct.mysql;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestMySQLUtils {
    @Test
    public void handlesInitialComments() {
        assertThat(MySQLUtils.extractFirstWordFromSQL("-- foo\r\n--bar\n\r select from foobar"), is("select"));
    }

    @Test
    public void handlesMultilineComments() {
        assertThat(MySQLUtils.extractFirstWordFromSQL("/* comment */insert/*reqid*/ into foobar"), is("insert"));
    }

    @Test
    public void handlesNestedComments() {
        assertThat(MySQLUtils.extractFirstWordFromSQL("/* comment /* in /* comment */*/*/ insert into foobar"), is("insert"));
    }

    @Test
    public void doesNotAcceptLeadingNumbers() {
        assertThat(MySQLUtils.extractFirstWordFromSQL("123hello world"), is(""));
    }

    @Test
    public void acceptsTrailingNumbers() {
        assertThat(MySQLUtils.extractFirstWordFromSQL("hello123 world"), is("hello123"));
    }

    @Test
    public void singleWordIsOk() {
        assertThat(MySQLUtils.extractFirstWordFromSQL("hello"), is("hello"));
    }

    @Test
    public void sqlWordsParserTest() {
        var sqlWordsParser = new MySQLUtils.SqlWordsParser("DROP TABLE `sadfadsf`");

        var soft = new SoftAssertions();
        soft.assertThat(sqlWordsParser.nextWord()).isEqualTo("DROP");
        soft.assertThat(sqlWordsParser.nextWord()).isEqualTo("TABLE");
        soft.assertAll();
    }
}
