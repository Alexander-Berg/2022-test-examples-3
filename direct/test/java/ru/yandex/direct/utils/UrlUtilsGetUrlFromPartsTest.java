package ru.yandex.direct.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.getUrlFromParts;

@RunWith(Parameterized.class)

public class UrlUtilsGetUrlFromPartsTest {

    @Parameterized.Parameters
    public static String[][] parameters() {
        return new String[][]{
                {
                        "https://local-direct.yandex.ru/"
                },
                {
                        "https://local-direct.yandex.ru:8443/internal_tools/"
                },
                {
                        "https://play.google.com/store/apps/details?id=com.rovio.abcasual"
                },
                {
                        "https://login:password@local-direct.yandex.ru:8443/internal_tools/"
                },
                {
                        "https://login:password@local-direct.yandex.ru:8443/internal_tools/query?a=b&c=d#31337"
                }
        };
    }

    @Parameterized.Parameter(0)
    public String input;

    @Test
    public void getUrlFromPartsTest() throws MalformedURLException, URISyntaxException {
        URL url = new URL(input);
        String expected = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                url.getQuery(), url.getRef()).toString();
        String actual = getUrlFromParts(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                url.getPath(), url.getQuery(), url.getRef());
        assertThat(actual).isEqualTo(expected);
    }

}
