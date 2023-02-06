package ru.yandex.direct.core.service.urlchecker;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;

@RunWith(JUnitParamsRunner.class)
public class RedirectCheckerParseMetaRedirectTest {

    public static List<Object[]> parametersForParseMetaRedirectTest() {
        return asList(new Object[][]{
                {"<html>\n" +
                        "\t<head>\n" +
                        "\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
                        "\t\t<meta http-equiv=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\">\n" +
                        "\t\t<title>test title</title>\n" +
                        "\t</head>\n" +
                        "\t<body>\n" +
                        "\t\t<h1>Увы, ссылка, по которой вы перешли не работает.</h1>\n" +
                        "\t\t<p>Не волнуйтесь, вы будете переадресованы на другую страницу.</p>\n" +
                        "\t\tЕсли вы не были переадресованы, " +
                        "<a href=\"https://another.domain.com/test/\">нажмите здесь</a>\n" +
                        "\t</body>\n" +
                        "</html>",
                        "https://url-to-redirect.com/test"
                },
                {"<html>\n" +
                        "\t<head>\n" +
                        "\t\t<meta http-equiv=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\">\n" +
                        "\t</head>\n" +
                        "</html>",
                        "https://url-to-redirect.com/test"
                },
                {"<html>\n" +
                        "\t<head>\n" +
                        "\t\t<!-- <meta http-equiv=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\"> " +
                        "-->\n" +
                        "\t</head>\n" +
                        "</html>",
                        null
                },
                {"<meta http-equiv=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\">\n",
                        "https://url-to-redirect.com/test"
                },
                {"<META HTTP-EQUIV=\"REFRESH\" CONTENT=\"5; URL=https://url-to-redirect.com/test\">\n",
                        "https://url-to-redirect.com/test"
                },
                {"<meta http-equiv='refresh' content='5; url=https://url-to-redirect.com/test'>\n",
                        "https://url-to-redirect.com/test"
                },
                {"<meta http-equiv=refresh content=5;https://url-to-redirect.com/test>\n",
                        "https://url-to-redirect.com/test"
                },
                {"<meta http-equiv=' REFRESH ' content='5; url=https://url-to-redirect.com/test'>\n",
                        "https://url-to-redirect.com/test"
                },
                {"<meta name=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\">\n",
                        "https://url-to-redirect.com/test"
                },
                {"<meta http-equiv=\"refresh\">\n",
                        null
                },
                {"<meta http-equiv=\"refresh\" content=\"url=https://url-to-redirect.com/test\">\n",
                        null
                },
                {"<meta http-equiv=\"refresh\" content=\"100; url=https://url-to-redirect.com/test\">\n",
                        null
                },
                // обрезается после тега meta
                {"<html>\n" +
                        "\t<head>\n" +
                        "\t\t<title>very long title: " + "q".repeat(9_865) + " </title>\n" +
                        "\t\t<meta http-equiv=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\">\n" +
                        "\t</head>\n" +
                        "\t<body>\n" +
                        "\t\t<h1>test body</h1>\n" +
                        "\t</body>\n" +
                        "</html>",
                        "https://url-to-redirect.com/test"
                },
                // обрезается внутри тега meta
                {"<html>\n" +
                        "\t<head>\n" +
                        "\t\t<title>very long title: " + "q".repeat(9_900) + " </title>\n" +
                        "\t\t<meta http-equiv=\"refresh\" content=\"5; url=https://url-to-redirect.com/test\">\n" +
                        "\t</head>\n" +
                        "\t<body>\n" +
                        "\t\t<h1>test body</h1>\n" +
                        "\t</body>\n" +
                        "</html>",
                        null
                },
                {null, null},
                {"", null},
                // чтобы получить ошибку: Input is binary and unsupported
                {new String(generateBlankGifImageData(10, 20), StandardCharsets.UTF_8), null},
        });
    }

    @Test
    @Parameters
    public void parseMetaRedirectTest(
            String responseBody,
            String expected
    ) {
        Optional<String> actual = RedirectChecker.parseMetaRedirect(responseBody);
        assertThat(actual).isEqualTo(Optional.ofNullable(expected));
    }
}
