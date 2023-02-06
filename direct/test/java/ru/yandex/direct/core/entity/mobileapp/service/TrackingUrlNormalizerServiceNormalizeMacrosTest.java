package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class TrackingUrlNormalizerServiceNormalizeMacrosTest {
    private final TrackingUrlNormalizerService service = TrackingUrlNormalizerService.instance();

    private final String source;
    private final String expectedResult;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList((Object[][]) new Object[][]{
                {
                        "https://example.com/?param={ANDROIDID}&foo=bar",
                        "https://example.com/?param=&foo=bar"
                },
                {
                        "https://example.com/?param={GOOGLEAID}&foo=bar",
                        "https://example.com/?param={google_aid}&foo=bar"
                },
                {
                        "https://example.com/?param={IOSIFA}&foo=bar",
                        "https://example.com/?param={ios_ifa}&foo=bar"
                },
                {
                        "https://example.com/?param={LOGID}&foo=bar",
                        "https://example.com/?param={logid}&foo=bar"
                },

                {
                        "https://example.com/?param={GOOGLE_AID_LC_BAZQUUX}&foo=bar",
                        "https://example.com/?param=&foo=bar"
                },
                {
                        "https://example.com/?param={ANDROID_ID_LC_BAZQUUX}&foo=bar",
                        "https://example.com/?param=&foo=bar"
                },
                {
                        "https://example.com/?param={IDFA_LC_BAZQUUX}&foo=bar",
                        "https://example.com/?param=&foo=bar"
                },
        });
    }

    public TrackingUrlNormalizerServiceNormalizeMacrosTest(String source, String expectedResult) {
        this.source = source;
        this.expectedResult = expectedResult;
    }

    @Test
    public void normalizeMacros() {
        assertThat(service.normalizeMacros(source)).isEqualTo(expectedResult);
    }
}
