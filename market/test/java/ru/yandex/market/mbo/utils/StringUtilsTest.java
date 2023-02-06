package ru.yandex.market.mbo.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.HashMap;

import ru.yandex.market.mbo.gwt.utils.StringUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 27.07.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class StringUtilsTest {
    @Test
    public void duration() throws Exception {
        assertThat(StringUtils.duration(0), is("0s"));
        assertThat(StringUtils.duration(7), is("7s"));
        assertThat(StringUtils.duration(60), is("1m"));
        assertThat(StringUtils.duration(60 + 3), is("1m 3s"));
        assertThat(StringUtils.duration(7 * 60 + 17), is("7m 17s"));
        assertThat(StringUtils.duration((14 * 60 + 13) * 60 + 42), is("14h 13m"));
        assertThat(StringUtils.duration((14 * 60) * 60 + 42), is("14h 42s"));
        assertThat(StringUtils.duration((99 * 24 * 60) * 60 + 42), is("99d 42s"));
        assertThat(StringUtils.duration((142 * 24 * 60) * 60), is("142d"));
    }

    @Test
    public void negativeDuration() throws Exception {
        assertThat(StringUtils.duration(-0), is("0s"));
        assertThat(StringUtils.duration(-7), is("-7s"));
        assertThat(StringUtils.duration(-60), is("-1m"));
        assertThat(StringUtils.duration(-(60 + 3)), is("-1m 3s"));
        assertThat(StringUtils.duration(-((99 * 24 * 60) * 60 + 42)), is("-99d 42s"));
    }

    @Test
    public void gwtEllipsis() throws Exception {
        assertThat("limits simple",
            StringUtil.ellipsis("long text", 5, "…"), is("long…"));
        assertThat("limits trims right spaces",
            StringUtil.ellipsis("long text", 6, "…"), is("long…"));
        assertThat("ellipsis length is counted",
            StringUtil.ellipsis("long text", 5, "..."), is("lo..."));
        assertThat("ellipsis not used if limit is less than length",
            StringUtil.ellipsis("long text", 4, "...."), is("long"));
        assertThat("whitespaces are left untouched if less",
            StringUtil.ellipsis("   ", 5, "..."), is("   "));
        assertThat("whitespaces trims if less",
            StringUtil.ellipsis("          trailing text", 5, "..."), is("..."));
    }

    @Test
    public void abbreviateCamelCase() {
        assertThat(StringUtils.abbreviateCamelCase("aaaaCccBbb", 6), is("aaaaCB"));
        assertThat(StringUtils.abbreviateCamelCase("aaaaCccBbb", 5), is("aaaaC"));
        assertThat(StringUtils.abbreviateCamelCase("aaaacccBbb", 5), is("aaaac"));
        assertThat(StringUtils.abbreviateCamelCase("AaaaCccBbb", 5), is("ACB"));
        assertThat(StringUtils.abbreviateCamelCase("AaaaCCcBBB"), is("ACCBBB"));
    }

    @Test
    public void formatVars() {
        Assertions.assertThat(StringUtils.formatVars(
            "This is a {var1}, and there lies a {var2}", new HashMap<String, Object>() {{
                put("var1", "cat");
                put("var2", "dog");
            }}
        )).isEqualTo("This is a cat, and there lies a dog");
    }
}
