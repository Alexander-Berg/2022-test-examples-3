package ru.yandex.direct.core.entity.banner.model;

import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(JUnitParamsRunner.class)
public class TextBannerFlagsConvertingTest {

    public static Object provideTestData() {
        return new Object[][]{
                {new BannerFlags().withFlags(singletonMap("alcohol", null)), "alcohol"},
                {new BannerFlags().withFlags(singletonMap("age", "1")), "age:1"},

                {new BannerFlags().withFlags(ImmutableMap.of("unknown_flag", "42")), "unknown_flag:42"},

                {new BannerFlags().withFlags(ImmutableMap.of("age", "12", "baby_food", "1")), "age:12,baby_food:1"},

                {new BannerFlags().withFlags(
                        new HashMap<String, String>() {{
                            put("baby_food", "1");
                            put("medicine", null);
                            put("age", "6");
                        }}),
                        "age:6,baby_food:1,medicine"
                }
        };
    }

    @Test
    @Parameters(source = TextBannerFlagsConvertingTest.class)
    public void toSource(BannerFlags bannerFlags, String flagsString) {
        assertThat(BannerFlags.toSource(bannerFlags), is(flagsString));
    }

    @Test
    @Parameters(source = TextBannerFlagsConvertingTest.class)
    public void fromSource(BannerFlags bannerFlags, String flagsString) {
        assertThat(BannerFlags.fromSource(flagsString), is(bannerFlags));
    }

    @Test
    public void fromSource_NullStringToNullBannerFlags() {
        assertThat(BannerFlags.fromSource(null), nullValue());
    }

    @Test
    public void fromSource_EmptyStringToNullBannerFlags() {
        assertThat(BannerFlags.fromSource(""), nullValue());
    }

    @Test
    public void toSource_NullBannerFlagsToNullString() {
        assertThat(BannerFlags.toSource(null), nullValue());
    }

    @Test
    public void toSource_EmptyBannerFlagsToNullString() {
        assertThat(BannerFlags.toSource(new BannerFlags()), nullValue());
    }
}

