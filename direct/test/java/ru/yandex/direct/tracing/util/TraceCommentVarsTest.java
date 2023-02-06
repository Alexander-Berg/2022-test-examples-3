package ru.yandex.direct.tracing.util;

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TraceCommentVarsTest {

    @Test
    public void parseHeader_success() {
        Map<String, String> actual = TraceCommentVars.parseHeader("uid=123,cluid=345");
        assertThat(actual).containsExactly(entry("uid", "123"), entry("cluid", "345"));
    }

    @Test
    public void parseHeader_empty_whenNullHeader() {
        Map<String, String> actual = TraceCommentVars.parseHeader(null);
        assertThat(actual).isEmpty();
    }

    @Test
    public void parseHeader_empty_whenEmptyHeader() {
        Map<String, String> actual = TraceCommentVars.parseHeader("");
        assertThat(actual).isEmpty();
    }

    @Test
    public void parseHeader_empty_whenBlankHeader() {
        Map<String, String> actual = TraceCommentVars.parseHeader(" ");
        assertThat(actual).isEmpty();
    }

    @Test
    public void parsePageInfoHeader_success() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader("reqid:123,cmd:showDna");
        assertThat(actual).containsOnly(entry("pageReqid", "123"), entry("pageCmd", "showDna"));
    }

    @Test
    public void parsePageInfoHeader_onlyReqid() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader("reqid:123");
        assertThat(actual).containsOnly(entry("pageReqid", "123"));
    }

    @Test
    public void parsePageInfoHeader_onlyCmd() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader("cmd:showDna");
        assertThat(actual).containsOnly(entry("pageCmd", "showDna"));
    }

    @Test
    public void parsePageInfoHeader_skipsUnknownKey() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader("reqid:123,cmd:showDna,definitelyUnknownKey:value");
        assertThat(actual).containsOnly(entry("pageReqid", "123"), entry("pageCmd", "showDna"));
    }

    @Test
    public void parsePageInfoHeader_empty_whenNullHeader() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader(null);
        assertThat(actual).isEmpty();
    }

    @Test
    public void parsePageInfoHeader_empty_whenEmptyHeader() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader("");
        assertThat(actual).isEmpty();
    }

    @Test
    public void parsePageInfoHeader_empty_whenBlankHeader() {
        Map<String, String> actual = TraceCommentVars.parsePageInfoHeader(" ");
        assertThat(actual).isEmpty();
    }
}
